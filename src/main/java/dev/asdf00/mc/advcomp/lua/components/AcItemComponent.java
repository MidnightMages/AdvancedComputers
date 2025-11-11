package dev.asdf00.mc.advcomp.lua.components;

import net.minecraft.world.item.ItemStack;

public interface AcItemComponent {
    LuaUserDataComponent CreateUserdata(ItemStack stack);
}
