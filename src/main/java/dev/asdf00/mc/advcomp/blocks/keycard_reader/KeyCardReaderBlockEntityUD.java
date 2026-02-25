package dev.asdf00.mc.advcomp.blocks.keycard_reader;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class KeyCardReaderBlockEntityUD extends BaseAcComponent {
    private final KeyCardReaderBlockEntity keyCardReaderBlockEntity;

    public KeyCardReaderBlockEntityUD(KeyCardReaderBlockEntity keyCardReaderBlockEntity) {
        super("keycardReader");
        this.keyCardReaderBlockEntity = keyCardReaderBlockEntity;
    }

    @LuaCallable
    public String test(LuaObject[] args) {
        return "keycard reader works!";
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static KeyCardReaderBlockEntityUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        // TODO actually provide serializaion
        return null;
    }
}
