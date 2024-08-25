package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import party.iroiro.luajava.AbstractLua;
import party.iroiro.luajava.JFunction;
import party.iroiro.luajava.lua54.Lua54;
import party.iroiro.luajava.value.RefLuaValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static dev.asdf00.mc.advcomp.lua.LuaUtils.setGlobalField;

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

    private final AbstractLua L;
    private final int ipt;
    private RefLuaValue L_eventCallbackRef;
    private long timeLastHook = 0;

    private final Object startStopLock = new Object();
    private volatile boolean suspended;
    private volatile boolean isRunning;
    private volatile Thread executionEnv;

    private LuaStdOut stdOut;
    private String stopCode;

    private record MachineEvent(String name, Object content) {
    }

    private final ArrayDeque<MachineEvent> machineEvents = new ArrayDeque<>();
    private final Set<String> subbedEvents = new HashSet<>();

    public LuaVirtualMachine(ComputerBlockEntity computer, int instructionsPerSecond) {
        this.computer = computer;
        L = new Lua54();
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

    public void setGlobalFunction(String funcName, JFunction callback) {
        L.push(callback);
        L.setGlobal(funcName);
    }

    public void pushEventIntoSandbox(String name, Object[] args) {
        // L.newThread(); // shouldn't be needed I think??
        L_eventCallbackRef.push();
        L.push(name);
        LuaUtils.pushArgs(L, args);
        var status = L.resume(args.length + 1);
        sandboxLog("CO status: " + status, false);
    }

    private void setEventCallback(Object[] args) {
        L_eventCallbackRef = (RefLuaValue) args[0];
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

    public void tryKill(String reason) {
        synchronized (startStopLock) {
            if (isRunning) {
                synchronized (startStopLock) {
                    executionEnv.interrupt();
                    if (suspended) {
                        resume();
                    }

                    // cleanup after kill
                    isRunning = false;
                    executionEnv = null;
                    stdOut = null;
                    stopCode = "[KILLED] " + reason;
                }
            }
        }
    }

    public void toggleOnOff() {
        synchronized (startStopLock) {
            if (getState() == 0) {
                start();
            } else {
                tryKill("ON/OFF button pushed");
            }
        }
    }

    private void runLua() {
        stdOut = new LuaStdOut();
        stdOut.clear();
        machineEvents.clear();
        AdvancedComputers.LOGGER.info("trying to start LVM");
        setGlobalFunction("print", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")), false)));
        setGlobalFunction("printInline", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")), false, false)));
        setGlobalFunction("printErr", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")), true)));
        setGlobalFunction("clear", new LuaFunctionProxy((Object[] o) -> stdOut.clear()));

        setGlobalFunction("sandboxCountHookCallback", new LuaFunctionProxy(this::sandboxCountHookCallback));
        setGlobalFunction("setEventCallback", new LuaFunctionProxy(this::setEventCallback));
        setGlobalField(L, "sandboxCountHookCallbackInterval", 10);

        setGlobalFunction("setStopCode", new LuaFunctionProxy((Object[] args) -> {
            var msg = Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" "));
            System.out.println("setStopCode: " + msg);
            stopCode = msg;
        }));

        setGlobalFunction("unsubMachineEvent", new LuaFunctionProxy(this::unsubMachineEvent));
        setGlobalFunction("subMachineEvent", new LuaFunctionProxy(this::subMachineEvent));
        setGlobalFunction("getMachineEvent", new LuaFunctionProxy(this::getMachineEvent));
        setGlobalFunction("waitForMachineEvent", new LuaFunctionProxy(this::waitForMachineEvent));
        setGlobalFunction("sleep", new LuaFunctionProxy((Object[] args) -> {
            if (args.length != 1) {
                throw new AcLuaException("'sleep' expects 1 timeout argument");
            }
            if (args[0] instanceof Double d) {
                try {
                    Thread.sleep((long) (d * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                throw new AcLuaException("'sleep' expects number as argument");
            }
        }));
        setGlobalField(L, "luaShell", luaShellScript);

        L.openLibrary("table");
        L.openLibrary("debug");
        //L.openLibrary("io"); // TODO make custom implementation
        L.openLibrary("math");
        //L.openLibrary("os"); // TODO make custom implementation
        L.openLibrary("string");
        //L.openLibrary("package"); // TODO make custom implementation

        timeLastHook = System.currentTimeMillis();

        var rv = L.run(luaEntryScript);
        AdvancedComputers.LOGGER.info(String.format("LVM exited with code %s", rv));

        // cleanup after shutdown
        synchronized (startStopLock) {
            isRunning = false;
            executionEnv = null;
            stdOut = null;
        }
    }


    /**
     * Called about every tick via a lua debug count hook. Used to create an artificial slowdown for Lua PCs.
     */
    public void sandboxCountHookCallback(Object[] args) {
        LockSupport.parkNanos(1_000_000 / TPS - 1000 * (System.currentTimeMillis() - timeLastHook));
        var curThread = Thread.currentThread();
        if (curThread.isInterrupted()) {
            curThread.interrupt();
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
            curThread.interrupt();
        }
        timeLastHook = System.currentTimeMillis();
    }

    public Object getStdOut() {
        synchronized (startStopLock) {
            return isRunning ? stdOut : stopCode;
        }
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
        return event == null ? null : new Object[]{event.name, event.content};
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
            Thread.currentThread().interrupt();
        }
    }
}
