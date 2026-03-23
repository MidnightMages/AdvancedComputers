package dev.asdf00.mc.advcomp.lua.components;

import net.neoforged.common.extensions.IForgeBlockEntity;

public interface AcBlockEntityComponent extends IForgeBlockEntity { // just extends IForgeBlockEntity to force implementers to only use this on a blockentity
    LuaUserDataComponent CreateUserdata();
}
