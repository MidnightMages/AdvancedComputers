package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.mc.advcomp.types.capabilities.DeviceCableConnectableEntity;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

// extends IForgeBlockEntity to force implementers to only use this on a blockentity
public interface AcBlockEntityComponent extends IForgeBlockEntity, DeviceCableConnectableEntity {
    LuaUserDataComponent createUserdata();
}
