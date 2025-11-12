package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;

public abstract class BaseAcComponent implements LuaUserDataComponent {
    private final String componentTypeString;
    protected LuaVirtualMachine acVm;
    private boolean isAccessible = true;

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty componentType;

    public BaseAcComponent(String componentType) {
        this.componentTypeString = componentType;
        this.componentType = LuaProperty.ofString(() -> componentType, null);
    }

    @Override
    public boolean luaFieldGuard(LuaObject key, LuaObject value) {
        return isAccessible;
    }

    @Override
    public boolean luaCallGuard(String name, LuaObject[] arguments) {
        return isAccessible;
    }

    @Override
    public String getComponentType() {
        return componentTypeString;
    }

    @Override
    public void onVmInit(LuaVirtualMachine acVm) {
        this.acVm = acVm;
    }

    @Override
    public void makeObjectInaccessible() {
        this.isAccessible = false;
    }
}
