package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraft.world.item.ItemStack;

public abstract class BaseAcComponent implements LuaUserDataComponent {
    private LuaObject luaIdentity;

    protected LuaVirtualMachine acVm;
    protected volatile boolean isAccessible = true;

    @LuaExposed(LuaExposed.Policy.READ)
    public final String componentType;
    private ItemStack itemStack; // TODO put this into a seperate class derived from this one, and do the same for block components, giving them a blockentity?

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

    /**
     * Is supposed to only run once during object construction. NOT during deserialization
     */
    @Override
    public void onVmInit(LuaVirtualMachine acVm, ItemStack is) {
        this.acVm = acVm;
        this.itemStack = is;
    }

    @Override
    public void makeObjectInaccessible() {
        this.isAccessible = false;
    }
}
