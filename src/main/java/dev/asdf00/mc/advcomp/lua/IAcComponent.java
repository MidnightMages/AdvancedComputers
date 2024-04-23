package dev.asdf00.mc.advcomp.lua;

public interface IAcComponent {
    String getComponentName();
    void onRegister(LuaComponentRegistry.LuaFunctionGroup group);
    void onDeregister(LuaComponentRegistry.LuaFunctionGroup group);
}
