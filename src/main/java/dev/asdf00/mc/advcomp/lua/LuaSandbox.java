package dev.asdf00.mc.advcomp.lua;

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
        }
        catch (IOException e) {
            throw new IllegalStateException("Resource 'entry.lua' not found!");
        }
    }

    private final AbstractLua L;
    private final int ipt;
    private Thread executionEnv;
    private RefLuaValue L_eventCallbackRef;
    private long timeLastHook = 0;
    private volatile boolean suspended;

    public LuaSandbox(int instructionsPerSecond) {
        L = new Lua54();
        ipt = Math.max(instructionsPerSecond / 20, 1);
    }

    public void sandboxLog(String s, boolean newLine) {
        if (s.replace(" ", "").toLowerCase().startsWith("error:") || s.trim().toLowerCase().startsWith("warning:"))
            s = " \r" + s; // needed so idea/gradle doesnt remove it from the stdoutput and put it into stderr. What a dumb 'feature'.

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

    public void runLua() {
        setGlobalFunction("print", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")))));
        setGlobalFunction("printInline", new LuaFunctionProxy((Object[] args) -> sandboxLog(
                Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString())).collect(Collectors.joining(" ")), false)));

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
            throw new InterruptedException();
        }
        while (suspended) {
            LockSupport.park();
        }
        timeLastHook = System.currentTimeMillis();
    }
}
