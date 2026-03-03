package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.api.ItemCanBeInitialized;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.lua.components.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LuaVirtualMachine {
    private static final int TPS = 20;
    public LuaEventQueue eventQueue;

    public final ComputerBlockEntity cbe;

    private LuaVM vm;
    private final int ipt; // instructions per tick
    private long timeLastHook = 0;

    private final Object startStopLock = new Object();
    private volatile boolean suspended;
    private volatile boolean isRunning;
    private volatile Thread executorThread;

    private LuaStdOut stdOut;
    private String stopCode;
    private boolean stopCode_isGraceful = false;

    private Consumer<Boolean> runOnExit = null;
    private volatile boolean killingLVM = false;

    public ComputerUD computerUD = null;
    public GpuUD gpuUD = null;
    public ComponentRegistryUD componentReg = null;

    private final LinkedHashSet<TextBufferUD> dirtyBuffers = new LinkedHashSet<>();

    public LuaVirtualMachine(ComputerBlockEntity cbe, int instructionsPerSecond) {
        this.cbe = cbe;
        ipt = Math.max(instructionsPerSecond / 20, 1);
        stdOut = null;
    }

    public void sandboxLog(String s, boolean newLine, boolean error) {
        var msg = s.replace("\r", "");
        stdOut.print(msg);
        if (newLine) {
            stdOut.print("\n");
        }

        // print to system.out for debugging purposes
        if (msg.replace(" ", "").toLowerCase().startsWith("error:") || msg.trim().toLowerCase().startsWith("warning:"))
            msg = " \r" + msg; // needed so idea/gradle doesnt remove it from the stdoutput and put it into stderr. What a dumb 'feature'.
        var printer = error ? System.err : System.out;
        if (newLine)
            printer.println(msg);
        else
            printer.print(msg);
    }

    public void sandboxLog(String s, boolean error) {
        sandboxLog(s, true, error);
    }

    public int getState() {
        synchronized (startStopLock) {
            if (suspended) {
                return 2;
            } else if (isRunning) {
                return 1;
            }
            return 0;
        }
    }

    public void startIfOff() {
        synchronized (startStopLock) {
            if (!isRunning)
                start();
        }
    }

    private String getComponentIdentifier(ItemStack is, int slotId) {
        return "computerInvItem" + is.getItem() + ";" + slotId;
    }

    public void start() {
        synchronized (startStopLock) {
            if (isRunning) {
                throw new IllegalStateException("LVM is already running");
            }
            isRunning = true;
            stopCode = "";
            stopCode_isGraceful = false;

            AdvancedComputers.LOGGER.info("Trying to start LVM");
            stdOut = new LuaStdOut();
            stdOut.clear();
            CableCluster.onBlockPosChangedInternal(cbe.getLevel(), cbe.getBlockPos(), AdvancedComputers.CLUSTER_TYPE_DEVICE);

            eventQueue = new LuaEventQueue(); // TODO serialize this

            // REGISTER USERDATA COMPONENTS
            componentReg = new ComponentRegistryUD(this);

            boolean isFreshInit = true; // TODO for deserialize, set to 'false' instead
            String uefiScript = null; // entry code; i.e. uefi
            // set up inventory components
            var inv = cbe.itemHandler;
            for (int i = 0; i < inv.getSlots(); i++) {
                var is = onInventorySlotChanged(i, isFreshInit);
                if (is.getItem() instanceof MainboardItem mi) {
                    uefiScript = mi.readUefiScript(is);
                }
            }

            if (uefiScript == null) {
                tryKill("No uefi installed", false, false);
                return;
            }

            // set up peripheral components // TODO this needs to be reworked a little, e.g. a device thats directly attached to a computer does not yet show up here
            var deviceComponentBlockEntities = cbe.connectedNetworks.values().stream()
                    .filter(x -> x.clusterType.getClusterName().equals("device"))
                    .flatMap(x -> Arrays.stream(x.connectedEntities))
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));

            for (var be : deviceComponentBlockEntities.stream().distinct().toArray()) {
                if (be instanceof AcBlockEntityComponent bec) {
                    componentReg.addComponentInitAndNotify(bec.CreateUserdata(), AcComponentSlotInfo.ofBlockComponent((BlockEntity)be), isFreshInit);
                }
                if (be instanceof ScreenBlockEntity sbe) {
                    NetCodeUtils.sendToClient(PacketDistributor.ALL.noArg(), new ScreenBlockEntity.ScreenContentToClientEvent(
                            new ScreenBlockEntity[]{sbe}, "clearGuiText", ""));
                }
            }

            gpuUD = new GpuUD();
            componentReg.addComponentInitAndNotify(new InternetUD(), AcComponentSlotInfo.ofBlockComponent(cbe), isFreshInit);
            componentReg.addComponentInitAndNotify(new ComputerUD(), AcComponentSlotInfo.ofBlockComponent(cbe), isFreshInit);
            componentReg.addComponentInitAndNotify(gpuUD, AcComponentSlotInfo.ofBlockComponent(cbe), isFreshInit);

            // TODO traverse peripheral network and instantiate and init the block userdata objects similarly
            // TODO define how exactly the peripheral network should behave (and make it behave that way then)

            // --------------------------
            // DEFINE GLOBALS
            var acFunReg = new ExtendedMixedStateFunctionRegistry("advancedcomputers");

            var executionTimeTracker = new LuaTimeTracker(cbe.getTier().threadExecutionSleepFactor);
            acFunReg.register("sleep", AtomicLuaFunction.forZeroResults(acFunReg, (vm, time) -> {
                try {
                    long sleepBegunAt = System.nanoTime();
                    Thread.sleep((int) (time.asDouble() * 1000));
                    long sleptForNs = Math.max(0, System.nanoTime() - sleepBegunAt);
                    executionTimeTracker.refundNanos(sleptForNs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LvmKillException();
                }
            }));
            acFunReg.register("print",
                    AtomicLuaFunction.vaForZeroResults(acFunReg, (vm, args) -> printlnLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));
            acFunReg.register("printInline",
                    AtomicLuaFunction.vaForZeroResults(acFunReg, (vm, args) -> printInlineLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));

            // SET UP GLOBAL ENV
            var _G = LuaObject.table();
            acFunReg.addFunctionsToTable(_G);

            // ADD COMPONENT TO _G
            _G.set("components", LuaObject.of(componentReg));
            _G.set("_HOST", LuaObject.of("AdvancedComputers %s; Minecraft %s".formatted(AdvancedComputers.getModVersion(), AdvancedComputers.getMinecraftVersion())));

            // TODO set stopCode and stopCode_isGraceful

            //        setGlobalFunction("setStopCode", new LuaFunctionProxy(this, (Object[] args) -> {
            //            var msg = Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" "));
            //            System.out.println("setStopCode: " + msg);
            //            stopCode = msg;
            //            stopCode_isGraceful = false; // TODO expose this as an arg to the vm maybe?
            //        }));


            //        try {
            //            vm.withRootFunc(luaEntryScript);
            //            var rv = vm.run();
            //            AdvancedComputers.LOGGER.info(String.format("LVM exited with code %s", rv));
            //            lvmCleanExit = rv.state() == LuaVM.VmRunState.SUCCESS;
            //        } catch (Exception e) {
            //            AdvancedComputers.LOGGER.error(e.toString());
            //            lvmException = true;
            //        }


            final String uefiScriptForLambda = uefiScript;
            executorThread = new Thread(() -> {
                try {
                    vm = LuaVM.builder().withApiRegistry(acFunReg).modifyEnv(t -> {
                        var map = _G.asMap();
                        for (var k : map.keys()) {
                            t.set(k, map.getOrDefault(k, LuaObject.NIL));
                        }

                        var vmTable = t.get("vm");
                        vmTable.set("pause", LuaObject.NIL);
                    }).rootFunc(uefiScriptForLambda).build();
                    vm.eventCallback = executionTimeTracker::handleVmEvent;
                    LuaVirtualMachine.this.runLua();
                } catch (Exception e) {
                    AdvancedComputers.LOGGER.error("Caught lua executor exception: %s".formatted(e.toString()));
                }
            });

            executorThread.start();
        }
    }

    public void suspend() {
        synchronized (startStopLock) {
            if (isRunning) {
                suspended = true;
            }
        }
    }

    public void resume() {
        synchronized (startStopLock) {
            if (executorThread == null) {
                throw new IllegalStateException("no execution environment to resume");
            }
            var prev = suspended;
            suspended = false;
            if (prev) {
                // only grant unpark permit if thread was parked in the first place
                LockSupport.unpark(executorThread);
            }
        }
    }

    public void tryKill(String reason, boolean isGracefulShutdown, boolean suppressBlockStateUpdate) {
        try {
            synchronized (startStopLock) {
                if (isRunning) {
                    synchronized (startStopLock) {
                        killingLVM = true;
                        stopCode = "[KILLED] " + reason;
                        stopCode_isGraceful = isGracefulShutdown;

                        AdvancedComputers.LOGGER.info("Lvm was killed for reason: %s".formatted(stopCode));
                        if(executorThread != null)
                            executorThread.interrupt();
                        if (suspended) {
                            resume();
                        }
                        // cleanup after kill
                        isRunning = false;
                        executorThread = null;
                    }
                }
            }
        } finally {
            // TODO unify this in some other function probs
            if (!suppressBlockStateUpdate)
                cbe.SetRunState(isGracefulShutdown ? ComputerBlock.ComputerRunState.STOPPED : ComputerBlock.ComputerRunState.CRASHED);
        }
    }

    public void toggleOnOff(Runnable onStart, Consumer<Boolean> onExit) {
        synchronized (startStopLock) {
            if (getState() == 0) {
                runOnExit = onExit;
                onStart.run();
                start();
            } else {
                tryKill("ON/OFF button pushed", true, true);
            }
        }
    }

    boolean isBeingKilled() {
        return killingLVM;
    }

    private void runLua() {
        boolean lvmException = false;
        boolean lvmCleanExit = false;
        try {
            var res = vm.run();

            if (res.state() == LuaVM.VmRunState.EXECUTION_ERROR) {
                AdvancedComputers.LOGGER.error("vm exited with error: %s".formatted(res.toString().replace("\\n", "\n")));
            } else {
                AdvancedComputers.LOGGER.info("vm exited with result: %s".formatted(res.toString().replace("\\n", "\n")));
            }
        } catch (Exception ex) {
            AdvancedComputers.LOGGER.error("caught lvm exception: ", ex);
        }

        boolean shutdownWasGraceful;
        // cleanup after shutdown
        synchronized (startStopLock) {
            killingLVM = false;
            isRunning = false;
            executorThread = null;
            stdOut = null;

            if (lvmCleanExit)
                shutdownWasGraceful = true;
            else if (lvmException) {
                shutdownWasGraceful = false;
            } else {
                shutdownWasGraceful = stopCode_isGraceful;
            }
        }
        runOnExit.accept(shutdownWasGraceful);
    }

    // TODO remove
    private static void println(String s) {
        System.out.println(s);
    }

    private static void printlnLUA(String s) {
        println(s);
    }

    private static void printInlineLUA(String s) {
        System.out.print(s);
    }
    // ------------

    private static RuntimeException interruptAndKillCurrent() {
        Thread.currentThread().interrupt();
        throw new LvmKillException();
    }

    public static LuaVirtualMachine deserializeOrNull(ComputerBlockEntity computerBlockEntity) {
        return null;
    }

    public void serialize() {
        tryKill("Chunk unloaded", false, true);
    }

    public ItemStack onInventorySlotChanged(int slot, boolean isFreshInit) {
        // when we get a new slot item here, remove all existing components that occupy the slot and then add this new one and init it
        componentReg.removeAllComponentsInSlot(x -> x != null && x.getSlotIndex() == slot && x.getInventoryOwnerPos().equals(cbe.getBlockPos()));
        // TODO should probs move this into component reg ud somehow to make it convenient to use for block components, but we'll see

        var newItemStack = cbe.itemHandler.getStackInSlot(slot);
        var item = newItemStack.getItem();
        if (item instanceof ItemCanBeInitialized icbi) {
            icbi.Initialize(newItemStack);
        }

        if (item instanceof AcItemComponent ud) {
            var srcInfo = AcComponentSlotInfo.ofItemComponent(cbe.getBlockPos(), slot);
            componentReg.addComponentInitAndNotify(ud.CreateUserdata(srcInfo), srcInfo, isFreshInit);
        }

        return newItemStack;
    }

    static class LvmKillException extends RuntimeException {
    }

    public void dirtyBuffer(TextBufferUD buf) {
        dirtyBuffers.add(buf);
    }

    /**
     * Sends all pending {@link TextBufferUD} updates for the given LVM. Call this method before any
     * long-running Lua library function to avoid screen-lag due to the LVM not hitting a safepoint
     * in the upcoming time.
     *
     * @return if updates were sent.
     */
    public boolean sendTextBufferUpdates() {
        if (dirtyBuffers.isEmpty()) {
            return false;
        }
        for (TextBufferUD buf : dirtyBuffers) {
            // this is still the LUA thread, so thread-safety is not a concern here
            // here we send the stuff
            Set<ScreenBlockEntity> screens = buf.getAssociatedScreens();
            if (screens.isEmpty()) {
                // no screen to sent to
                continue;
            }
            if (buf.isFreed) {
                NetCodeUtils.sendToClient(
                        PacketDistributor.ALL.noArg(),
                        new ScreenBlockEntity.ScreenContentToClientEvent(screens.toArray(ScreenBlockEntity[]::new), "clearGuiText", ""));
            }
            String text = buf.getTextAsString();
            NetCodeUtils.sendToClient(
                    PacketDistributor.ALL.noArg(),
                    new ScreenBlockEntity.ScreenContentToClientEvent(screens.toArray(ScreenBlockEntity[]::new), "setGuiText", text));
        }
        dirtyBuffers.clear();
        return true;
    }

    private class LuaTimeTracker {
        private static final long SECOND = 1_000_000_000;
        private static final int BUF_SEND_PER_SEC = 30; // TODO make this a config option

        long lastSafepointTimestamp = 0;
        long lastCompilationStartedTimestamp = 0;
        long lastBufferSend = 0;
        private final double sleepFactor;

        LuaTimeTracker(double sleepFactor) {
            this.sleepFactor = sleepFactor;
        }

        public void refundNanos(long nanos) {
            lastCompilationStartedTimestamp += nanos;
        }

        public void handleVmEvent(LuaVM vmObj, LuaVM.HookType eventType) {
            switch (eventType) {
                case COMPILATION_STARTED -> {
                    lastCompilationStartedTimestamp = System.nanoTime();
                    sendTextBufferUpdates();
                }
                case COMPILATION_FINISHED -> {
                    // fake the last safepoint timestamp to effectively refund the compilation time
                    long timeSpentCompiling = System.nanoTime() - lastCompilationStartedTimestamp;
                    refundNanos(timeSpentCompiling);
                }
                case SAFEPOINT_REACHED -> {
                    // capture time spent in lua calcuation
                    long now = System.nanoTime();
                    // maybe send text buffers
                    if (now - lastBufferSend > SECOND / BUF_SEND_PER_SEC) {
                        if (sendTextBufferUpdates()) {
                            lastBufferSend = now;
                        }
                    }
                    // do the timeout calculation
                    long timeSpentNs = (now - lastSafepointTimestamp);
                    long sleepTimeNs = (long) Math.ceil(timeSpentNs * sleepFactor);
                    long sleepTimeMs = sleepTimeNs / 1_000_000;
                    if (sleepTimeMs > 10) {
                        try {
                            Thread.sleep(sleepTimeMs, (int) (sleepTimeNs % 1_000_000));
                        } catch (InterruptedException ignore) {
                            Thread.currentThread().interrupt();
                        }
                        lastSafepointTimestamp = System.nanoTime();
                    }
                }
                case VM_STARTED, VM_RESUMED -> {
                    lastSafepointTimestamp = System.nanoTime();
                }
            }
        }
    }
}
