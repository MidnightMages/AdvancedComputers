package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;

import java.util.Objects;

public abstract class BaseAcComponent implements LuaUserDataComponent {
    private String componentType;
    protected LuaVirtualMachine acVm;
    private boolean isAccessible = true;

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty id = LuaProperty.ofString(() -> Objects.requireNonNull(componentType), null);

    public BaseAcComponent(String componentType) {
        this.componentType = componentType;
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
        return componentType;
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
