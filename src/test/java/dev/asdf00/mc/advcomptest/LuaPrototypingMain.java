package dev.asdf00.mc.advcomptest;

import dev.asdf00.mc.advcomp.lua.LuaSandbox;

public class LuaPrototypingMain
{
    private static void CWriteLine(String s) {
        System.out.println(s);
    }

    public static void main(String[] args)
    {
        CWriteLine("Creating sandbox...");
        var sandbox = new LuaSandbox(10000);
        try{
            sandbox.runLua();
        }
        catch (Exception ex)
        {
            CWriteLine(String.format("Caught: %s", ex));
        }

        CWriteLine("Finished execution!");
    }
}
