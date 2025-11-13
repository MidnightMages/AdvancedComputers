package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;

import java.util.List;
import java.util.Map;

public class ScreenBlockUD extends BaseAcComponent {

    private final ScreenBlockEntity screenBlockEntity;

    public ScreenBlockUD(ScreenBlockEntity screenBlockEntity) {
        super("screen");
        this.screenBlockEntity = screenBlockEntity;
    }

    // TODO implement lua api

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static ScreenBlockUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
