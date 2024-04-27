package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import party.iroiro.luajava.AbstractLua;
import party.iroiro.luajava.JFunction;
import party.iroiro.luajava.Lua;
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

    private final AbstractLua L;
    private final int ipt;
    private RefLuaValue L_eventCallbackRef;
    private long timeLastHook = 0;

    private final Object startStopLock = new Object();
    private volatile boolean suspended;
    private volatile boolean isRunning;
    private volatile Thread executionEnv;

    public LuaSandbox(int instructionsPerSecond) {
        L = new Lua54();
        ipt = Math.max(instructionsPerSecond / 20, 1);
    }

    private static final boolean OUTTOLOGGER = true;

    public void sandboxLog(String s, boolean newLine) {
        if (s.replace(" ", "").toLowerCase().startsWith("error:") || s.trim().toLowerCase().startsWith("warning:"))
            s = " \r" + s; // needed so idea/gradle doesnt remove it from the stdoutput and put it into stderr. What a dumb 'feature'.

        if (OUTTOLOGGER) {
            AdvancedComputers.LOGGER.info(s);
        }

        if (newLine)
            System.out.println(s);
        else
            System.out.print(s);
    }

    public void sandboxLog(String s) {
        sandboxLog(s, true);
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
        sandboxLog("CO status: " + status);
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
            executionEnv = new Thread(this::runLua);
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

    public void tryKill() {
        synchronized (startStopLock) {
            if (isRunning) {
                executionEnv.interrupt();
                if (suspended) {
                    resume();
                }
            }
        }
    }

    public void toggleOnOff() {
        synchronized (startStopLock) {
            if (getState() == 0) {
                start();
            } else {
                tryKill();
            }
        }
    }

    public void runLua() {
        synchronized (startStopLock) {
            isRunning = true;
        }
        AdvancedComputers.LOGGER.info("trying to start LVM");
        setGlobalFunction("print", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")))));
        setGlobalFunction("printInline", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")), false)));

        if (true) {
            var rv = L.run("print(\"TEST INSIDE LUA\")");
            AdvancedComputers.LOGGER.info(String.format("lua exited with %s", rv));
            synchronized (startStopLock) {
                isRunning = false;
                executionEnv = null;
            }
            return;
        }

        setGlobalFunction("setEventCallback", new LuaFunctionProxy(this::setEventCallback));
        setGlobalField(L, "sandboxCountHookCallbackInterval", 10);

        L.openLibrary("table");
        L.openLibrary("debug");
        //L.openLibrary("io"); // TODO make custom implementation
        L.openLibrary("math");
        //L.openLibrary("os"); // TODO make custom implementation
        L.openLibrary("string");
        //L.openLibrary("package"); // TODO make custom implementation

        timeLastHook = System.currentTimeMillis();
        var rv = L.run(luaEntryScript);

        // cleanup after shutdown
        synchronized (startStopLock) {
            isRunning = false;
            executionEnv = null;
        }

        pushEventIntoSandbox("testEvent", new Object[]{1, 2, 3});
        if (rv != Lua.LuaError.OK)
            sandboxLog("Unexpected fatal error: " + rv.toString());

    }


    /**
     * Called about every tick via a lua debug count hook. Used to create an artificial slowdown for Lua PCs.
     */
    public void sandboxCountHookCallback() throws InterruptedException {
        LockSupport.parkNanos(1_000_000 / TPS - 1000 * (System.currentTimeMillis() - timeLastHook));
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }
        while (suspended) {
            LockSupport.park();
        }
        timeLastHook = System.currentTimeMillis();
    }
}
