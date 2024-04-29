package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import party.iroiro.luajava.AbstractLua;
import party.iroiro.luajava.JFunction;
import party.iroiro.luajava.lua54.Lua54;
import party.iroiro.luajava.value.RefLuaValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static dev.asdf00.mc.advcomp.lua.LuaUtils.setGlobalField;

public class LuaSandbox {
    private static final int TPS = 20;
    private static final String luaEntryScript;

    static {
        try (var stream = LuaMain.class.getClassLoader().getResourceAsStream("assets/advancedcomputers/lua/entry.lua")) {
            Objects.requireNonNull(stream, "Error reading resource 'entry.lua'");
            luaEntryScript = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Resource 'entry.lua' not found!");
        }
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

    public LuaSandbox(ComputerBlockEntity computer, int instructionsPerSecond) {
        this.computer = computer;
        L = new Lua54();
        ipt = Math.max(instructionsPerSecond / 20, 1);
        stdOut = null;
    }

    public void sandboxLog(String s, boolean newLine, boolean error) {
        s = s.replace("\r", "");
        if (s.replace(" ", "").toLowerCase().startsWith("error:") || s.trim().toLowerCase().startsWith("warning:"))
            s = " \r" + s; // needed so idea/gradle doesnt remove it from the stdoutput and put it into stderr. What a dumb 'feature'.

        var printer = error ? System.err : System.out;

        if (newLine)
            printer.println(s);
        else
            printer.print(s);
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
        AdvancedComputers.LOGGER.info("trying to start LVM");
        setGlobalFunction("print", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")), false)));
        setGlobalFunction("printInline", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")), false, false)));
        setGlobalFunction("printErr", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")), true)));

        setGlobalFunction("sandboxCountHookCallback", new LuaFunctionProxy(this::sandboxCountHookCallback));
        setGlobalFunction("setEventCallback", new LuaFunctionProxy(this::setEventCallback));
        setGlobalField(L, "sandboxCountHookCallbackInterval", 10);

        setGlobalFunction("setStopCode", new LuaFunctionProxy((Object[] args) -> {
            var msg = Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" "));
            System.out.println("setStopCode: " + msg);
            stopCode = msg;
        }));

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
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }
        boolean isInterrupted = false;
        while (suspended) {
            LockSupport.park();
            if (Thread.currentThread().isInterrupted()) {
                // if the executor thread gets interrupted while the LVM is suspended, break suspension and continue the interrupt
                isInterrupted = true;
                break;
            }
        }
        if (isInterrupted) {
            Thread.currentThread().interrupt();
        }
        timeLastHook = System.currentTimeMillis();
    }

    public Object getStdOut() {
        synchronized (startStopLock) {
            return isRunning ? stdOut : stopCode;
        }
    }
}
