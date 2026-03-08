package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;
import dev.asdf00.mc.advcomp.utils.LuaSerializationUtils;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class ScreenBlockUD extends BaseAcBlockEntityComponent<ScreenBlockEntity> {

    public ScreenBlockUD(ScreenBlockEntity screenBlockEntity) {
        super("screen", screenBlockEntity);
    }

    private ScreenBlockUD(LuaVirtualMachine acVm, boolean isAccessible, ScreenBlockEntity screenBlockEntity) {
        super("screen", acVm, isAccessible, screenBlockEntity);
    }

    @LuaDeserializer
    public static ScreenBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(ScreenBlockEntity.class, ScreenBlockUD::new, objs, reader, postActions, additionalData);
    }
}
