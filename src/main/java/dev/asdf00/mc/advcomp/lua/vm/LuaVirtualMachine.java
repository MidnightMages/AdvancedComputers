package dev.asdf00.mc.advcomp.lua.vm;

import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.functions.MixedStateFunctionRegistry;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.api.ItemCanBeInitialized;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.lua.ExtendedMixedStateFunctionRegistry;
import dev.asdf00.mc.advcomp.lua.LuaEventQueue;
import dev.asdf00.mc.advcomp.lua.components.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
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
    private LuaVM vm;
    private ComputerUD luaComputer;
    public ComponentRegistryUD componentReg;
    LuaSafepointHandler timeTracker;
    public String stopCode;
    final LinkedHashSet<TextBufferUD> dirtyBuffers = new LinkedHashSet<>();

    public LuaVirtualMachine(ComputerBlockEntity computerBlockEntity) {
        this.computerBlockEntity = computerBlockEntity;
        this.state = new VmRunStateHandler(computerBlockEntity);
    }

    // =================================================================================================================
    //      THREAD INDEPENDENT API     THREAD INDEPENDENT API     THREAD INDEPENDENT API     THREAD INDEPENDENT API
    // =================================================================================================================

    public void triggerMachineEvent(String eventName, LuaObject... args) {
        luaComputer.triggerMachineEvent(eventName, args);
    }

    public void tryKill(String reason) {
        synchronized (state) {
            if (state.getState().killable) {
                AdvancedComputers.LOGGER.error(reason);
                executorThread.interrupt();
            }
        }
    }

    // =================================================================================================================
    //    TICK THREAD     TICK THREAD     TICK THREAD     TICK THREAD     TICK THREAD     TICK THREAD     TICK THREAD
    // =================================================================================================================

    public void toggleOnOff() {
        synchronized (state) {
            if (state.getState().resting) {
                startCold();
            } else if (state.getState().killable) {
                tryKill("ON/OFF button to kill");
            } else {
                throw new UnsupportedOperationException("can not handle state toggling during " + state.getState().name());
            }
        }
    }

    public ItemStack onInventorySlotChanged(int slot) {
        // when we get a new slot item here, remove all existing components that occupy the slot and then add this new one and init it
        componentReg.removeAllComponentsInSlot(x -> x != null && x.getSlotIndex() == slot && x.getInventoryOwnerPos().equals(computerBlockEntity.getBlockPos()));
        // TODO should probs move this into component reg ud somehow to make it convenient to use for block components, but we'll see

        var newItemStack = computerBlockEntity.itemHandler.getStackInSlot(slot);
        var item = newItemStack.getItem();
        if (item instanceof ItemCanBeInitialized icbi) {
            icbi.Initialize(newItemStack);
        }

        if (item instanceof AcItemComponent ud) {
            var srcInfo = AcComponentSlotInfo.ofItemComponent(computerBlockEntity.getBlockPos(), slot);
            componentReg.addComponentInitAndNotify(ud.CreateUserdata(srcInfo), srcInfo);
        }

        return newItemStack;
    }

    public static LuaVirtualMachine deserializeOrNull(ComputerBlockEntity computerBlockEntity) {
        return null;
    }

    public void serialize() {

    }

    private void startCold() {
        synchronized (state) {
            coldInitialize();
            state.initializing();
            executorThread = new Thread(() -> {
                startLuaExecution();
            });
            executorThread.start();
        }
    }

    private void startFromState() {
        synchronized (state) {
            initializeFromState();
            // TODO
        }
    }

    private void coldInitialize() {
        synchronized (state) {
            if (!state.getState().resting) {
                throw new IllegalStateException("trying to initialize non-resting LVM");
            }
            state.initializing();
            AdvancedComputers.LOGGER.info("Trying to start LVM");

            // rebuild device cable cluster just in case
            CableCluster.onBlockPosChangedInternal(computerBlockEntity.getLevel(), computerBlockEntity.getBlockPos(), AdvancedComputers.CLUSTER_TYPE_DEVICE);

            // initialize state of 'this'
            timeTracker = new LuaSafepointHandler(this, computerBlockEntity.getTier().threadExecutionSleepFactor);
            luaComputer = new ComputerUD();
            componentReg = new ComponentRegistryUD(this);
            stopCode = "";

            // add builtin components
            componentReg.addComponentInitAndNotify(luaComputer, AcComponentSlotInfo.ofBlockComponent(computerBlockEntity));
            componentReg.addComponentInitAndNotify(new InternetUD(), AcComponentSlotInfo.ofBlockComponent(computerBlockEntity));
            componentReg.addComponentInitAndNotify(new GpuUD(), AcComponentSlotInfo.ofBlockComponent(computerBlockEntity));

            // set up inventory components
            String uefiScript = null; // entry code; i.e. uefi
            var inv = computerBlockEntity.itemHandler;
            for (int i = 0; i < inv.getSlots(); i++) {
                var is = onInventorySlotChanged(i);
                if (is.getItem() instanceof MainboardItem mi) {
                    uefiScript = mi.readUefiScript(is);
                }
            }
            if (uefiScript == null) {
                stopCode = "No uefi installed";
                state.crash();
                return;
            }

            // set up peripheral devices from IO-net
            computerBlockEntity.connectedNetworks.values().stream()
                    .filter(x -> x.clusterType.getClusterName().equals("device"))
                    .flatMap(x -> Arrays.stream(x.connectedEntities))
                    .distinct()
                    .forEach(be -> {
                        // add peripheral device to registry
                        if (be instanceof AcBlockEntityComponent bec) {
                            componentReg.addComponentInitAndNotify(bec.CreateUserdata(),
                                    AcComponentSlotInfo.ofBlockComponent((BlockEntity) be));
                        }
                        // clear all found screens
                        if (be instanceof ScreenBlockEntity sbe) {
                            NetCodeUtils.sendToClient(PacketDistributor.ALL.noArg(), new ScreenBlockEntity.ScreenContentToClientEvent(
                                    new ScreenBlockEntity[]{sbe}, "clearGuiText", ""));
                        }
                    });

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
        }
    }

    private void initializeFromState() {

    }


    // =================================================================================================================
    //       LUA THREAD     LUA THREAD     LUA THREAD     LUA THREAD     LUA THREAD     LUA THREAD     LUA THREAD
    // =================================================================================================================

    /**
     * Always call this method before executing a long-running or blocking LUA function to allow for proper syncing with
     * the client.
     */
    public void beforeLongLuaOperation() {
        timeTracker.beforeLongLuaOperation();
    }

    public void dirtyBuffer(TextBufferUD buf) {
        dirtyBuffers.add(buf);
    }

    private void startLuaExecution() {
        try {
            synchronized (state) {
                if (state.getState() != State.STARTING) {
                    throw new IllegalStateException("not starting from proper state");
                }
                state.startRun();
            }
            try {
                LuaVM.VmResult res = vm.run();
                switch (res.state()) {
                    case SUCCESS -> {
                        state.stop();
                        AdvancedComputers.LOGGER.info("vm exited with result: %s".formatted(res.toString().replace("\\n", "\n")));
                    }
                    case EXECUTION_ERROR -> {
                        state.crash();
                        AdvancedComputers.LOGGER.error("vm exited with error: %s".formatted(res.toString().replace("\\n", "\n")));
                    }
                    case PAUSED -> {
                        state.suspend();
                        AdvancedComputers.LOGGER.info("vm paused");
                    }
                }
            } catch (LvmKillException kill) {
                state.crash();
                AdvancedComputers.LOGGER.error("vm killed");
            }
        } catch (Exception ex) {
            state.crash();
            AdvancedComputers.LOGGER.error("caught lvm exception: ", ex);
        }
    }

    // =================================================================================================================
    //      STATICS     STATICS     STATICS     STATICS     STATICS     STATICS     STATICS     STATICS     STATICS
    // =================================================================================================================

    private static final MixedStateFunctionRegistry BUILTIN_FUNCTIONS;

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
}
