package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;

import java.util.List;
import java.util.Map;

public abstract class BaseAcComponent implements LuaUserDataComponent {
    private LuaObject luaIdentity;

    protected LuaVirtualMachine acVm;
    protected volatile boolean isAccessible = true;

    @LuaExposed(LuaExposed.Policy.READ)
    public final String componentType;

    public BaseAcComponent(String componentType) {
        this(componentType, null, true);
    }

    protected BaseAcComponent(String componentType, LuaVirtualMachine acVm, boolean isAccessible) {
        this.componentType = componentType;
        this.acVm = acVm;
        this.isAccessible = isAccessible;
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
    public final LuaObject getSelfAsLuaObject() {
        return luaIdentity;
    }

    @Override
    public final void setSelfAsLuaObject(LuaObject self) {
        luaIdentity = self;
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
