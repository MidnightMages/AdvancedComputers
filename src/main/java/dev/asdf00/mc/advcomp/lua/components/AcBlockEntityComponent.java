package dev.asdf00.mc.advcomp.lua.components;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

public interface AcBlockEntityComponent extends IForgeBlockEntity { // just extends IForgeBlockEntity to force implementers to only use this on a blockentity
    LuaUserDataComponent CreateUserdata();
}
