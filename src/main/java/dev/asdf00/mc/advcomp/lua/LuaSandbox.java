package dev.asdf00.mc.advcomp.lua;

import party.iroiro.luajava.AbstractLua;
import party.iroiro.luajava.JFunction;
import party.iroiro.luajava.Lua;
import party.iroiro.luajava.lua54.Lua54;
import party.iroiro.luajava.value.LuaValue;
import party.iroiro.luajava.value.RefLuaValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class LuaSandbox
{
    static String luaEntryScript = null;

    static
    {
        var rl = LuaMain.class.getClassLoader().getResourceAsStream("assets/advancedcomputers/lua/entry.lua");
        assert rl != null;
        luaEntryScript = new BufferedReader(new InputStreamReader(rl, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    }

    public void SandboxLog(String s)
    {
        if (s.replace(" ","").toLowerCase().startsWith("error:") || s.trim().toLowerCase().startsWith("warning:"))
            s = " \r" + s; // needed so idea/gradle doesnt remove it from the stdoutput and put it into stderr. What a dumb 'feature'.
        System.out.println(s);
    }

    AbstractLua L;
    RefLuaValue L_eventCallbackRef;

    public LuaSandbox()
    {
        L = new Lua54();
    }

    public void SetGlobalFunction(String funcName, JFunction callback)
    {
        L.push(callback);
        L.setGlobal(funcName);
    }

    public void PushEventIntoSandbox(String name, Object[] args)
    {
//        L.newThread(); // shouldnt be needed i think??
        L_eventCallbackRef.push();
        L.push(name);
        LuaUtils.PushArgs(L, args);
        var status = L.resume(args.length + 1);
        SandboxLog("CO status: "+status);
    }

    private void SetEventCallback(Object[] args)
    {
        L_eventCallbackRef = (RefLuaValue)args[0];
    }

    public void Run()
    {
        SetGlobalFunction("print", new LuaFunctionProxy((Object[] args)
                -> SandboxLog(Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString()))
                .collect(Collectors.joining(" ")))));

        SetGlobalFunction("setEventCallback", new LuaFunctionProxy(this::SetEventCallback));

        L.openLibrary("table");
        L.openLibrary("debug");
        //L.openLibrary("io"); // TODO make custom implementation
        L.openLibrary("math");
        //L.openLibrary("os"); // TODO make custom implementation
        L.openLibrary("string");
        //L.openLibrary("package"); // TODO make custom implementation

        var rv = L.run(luaEntryScript);

        PushEventIntoSandbox("testEvent", new Object[]{1,2,3});
        if (rv != Lua.LuaError.OK)
            SandboxLog("Unexpected fatal error: "+rv.toString());
    }
}
