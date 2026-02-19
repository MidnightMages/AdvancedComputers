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
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockUD;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.lua.components.*;
import dev.asdf00.mc.advcomp.utils.Tuple;
import net.minecraftforge.network.PacketDistributor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LuaVirtualMachine {
    private static final int TPS = 20;
    public LuaEventQueue eventQueue;

    private final ComputerBlockEntity computer;

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

    private final Object screenBEsThatNeedUpdatingLock = new Object();
    private final HashSet<ScreenBlockEntity> screenBEsThatNeedUpdating = new HashSet<>();

    public LuaVirtualMachine(ComputerBlockEntity computer, int instructionsPerSecond) {
        this.computer = computer;
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

            eventQueue = new LuaEventQueue();
            //        console.onKeyPressed = eventQueue::addKeyPressed;
            //        console.onKeyReleased = eventQueue::addKeyReleased;
            //        console.onKeyTyped = eventQueue::addKeyTyped;

            // REGISTER USERDATA COMPONENTS
            var componentReg = new ComponentRegistryUD(this);
            // set up disk filesystems // TODO set up fs
            //        for (int i = 1; i <= 3; i++) {
            //            var dp = luaRootDir.resolve("disk" + i);
            //            var fs = new SandboxedFs(dp, !cfg.allowPhysicalFilesystemWrites());
            //            try {
            //                if (!Files.isDirectory(dp))
            //                    Files.createDirectory(dp);
            //            } catch (IOException e) {
            //                throw new RuntimeException(e);
            //            }
            //            fs.init(dp);
            //
            //            var ud = new DiskUD(i);
            //            ud.init(fs);
            //            componentReg.addComponentAndNotify(ud);
            //        }

            String uefiScript = null; // entry code; i.e. uefi
            var componentsToInit = new ArrayList<LuaUserDataComponent>();
            // set up inventory components
            var inv = computer.itemHandler;
            for (int i = 0; i < inv.getSlots(); i++) {
                var is = inv.getStackInSlot(i);
                var item = is.getItem();
                if (item instanceof ItemCanBeInitialized icbi) {
                    icbi.Initialize(is);
                }

                if (item instanceof AcItemComponent ud) {
                    var comp = ud.CreateUserdata(is);
                    componentsToInit.add(comp);
                } else if (item instanceof MainboardItem mi) {
                    uefiScript = mi.readUefiScript(is);
                }
            }

            if (uefiScript == null) {
                tryKill("No uefi installed", false, true);
                return;
            }

            // set up peripheral components // TODO this needs to be reworked a little, e.g. a device thats directly attached to a computer does not yet show up here
            var deviceComponentBlockEntities = computer.connectedNetworks.values().stream()
                    .filter(x -> x.clusterType.getClusterName().equals("device"))
                    .flatMap(x -> Arrays.stream(x.connectedEntities))
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));

            for (var be : deviceComponentBlockEntities.stream().distinct().toArray()) {
                if (be instanceof AcBlockEntityComponent bec) {
                    componentsToInit.add(bec.CreateUserdata());
                }
                if (be instanceof ScreenBlockEntity sbe) {
                    NetCodeUtils.sendToClient(PacketDistributor.ALL.noArg(), new ScreenBlockEntity.ScreenContentToClientEvent(sbe, "clearGuiText", ""));
                }
            }

            // TODO drop that
            for (var comp : componentsToInit) {
                componentReg.addComponentAndNotify(comp);
                comp.onVmInit(this);
            }


            // TODO traverse peripheral network and instantiate and init the block userdata objects similarly
            // TODO define how exactly the peripheral network should behave (and make it behave that way then)

            componentReg.addComponentAndNotify(new InternetUD(this));
            //componentReg.registerComponent(new BiosUD());
            componentReg.addComponentAndNotify(new ComputerUD(this));
            gpuUD = new GpuUD(this);
            componentReg.addComponentAndNotify(gpuUD);
            // --------------------------
            // DEFINE GLOBALS
            var greg = new ExtendedMixedStateFunctionRegistry("advancedcomputers");

            var executionTimeTracker = new ExecutionTimeTracker(computer.getTier().threadExecutionSleepFactor);
            greg.register("sleep", AtomicLuaFunction.forZeroResults(greg, (vm, time) -> {
                try {
                    long sleepBegunAt = System.nanoTime();
                    Thread.sleep((int) (time.asDouble() * 1000));
                    long sleptForNs = Math.max(0, System.nanoTime() - sleepBegunAt);
                    executionTimeTracker.refundNanos(sleptForNs);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
            greg.register("print",
                    AtomicLuaFunction.vaForZeroResults(greg, (vm, args) -> printlnLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));
            greg.register("printInline",
                    AtomicLuaFunction.vaForZeroResults(greg, (vm, args) -> printInlineLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));

            var br = new BufferedReader(new InputStreamReader(System.in));
            //        greg.register("readline", AtomicLuaFunction.forOneResult(greg, (vm, msg) -> {
            //            try {
            //                if (!msg.isNil()) {
            //                    printlnLUA(msg.asString());
            //                }
            //
            //                return LuaObject.of(br.readLine());
            //            } catch (IOException e) {
            //                throw new RuntimeException(e);
            //            }
            //        }));
            // --------------------------

            // SET UP GLOBAL ENV
            var _G = LuaObject.table();
            greg.addFunctionsToTable(_G);

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
                    vm = LuaVM.builder().withApiRegistry(greg).modifyEnv(t -> {
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
            if(!suppressBlockStateUpdate)
                computer.SetRunState(isGracefulShutdown ? ComputerBlock.ComputerRunState.STOPPED : ComputerBlock.ComputerRunState.CRASHED);
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

    private final HashMap<Integer, Tuple<AcItemComponent, LuaUserDataComponent>> luaComputerInventoryUserdataObjectsBySlotId = new HashMap<>();

    public void rebuildUserdataFromInventory() {
        var slots = computer.itemHandler.getSlots();
        for (int i = 0; i < slots; i++) {
            var is = computer.itemHandler.getStackInSlot(i);
            AcItemComponent assoc = null;
            if (!is.isEmpty() && is.getItem() instanceof AcItemComponent a) {
                assoc = a;
            }
            var oldTpl = luaComputerInventoryUserdataObjectsBySlotId.get(i);
            var old = oldTpl == null ? null : oldTpl.x();
            if (old != assoc) {
                this.tryKill("items arent hotswappable yet", false, true);
                // TODO invalidate the original user data object
            }

            // then add the fresh one
            if (assoc != null) {
                var ud = assoc.CreateUserdata(is);
                luaComputerInventoryUserdataObjectsBySlotId.put(i, new Tuple<>(assoc, ud));
            }
        }
    }

    static class LvmKillException extends RuntimeException {
    }

    public void markScreenForUpdate(ScreenBlockEntity toUpdate) {
        synchronized (screenBEsThatNeedUpdatingLock) {
            screenBEsThatNeedUpdating.add(toUpdate);
        }
    }

    public void sendScreenUpdatesToClients() {
        ScreenBlockEntity[] screensToUpdateNow;
        synchronized (screenBEsThatNeedUpdatingLock) {
            screensToUpdateNow = screenBEsThatNeedUpdating.toArray(ScreenBlockEntity[]::new);
            screenBEsThatNeedUpdating.clear();
        }
        for (ScreenBlockEntity sbe : screensToUpdateNow) {
            var textBuffer = this.gpuUD.getBufferForBlockEntity(sbe);
            String text = textBuffer.getTextAsString();
            NetCodeUtils.sendToClient(PacketDistributor.ALL.noArg(), new ScreenBlockEntity.ScreenContentToClientEvent(sbe, "setGuiText", text));
        }
    }

    static class ExecutionTimeTracker {
        long lastSafepointTimestamp = 0;
        long lastCompilationStartedTimestamp = 0;
        private final double sleepFactor;

        ExecutionTimeTracker(double sleepFactor) {
            this.sleepFactor = sleepFactor;
        }

        public void refundNanos(long nanos) {
            lastCompilationStartedTimestamp += nanos;
        }

        public void handleVmEvent(LuaVM vmObj, LuaVM.HookType eventType) {
            switch (eventType) {
                case COMPILATION_STARTED -> {
                    lastCompilationStartedTimestamp = System.nanoTime();
                }
                case COMPILATION_FINISHED -> {
                    // fake the last safepoint timestamp to effectively refund the compilation time
                    long timeSpentCompiling = System.nanoTime() - lastCompilationStartedTimestamp;
                    refundNanos(timeSpentCompiling);
                }
                case SAFEPOINT_REACHED -> {
                    long now = System.nanoTime();
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
