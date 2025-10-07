package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.lua.components.ComponentRegistryUD;
import dev.asdf00.mc.advcomp.lua.components.ComputerUD;
import dev.asdf00.mc.advcomp.lua.components.IsAssociatedWithLuaUserdata;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.utils.Tuple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LuaVirtualMachine {
    private static final int TPS = 20;
    private static final String luaBootScript;
    public LuaEventQueue eventQueue;

    private static String loadLuaScript(String name) {
        try (var stream = LuaMain.class.getClassLoader().getResourceAsStream("assets/advancedcomputers/lua/" + name)) {
            Objects.requireNonNull(stream, "Error reading resource '%s'".formatted(name));
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Resource '%s' not found!".formatted(name));
        }
    }

    static {
        luaBootScript = loadLuaScript("boot.lua");
    }

    private final ComputerBlockEntity computer;

    private LuaVM vm;
    private final int ipt; // instructions per tick
    private long timeLastHook = 0;

    private final Object startStopLock = new Object();
    private volatile boolean suspended;
    private volatile boolean isRunning;
    private volatile Thread executionEnv;

    private LuaStdOut stdOut;
    private String stopCode;
    private boolean stopCode_isGraceful = false;

    private Consumer<Boolean> runOnExit = null;
    private volatile boolean killingLVM = false;

    public ComputerUD computerUD = null;


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
        AdvancedComputers.LOGGER.info("Starting LVM");
        synchronized (startStopLock) {
            if (isRunning) {
                throw new IllegalStateException("LVM is already running");
            }
            isRunning = true;
            stdOut = new LuaStdOut();
            executionEnv = new Thread(this::runLua);
            stopCode = "";
            stopCode_isGraceful = false;
        }
        executionEnv.start();
    }

    public void addComponent(LuaUserData component) {

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
            if (executionEnv == null) {
                throw new IllegalStateException("no execution environment to resume");
            }
            var prev = suspended;
            suspended = false;
            if (prev) {
                // only grant unpark permit if thread was parked in the first place
                LockSupport.unpark(executionEnv);
            }
        }
    }

    public void tryKill(String reason, boolean isGracefulShutdown) {
        synchronized (startStopLock) {
            if (isRunning) {
                synchronized (startStopLock) {
                    killingLVM = true;
                    stopCode = "[KILLED] " + reason;
                    stopCode_isGraceful = isGracefulShutdown;

                    executionEnv.interrupt();
                    if (suspended) {
                        resume();
                    }

                    // cleanup after kill
                    isRunning = false;
                    executionEnv = null;
                }
            }
        }
    }

    public void toggleOnOff(Runnable onStart, Consumer<Boolean> onExit) {
        synchronized (startStopLock) {
            if (getState() == 0) {
                runOnExit = onExit;
                onStart.run();
                start();
            } else {
                tryKill("ON/OFF button pushed", true);
            }
        }
    }

    boolean isBeingKilled() {
        return killingLVM;
    }

    private void runLua() {
        stdOut = new LuaStdOut();
        stdOut.clear();
        AdvancedComputers.LOGGER.info("trying to start LVM");


        String bootFile = luaBootScript; // entry code

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
        var inv = computer.itemHandler;
        for (int i = 0; i < inv.getSlots(); i++) {
            var is = inv.getStackInSlot(i);
            var item = is.getItem();
            if (item instanceof IsAssociatedWithLuaUserdata ud) {
                componentReg.addComponentAndNotify(ud.CreateUserdata(is));
            }
        }

        //componentReg.registerComponent(new InternetUD());
        //componentReg.registerComponent(new BiosUD());
        componentReg.addComponentAndNotify(new ComputerUD(this));
        // --------------------------
        // DEFINE GLOBALS
        var greg = new ExtendedMixedStateFunctionRegistry("advancedcomputers");
        greg.register("sleep", AtomicLuaFunction.forZeroResults(greg, (vm, time) -> {
            try {
                Thread.sleep((int) (time.asDouble() * 1000));
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
        _G.set("component", LuaObject.of(componentReg));

// TODO set stopCode and stopCode_isGraceful

//        setGlobalFunction("setStopCode", new LuaFunctionProxy(this, (Object[] args) -> {
//            var msg = Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" "));
//            System.out.println("setStopCode: " + msg);
//            stopCode = msg;
//            stopCode_isGraceful = false; // TODO expose this as an arg to the vm maybe?
//        }));


        boolean lvmException = false;
        boolean lvmCleanExit = false;
//        try {
//            vm.withRootFunc(luaEntryScript);
//            var rv = vm.run();
//            AdvancedComputers.LOGGER.info(String.format("LVM exited with code %s", rv));
//            lvmCleanExit = rv.state() == LuaVM.VmRunState.SUCCESS;
//        } catch (Exception e) {
//            AdvancedComputers.LOGGER.error(e.toString());
//            lvmException = true;
//        }


        vm = LuaVM.builder().withApiRegistry(greg).modifyEnv(t -> {
            var map = _G.asMap();
            for (var k : map.keys()) {
                t.set(k, map.getOrDefault(k, LuaObject.NIL));
            }
        }).rootFunc(bootFile).build();try {
            var res = vm.run();

            if (res.state() == LuaVM.VmRunState.EXECUTION_ERROR) {
                AdvancedComputers.LOGGER.error("vm exited with error: %s".formatted(res.toString().replace("\\n","\n")));
            } else {
                AdvancedComputers.LOGGER.info("vm exited with result: %s".formatted(res.toString().replace("\\n","\n")));
            }
        } catch (Exception ex) {
            AdvancedComputers.LOGGER.error("caught lvm exception: ",ex);
        }

        boolean shutdownWasGraceful;
        // cleanup after shutdown
        synchronized (startStopLock) {
            killingLVM = false;
            isRunning = false;
            executionEnv = null;
            stdOut = null;

            if (lvmCleanExit)
                shutdownWasGraceful = true;
            else if (lvmException){
                shutdownWasGraceful = false;
            }
            else {
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

    private final HashMap<Integer, Tuple<IsAssociatedWithLuaUserdata,LuaUserDataComponent>> luaComputerInventoryUserdataObjectsBySlotId = new HashMap<>();

    public void rebuildUserdataFromInventory() {
        var slots = computer.itemHandler.getSlots();
        for (int i = 0; i < slots; i++) {
            var is = computer.itemHandler.getStackInSlot(i);
            IsAssociatedWithLuaUserdata assoc = null;
            if(!is.isEmpty() && is.getItem() instanceof IsAssociatedWithLuaUserdata a) {
                assoc = a;
            }
            var oldTpl = luaComputerInventoryUserdataObjectsBySlotId.get(i);
            var old = oldTpl == null ? null : oldTpl.x();
            if (old != assoc)
            {
                // TODO invalidate the original user data object
            }

            // then add the fresh one
            if (assoc != null)
            {
                var ud = assoc.CreateUserdata(is);
                luaComputerInventoryUserdataObjectsBySlotId.put(i, new Tuple<>(assoc, ud));
            }
        }
    }

    static class LvmKillException extends RuntimeException {
    }
}
