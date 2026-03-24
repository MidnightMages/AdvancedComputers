package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;

import java.util.Queue;

public final class ScreenBlockUD extends BaseAcBlockEntityComponentUD<ScreenBlockEntity> {

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
