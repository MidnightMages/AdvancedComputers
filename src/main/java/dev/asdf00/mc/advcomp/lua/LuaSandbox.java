package dev.asdf00.mc.advcomp.lua;

import party.iroiro.luajava.AbstractLua;
import party.iroiro.luajava.JFunction;
import party.iroiro.luajava.Lua;
import party.iroiro.luajava.lua54.Lua54;

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
            s = " \r" + s; // needed so idea/gradle dont remove it from the stdoutput and put it into. What a dumb 'feature'.
        System.out.println(s);
    }

    AbstractLua L;

    public LuaSandbox()
    {
        L = new Lua54();
    }

    public void SetGlobalFunction(String funcName, JFunction callback)
    {
        L.push(callback);
        L.setGlobal(funcName);
    }

    public void Run()
    {
        SetGlobalFunction("print", new LuaFunctionProxy((Object[] args)
                -> SandboxLog(Arrays.stream(args).map(a -> (a == null ? "nil" : a.toString()))
                .collect(Collectors.joining(" ")))));
        L.openLibrary("table");
        L.openLibrary("debug");
        //L.openLibrary("io"); // TODO make custom implementation
        L.openLibrary("math");
        //L.openLibrary("os"); // TODO make custom implementation
        L.openLibrary("string");
        //L.openLibrary("package"); // TODO make custom implementation

        var rv = L.run(luaEntryScript);
        if (rv != Lua.LuaError.OK)
            SandboxLog("Unexpected fatal error: "+rv.toString());
    }
}
