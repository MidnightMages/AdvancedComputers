package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaUserData;
import net.minecraft.world.item.ItemStack;

public interface LuaUserDataComponent extends LuaUserData {
    String getComponentType();
}
