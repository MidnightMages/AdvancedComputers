package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class LuaVirtualMachine {
    private static final int TPS = 20;
    private static final String luaEntryScript;
    private static final String luaShellScript;

    private static String loadLuaScript(String name) {
        try (var stream = LuaMain.class.getClassLoader().getResourceAsStream("assets/advancedcomputers/lua/" + name)) {
            Objects.requireNonNull(stream, "Error reading resource '%s'".formatted(name));
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Resource '%s' not found!".formatted(name));
        }
    }

    static {
        luaEntryScript = loadLuaScript("entry.lua");
        luaShellScript = loadLuaScript("luaShell.lua");
    }

    private final ComputerBlockEntity computer;

    private final LuaVM vm;
    private final int ipt;
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

    private record MachineEvent(String name, Object content) {
    }

    private final ArrayDeque<MachineEvent> machineEvents = new ArrayDeque<>();
    private final Set<String> subbedEvents = new HashSet<>();

    public LuaVirtualMachine(ComputerBlockEntity computer, int instructionsPerSecond) {
        this.computer = computer;
//        vm = LuaVM.create().withStdLib();
        vm = null;
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
        machineEvents.clear();
        AdvancedComputers.LOGGER.info("trying to start LVM");
//        var g = vm.get_G();
//        g.set("print", AtomicLuaFunction.vaForZeroResults((vm, args) ->
//                sandboxLog(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")), true, false)).obj());
//        g.set("printInline", AtomicLuaFunction.vaForZeroResults((vm, args) ->
//                sandboxLog(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")), false, false)).obj());
//        g.set("printErr", AtomicLuaFunction.vaForZeroResults((vm, args) ->
//                sandboxLog(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")), true, true)).obj());
//        g.set("clear",AtomicLuaFunction.vaForZeroResults((vm, a) -> stdOut.clear()).obj()); // TODO make this a non-va function

// TODO set stopCode and stopCode_isGraceful

//        setGlobalFunction("setStopCode", new LuaFunctionProxy(this, (Object[] args) -> {
//            var msg = Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" "));
//            System.out.println("setStopCode: " + msg);
//            stopCode = msg;
//            stopCode_isGraceful = false; // TODO expose this as an arg to the vm maybe?
//        }));

//        setGlobalFunction("unsubMachineEvent", new LuaFunctionProxy(this, this::unsubMachineEvent));
//        setGlobalFunction("subMachineEvent", new LuaFunctionProxy(this, this::subMachineEvent));
//        setGlobalFunction("getMachineEvent", new LuaFunctionProxy(this, this::getMachineEvent));
//        setGlobalFunction("waitForMachineEvent", new LuaFunctionProxy(this, this::waitForMachineEvent));
//        setGlobalFunction("sleep", new LuaFunctionProxy(this, (Object[] args) -> {
//            if (args.length != 1) {
//                throw new AcLuaException("'sleep' expects 1 timeout argument");
//            }
//            if (args[0] instanceof Double d) {
//                try {
//                    Thread.sleep((long) (d * 1000));
//                } catch (InterruptedException e) {
//                    throw interruptAndKillCurrent();
//                }
//            } else {
//                throw new AcLuaException("'sleep' expects number as argument");
//            }
//        }));

        //L.openLibrary("io"); // TODO make custom implementation
        //L.openLibrary("os"); // TODO make custom implementation
        //L.openLibrary("package"); // TODO make custom implementation


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


    /**
     * Called about every tick via a lua debug count hook. Used to create an artificial slowdown for Lua PCs.
     */
    public void sandboxCountHookCallback(Object[] args) {
        LockSupport.parkNanos(1_000_000 / TPS - 1000 * (System.currentTimeMillis() - timeLastHook));
        var curThread = Thread.currentThread();
        if (curThread.isInterrupted()) {
            throw interruptAndKillCurrent();
        }
        boolean isInterrupted = false;
        while (suspended) {
            LockSupport.park();
            if (curThread.isInterrupted()) {
                // if the executor thread gets interrupted while the LVM is suspended, break suspension and continue the interrupt
                isInterrupted = true;
                break;
            }
        }
        if (isInterrupted) {
            throw interruptAndKillCurrent();
        }
        timeLastHook = System.currentTimeMillis();
    }

    public void subMachineEvent(Object[] args) {
        String name;
        try {
            name = (String) args[0];
        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            throw new AcLuaException("Syntax: subMachineEvent(<name>)");
        }
        synchronized (machineEvents) {
            subbedEvents.add(name);
        }
    }

    public void unsubMachineEvent(Object[] args) {
        String name;
        boolean evict;
        try {
            name = (String) args[0];
            evict = args.length > 1 ? (Boolean) args[1] : false;
        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            throw new AcLuaException("Syntax: unsubMachineEvent(<name> [, <evict>])");
        }
        synchronized (machineEvents) {
            subbedEvents.remove(name);
            if (evict) {
                machineEvents.removeIf(e -> e.name().equals(name));
            }
        }
    }

    public boolean pushMachineEvent(String name, Object event) {
        synchronized (machineEvents) {
            if (subbedEvents.contains(name)) {
                machineEvents.add(new MachineEvent(name, event));
                machineEvents.notifyAll();
                return true;
            }
            return false;
        }
    }

    private Object[] getMachineEvent(Object[] ignore) {
        if (ignore.length != 0) {
            throw new AcLuaException("cannot pass pass arguments to 'getMachineEvent'");
        }
        MachineEvent event;
        synchronized (machineEvents) {
            event = machineEvents.poll();
        }
        return event == null ? new Object[]{null} : new Object[]{event.name, event.content};
    }

    private void waitForMachineEvent(Object[] timeout) {
        if (timeout.length > 1) {
            throw new AcLuaException("'waitForMachineEvent' expects either no argument or 1 timeout argument");
        }
        try {
            synchronized (machineEvents) {
                if (machineEvents.isEmpty()) {
                    if (timeout.length == 1) {
                        machineEvents.wait(Long.parseLong(timeout[0].toString()));
                    } else {
                        machineEvents.wait();
                    }
                }
            }
        } catch (InterruptedException e) {
            throw interruptAndKillCurrent();
        }
    }

    private static RuntimeException interruptAndKillCurrent() {
        Thread.currentThread().interrupt();
        throw new LvmKillException();
    }

    static class LvmKillException extends RuntimeException {
    }
}
