package dev.asdf00.mc.advcomptest;

import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;

public class LuaPrototypingMain {
    private static void CWriteLine(String s) {
        System.out.println(s);
    }

    public static void main(String[] args) {
        CWriteLine("Creating sandbox...");
        var sandbox = new LuaVirtualMachine(null);
        try {
            // sandbox.start();
        } catch (Exception ex) {
            CWriteLine(String.format("Caught: %s", ex));
        }

        CWriteLine("Finished execution!");
    }
}
