package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;
import dev.asdf00.mc.advcomp.utils.LuaSerializationUtils;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class ScreenBlockUD extends BaseAcComponent {

    public final ScreenBlockEntity screenBlockEntity;

    public ScreenBlockUD(ScreenBlockEntity screenBlockEntity) {
        super("screen");
        this.screenBlockEntity = screenBlockEntity;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return LuaSerializationUtils.appendBlockEntity(new ByteArrayBuilder(Integer.BYTES * 3), screenBlockEntity).toArray();
    }

    @LuaDeserializer
    public static ScreenBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var be = LuaSerializationUtils.<ScreenBlockEntity>readBlockEntity(reader, ((LuaVirtualMachine) additionalData).cbe.getLevel());
        if (be == null) {
            throw new IllegalStateException("we did not find some ScreenBlockEntity");
        }
        return new ScreenBlockUD(be);
    }
}
