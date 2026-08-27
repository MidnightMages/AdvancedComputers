package dev.asdf00.mc.advcomp.lua.vm;

import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.functions.MixedStateFunctionRegistry;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.Config;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.api.ItemCanBeInitialized;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.blocks.punchcard_reader.PunchcardReaderBlockUD;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.lua.adapterapi.AdapterCompanion;
import dev.asdf00.mc.advcomp.lua.components.*;
import dev.asdf00.mc.advcomp.utils.AcPaths;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

public class LuaVirtualMachine {

    /**
     * The computer that this vm is running on.
     */
    public final ComputerBlockEntity computerBlockEntity;
    private final VmRunStateHandler state;

    // run state
    private volatile Thread executorThread;

    // execution state
    LuaVM vm;
    ComputerUD luaComputer;
    ComponentRegistryUD componentReg;
    LuaSafepointHandler timeTracker;
    public String stopCode;
    final LinkedHashSet<TextBufferUD> dirtyBuffers = new LinkedHashSet<>();
    final ConcurrentLinkedQueue<ScreenBlockEntity> dirtyScreenBlockEntities = new ConcurrentLinkedQueue<>();
    private boolean suppressDeviceNetworkUpdate = false;
    final PriorityBlockingQueue<DelayedQueuedEvent> delayedEventQueue = new PriorityBlockingQueue<>();

    public LuaVirtualMachine(ComputerBlockEntity computerBlockEntity) {
        this.computerBlockEntity = computerBlockEntity;
        this.state = new VmRunStateHandler(computerBlockEntity);
    }

    // =================================================================================================================
    //      THREAD INDEPENDENT API     THREAD INDEPENDENT API     THREAD INDEPENDENT API     THREAD INDEPENDENT API
    // =================================================================================================================

    public void triggerMachineEvent(String eventName, LuaObject... args) {
        // If this event is a "network packet received"-event, check if the destination port is actually open.
        // We do this here instead of during sending, as there is a slight delay between sending and the packet arriving which would open a sidechannel
        // allowing the receiver to estimate the distance of the sender in certain cases.
        if (NetworkUD.MESSAGE_RECEIVED_EVENT_NAME.equals(eventName)) {
            var destinationPortArg = args[1];
            RuntimeAssert.RuntimeAssert(destinationPortArg.isLong(), "expected this to be the port argument of the network message received event.");
            var destinationPort = destinationPortArg.asLong();
            RuntimeAssert.RuntimeAssert(destinationPort >= 0 && destinationPort <= Short.MAX_VALUE, "expected a valid 'short' port value.");

            var netUd = componentReg.getSingleOfType(NetworkUD.class);
            if (!netUd.canReceivePacketOnPort((int) destinationPort)) {
                return; // skip event processing
            }
        }
        luaComputer.triggerMachineEvent(eventName, args);
    }

    public void queueDelayedMachineEvent(long delayByMs, String eventName, LuaObject... args) {
        this.delayedEventQueue.add(new DelayedQueuedEvent(eventName, args, System.currentTimeMillis() + delayByMs));
    }

    // SOLE CALLER IS LuaSafepointHandler.
    void processDelayedEventsAtSafepoint() {
        while (!delayedEventQueue.isEmpty()) { // loop until we have no more elements, or we find an event that we shouldnt emit yet
            var peek = delayedEventQueue.peek();
            if (peek.shallEmitNow()) {
                delayedEventQueue.remove();
                triggerMachineEvent(peek.eventName, peek.args);
            } else {
                break;
            }
        }
    }

    public void requestScreenContents(ScreenBlockEntity sbe) {
        dirtyScreenBlockEntities.add(sbe);
    }

    public void tryKill(String reason) {
        synchronized (state) {
            if (state.getState().killable) {
                if (Config.debugEnableExtraDevelopmentLogging)
                    AdvancedComputers.LOGGER.warn("Killing vm for reason: '%s'".formatted(reason));
                executorThread.interrupt();
            }
        }
    }

    public State getState() {
        synchronized (state) {
            return state.getState();
        }
    }

    private record DelayedQueuedEvent(String eventName, LuaObject[] args, long emitAtEpoch) implements Comparable<DelayedQueuedEvent> {
        @Override
        public int compareTo(@NotNull LuaVirtualMachine.DelayedQueuedEvent o) {
            return Long.compare(emitAtEpoch, o.emitAtEpoch);
        }

        public boolean shallEmitNow() {
            return System.currentTimeMillis() >= emitAtEpoch;
        }
    }

    // =================================================================================================================
    //    TICK THREAD     TICK THREAD     TICK THREAD     TICK THREAD     TICK THREAD     TICK THREAD     TICK THREAD
    // =================================================================================================================

    public void toggleOnOff() {
        synchronized (state) {
            if (state.getState().resting) {
                // initialize cold start
                start(null);
            } else if (state.getState().killable) {
                tryKill("ON/OFF button to kill");
            } else {
                throw new UnsupportedOperationException("can not handle state toggling during " + state.getState().name());
            }
        }
    }

    public ItemStack onInventorySlotChanged(int slot) {
        // if the first slot was modified (mainboard), immediately crash the ocmputer
        if (slot == 0 && state.getState().equals(State.RUNNING)) {
            tryKill("mainboard was removed");
            return null;
        }

        // when we get a new slot item here, remove all existing components that occupy the slot and then add this new one and init it
        componentReg.removeAllMatchingComponents(x -> x != null && x.getSlotIndex() == slot && x.getInventoryOwnerPos().equals(computerBlockEntity.getBlockPos()));

        var newItemStack = computerBlockEntity.itemHandler.getStackInSlot(slot);
        var item = newItemStack.getItem();
        if (item instanceof ItemCanBeInitialized icbi) {
            icbi.initialize(newItemStack);
        }

        if (item instanceof AcItemComponent ud) {
            var srcInfo = AcComponentSlotInfo.ofItemComponent(computerBlockEntity.getBlockPos(), slot);
            componentReg.addComponentInitAndNotify(ud.CreateUserdata(srcInfo), srcInfo);
        }

        return newItemStack;
    }

    public void onBlockComponentRemoved(BlockEntity blockEntity) {
        if (suppressDeviceNetworkUpdate || componentReg == null) return;

        if (Config.debugEnableExtraDevelopmentLogging)
            AdvancedComputers.LOGGER.warn("Removing block component %s".formatted(blockEntity.toString()));
        componentReg.removeAllMatchingComponents(x -> x != null && x.getInventoryOwnerPos().equals(blockEntity.getBlockPos()));
    }

    public <T extends BlockEntity & AcBlockEntityComponent> void onBlockComponentAdded(T blockEntity) {
        if (suppressDeviceNetworkUpdate || componentReg == null) return;

        if (Config.debugEnableExtraDevelopmentLogging)
            AdvancedComputers.LOGGER.warn("Adding block component %s".formatted(blockEntity.toString()));
        var blockEntityUD = blockEntity.createUserdata();
        componentReg.addComponentInitAndNotify(blockEntityUD, AcComponentSlotInfo.ofBlockComponent(blockEntity));
    }

    public static LuaVirtualMachine deserializeOrNull(ComputerBlockEntity computerBlockEntity) {
        var serializedVmPath = AcPaths.getVmStatesPath(computerBlockEntity);
        var vmExists = Files.exists(serializedVmPath);
        if (!vmExists) return null;

        // deserialize
        var vm = new LuaVirtualMachine(computerBlockEntity);
        try {
            vm.start(Files.readAllBytes(serializedVmPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            // on error, give up and return null instead
            vm = null;
            var exceptionAsString = e + "\n" + Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
            AdvancedComputers.LOGGER.error(("Failed to deserialize vm, please report this unless the mod version changed since last time the world was saved. " +
                                            "Original exception:\n%s").formatted(exceptionAsString));
        }

        try {
            Files.delete(serializedVmPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return vm;
    }

    /**
     * This method stops execution on the LUA thread and subsequently serializes the entire state of the VM.
     *
     * @throws InterruptedException if an interrupt occurs while waiting on the LUA thread to stop.
     */
    public void serialize() throws InterruptedException {
        synchronized (state) {
            if (!state.getState().resting) {
                state.suspendAndWait(() -> {
                    vm.requestStop();
                    executorThread.interrupt();
                });
            }
            if (state.getState() != State.SUSPENDED) {
                // this thread is not suspended -> it is not in a serializable condition
                return;
            }

            // The VM is suspended, we may now serialize the VM.
            // We keep the lock because the state must not be changed during serialization.
            byte[] serializedState = vm.serialize(this);
            try {
                Files.write(AcPaths.getVmStatesPath(computerBlockEntity), serializedState);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This method starts this VM either from a fully OFF state or from some serialized state passed as a parameter
     *
     * @param serializedState the serialized state of an earlier run of this VM or {@code null} if this is a cold boot.
     */
    private void start(byte[] serializedState) {
        boolean isCold = serializedState == null;
        synchronized (state) {
            boolean initSuccess = false;
            try {
                if (isCold) {
                    coldInitialize();
                } else {
                    initializeFromState(serializedState);
                }
                initSuccess = true;
            } finally {
                if (!initSuccess && state.getState() != State.CRASHED) {
                    stopCode = "Unknown";
                    state.crash();
                }
            }
            if (state.getState() != State.CRASHED) {
                executorThread = new Thread(this::startLuaExecution);
                executorThread.start();
            } else if (Config.debugEnableExtraDevelopmentLogging) {
                AdvancedComputers.LOGGER.warn("Computer failed to start: %s".formatted(stopCode));
            }
        }
    }

    private boolean tooManyComputersConnected() {
        CableCluster deviceCluster = null;
        for (var cluster : computerBlockEntity.connectedNetworks.values()) {
            if (cluster.getClusterType().equals(AdvancedComputers.CLUSTER_TYPE_DEVICE)) {
                if (cluster == deviceCluster)
                    continue;

                if (deviceCluster != null)
                    throw new IllegalStateException("somehow there were multiple device clusters, even though this block is supposed to act as a cable");

                deviceCluster = cluster;
            }
        }

        // there are too many clusters if we found a cluster and it has too many hosts
        return deviceCluster != null && (deviceCluster.getHostCount() > 1);
    }

    private void coldInitialize() {
        synchronized (state) {
            if (!state.getState().resting) {
                throw new IllegalStateException("trying to initialize non-resting LVM");
            }
            if (Config.debugEnableExtraDevelopmentLogging)
                AdvancedComputers.LOGGER.info("Trying to start LVM (cold boot)");
            state.initialize();

            // rebuild device cable cluster just in case
            suppressDeviceNetworkUpdate = true;
            CableCluster.rebuildDeviceNetImmediately(computerBlockEntity.getLevel(), computerBlockEntity.getBlockPos(), AdvancedComputers.CLUSTER_TYPE_DEVICE);
            suppressDeviceNetworkUpdate = false;

            if (tooManyComputersConnected()) {
                stopCode = "More than one computer in device network";
                state.crash();
                return;
            }

            // initialize state of 'this'
            timeTracker = new LuaSafepointHandler(this, computerBlockEntity.getTier().threadExecutionSleepFactor);
            luaComputer = new ComputerUD();
            componentReg = new ComponentRegistryUD(this);
            stopCode = "";

            // add builtin components
            componentReg.addComponentInitAndNotify(luaComputer, null);
            componentReg.addComponentInitAndNotify(new InternetUD(), null);
            componentReg.addComponentInitAndNotify(new NetworkUD(), null);
            componentReg.addComponentInitAndNotify(new GpuUD(), null);

            // set up inventory components
            MainboardItem.MainboardInfo mainboardInfo = null; // for grabbing the uefi code
            var inv = computerBlockEntity.itemHandler;
            for (int i = 0; i < inv.getSlots(); i++) {
                var is = onInventorySlotChanged(i);
                if (is.getItem() instanceof MainboardItem mi) {
                    mainboardInfo = mi.getInfo(is);
                }
            }

            // add mainboard userdata objects to computerUD
            if (mainboardInfo != null)
                luaComputer.setupMainboard(mainboardInfo);

            // set up peripheral devices from IO-net

            computerBlockEntity.existingBlockComponents.clear();
            computerBlockEntity.connectedNetworks.values().stream()
                    .filter(x -> x.clusterType.getClusterName().equals("device"))
                    .flatMap(x -> Arrays.stream(x.connectedEntities))
                    .distinct()
                    .forEach(be -> {
                        // add peripheral device to registry
                        if (be instanceof AcBlockEntityComponent bec) {
                            onBlockComponentAdded((BlockEntity & AcBlockEntityComponent) bec);
                            computerBlockEntity.existingBlockComponents.add(bec);
                        }
                        // clear all found screens
                        if (be instanceof ScreenBlockEntity sbe) {
                            NetCodeUtils.sendToClient(PacketDistributor.ALL.noArg(), new ScreenBlockEntity.ScreenContentToClientEvent(
                                    new ScreenBlockEntity[]{sbe}, "clearGuiText", ""));
                        }
                    });


            // figure out what script to load
            String uefiScript = null;
            // try punchcard first
            var firstPunchCardReader = componentReg.getFirst("punchcardReader");
            if (!firstPunchCardReader.isNil()) {
                try {
                    uefiScript = ((PunchcardReaderBlockUD) firstPunchCardReader.refVal).read_tickThread(true);
                } catch (LuaJavaError ignored) {
                }
            }
            // otherwise fall back to regular uefi
            if (uefiScript == null) {
                if (mainboardInfo == null) { // crash if we odnt have a mainboard
                    stopCode = "No mainboard installed";
                    state.crash();
                    return;
                }
                uefiScript = ((UefiUD) luaComputer.uefi.refVal).getUefiScript();
            }


            // build lua virtual machine
            vm = LuaVM.builder().withApiRegistry(BUILTIN_FUNCTIONS).modifyEnv(_G -> {
                // add custom builtins
                _G.set("components", LuaObject.of(componentReg));
                _G.set("_HOST", LuaObject.of("AdvancedComputers %s; Minecraft %s".formatted(
                        AdvancedComputers.getModVersion(), AdvancedComputers.getMinecraftVersion())));
                _G.set("print", LuaObject.of(BUILTIN_FUNCTIONS.getFunction("print")));
                _G.set("printInline", LuaObject.of(BUILTIN_FUNCTIONS.getFunction("printInline")));
                _G.set("sleep", LuaObject.of(BUILTIN_FUNCTIONS.getFunction("sleep", LuaObject.of(timeTracker))));

                // remove _G.vm.pause
                _G.get("vm").set("pause", LuaObject.nil());
            }).rootFunc(uefiScript).build();
            vm.eventCallback = timeTracker::handleVmEvent;

            // now the VM is initialized for a cold start
        }
    }

    private void initializeFromState(byte[] serializedState) {
        synchronized (state) {
            if (state.getState() != State.UNINITIALIZED) {
                throw new IllegalStateException("trying to initialize non-resting LVM");
            }
            if (Config.debugEnableExtraDevelopmentLogging)
                AdvancedComputers.LOGGER.info("Trying to load suspended LVM (deserialized boot)");

            computerBlockEntity.existingBlockComponents = null; // this is a special state to prevent re-registration of userdata objects during the first network discovery
            // rebuild device cable cluster just in case
            CableCluster.rebuildDeviceNetImmediately(computerBlockEntity.getLevel(), computerBlockEntity.getBlockPos(), AdvancedComputers.CLUSTER_TYPE_DEVICE);

            // initialize state of 'this'
            timeTracker = new LuaSafepointHandler(this, computerBlockEntity.getTier().threadExecutionSleepFactor);
            stopCode = "";
            // luaComputer and componentReg are initialized automatically during deserization

            // deserialize lua VM
            vm = LuaVM.builder()
                    .withApiRegistry(BUILTIN_FUNCTIONS)
                    .withApiRegistry(AdapterCompanion.ADAPTER_FUNCTION_REGISTRY)
                    .fromState(serializedState, this)
                    .build();
            vm.eventCallback = timeTracker::handleVmEvent;

            // now the vm is suspended
            state.suspend();
        }
    }

    // =================================================================================================================
    //       LUA THREAD     LUA THREAD     LUA THREAD     LUA THREAD     LUA THREAD     LUA THREAD     LUA THREAD
    // =================================================================================================================

    /**
     * Blocks the lua thread for the given amount of time, but then refunds execution time as to not stall execution after.
     */
    public void performSleepAndRefundTime(double seconds) {
        try {
            long sleepBegunAt = System.nanoTime();
            beforeLongLuaOperation();
            Thread.sleep((int) (seconds * 1000));
            long sleptForNs = Math.max(0, System.nanoTime() - sleepBegunAt);
            timeTracker.refundNanos(sleptForNs);
        } catch (InterruptedException e) {
            // premature exit, preserve interrupted state
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Always call this method before executing a long-running or blocking LUA function to allow for proper syncing with
     * the client.
     */
    public void beforeLongLuaOperation() {
        timeTracker.beforeLongLuaOperation();
    }

    public void dirtyBuffer(@NotNull TextBufferUD buf) {
        //noinspection ConstantValue
        RuntimeAssert.RuntimeAssert(buf != null, "buf was null");
        dirtyBuffers.add(buf);
    }

    private void startLuaExecution() {
        try {
            boolean isResuming;
            synchronized (state) {
                if (!state.getState().startable) {
                    throw new IllegalStateException("not starting from proper state");
                }
                isResuming = state.getState() == State.SUSPENDED;
                state.startRun();
            }
            try {
                // continue on suspend, run on start
                LuaVM.VmResult res = isResuming ? vm.runContinue() : vm.run();
                switch (res.state()) {
                    case SUCCESS -> {
                        state.stop();
                        if (Config.debugEnableExtraDevelopmentLogging)
                            AdvancedComputers.LOGGER.info("vm exited with result: %s".formatted(res.toString().replace("\\n", "\n")));
                    }
                    case EXECUTION_ERROR -> {
                        state.crash();
                        stopCode = "\n" + Arrays.stream(res.returnVars())
                                .filter(LuaObject::isString)
                                .map(LuaObject::getString)
                                .collect(Collectors.joining("\n"));

                        if (Config.debugEnableExtraDevelopmentLogging)
                            AdvancedComputers.LOGGER.error("vm exited with error: %s".formatted(res.toString().replace("\\n", "\n")));
                    }
                    case PAUSED -> {
                        state.suspend();
                        if (Config.debugEnableExtraDevelopmentLogging)
                            AdvancedComputers.LOGGER.info("vm paused");
                    }
                }
            } catch (LvmKillException kill) {
                state.crash();
                if (Config.debugEnableExtraDevelopmentLogging)
                    AdvancedComputers.LOGGER.error("vm killed");
            }
        } catch (Exception ex) {
            state.crash();
            AdvancedComputers.LOGGER.error("caught lvm exception: ", ex);
        }
    }

    // =================================================================================================================
    //       INTERNAL HELPERS     INTERNAL HELPERS     INTERNAL HELPERS     INTERNAL HELPERS     INTERNAL HELPERS
    // =================================================================================================================

    public void onUdDeserialize(ComputerUD luaComputer) {
        this.luaComputer = luaComputer;
    }

    public void onUdDeserialize(ComponentRegistryUD componentReg) {
        this.componentReg = componentReg;
    }

    // =================================================================================================================
    //      STATICS     STATICS     STATICS     STATICS     STATICS     STATICS     STATICS     STATICS     STATICS
    // =================================================================================================================

    public static final MixedStateFunctionRegistry BUILTIN_FUNCTIONS;

    static {
        BUILTIN_FUNCTIONS = new MixedStateFunctionRegistry("advancedcomputers.builtins");
        BUILTIN_FUNCTIONS.register("print",
                AtomicLuaFunction.vaForZeroResults(BUILTIN_FUNCTIONS, (vm, args) ->
                        printlnLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));
        BUILTIN_FUNCTIONS.register("printInline",
                AtomicLuaFunction.vaForZeroResults(BUILTIN_FUNCTIONS, (vm, args) ->
                        printInlineLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));
        BUILTIN_FUNCTIONS.register("sleep", LuaSleepFunction.class,
                tracker -> new LuaSleepFunction(BUILTIN_FUNCTIONS, tracker));

        // general purpose iterator that returns one set of values after another
        BUILTIN_FUNCTIONS.register("$internal.unpacking_iterator", LuaUnpackingIteratorFunction.class,
                (tableToIterateOver, closures) -> new LuaUnpackingIteratorFunction(BUILTIN_FUNCTIONS, tableToIterateOver, closures));
    }

    private static void printlnLUA(String s) {
        if (Config.debugLuaPrintToServerConsole)
            System.out.println(s);
    }

    private static void printInlineLUA(String s) {
        if (Config.debugLuaPrintToServerConsole)
            System.out.print(s);
    }
    // ------------
}
