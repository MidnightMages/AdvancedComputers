package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import net.minecraft.world.item.ItemStack;

public interface LuaUserDataComponent extends LuaUserData {
    String getComponentType();

    /**
     * Purely for passing data to the userdata object upon vm creation.
     * @param acVm A reference to the parent {@link LuaVirtualMachine} object.
     */
    void onVmInit(LuaVirtualMachine acVm);

    /**
     * If this is called, the lua object shall no longer be accessible in any way. This is generally called when the
     * item / block that provides this userdata is removed or destroyed and therefore the component no longer exists.
     */
    void makeObjectInaccessible();
}
