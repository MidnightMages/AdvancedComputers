package dev.asdf00.mc.advcomp.lua;

public interface IAcComponent {
    String getComponentName();
    void onRegister();
    void onDeregister();
}
