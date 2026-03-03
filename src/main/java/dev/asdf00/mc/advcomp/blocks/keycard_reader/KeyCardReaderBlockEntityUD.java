package dev.asdf00.mc.advcomp.blocks.keycard_reader;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.blocks.redstone_io.RedstoneIoBlockEntity;
import dev.asdf00.mc.advcomp.blocks.redstone_io.RedstoneIoBlockUD;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;
import dev.asdf00.mc.advcomp.utils.LuaSerializationUtils;

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
        return LuaSerializationUtils.appendBlockEntity(new ByteArrayBuilder(Integer.BYTES * 3), keyCardReaderBlockEntity).toArray();
    }

    @LuaDeserializer
    public static KeyCardReaderBlockEntityUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var be = LuaSerializationUtils.<KeyCardReaderBlockEntity>readBlockEntity(reader, ((LuaVirtualMachine) additionalData).cbe.getLevel());
        if (be == null) {
            throw new IllegalStateException("we did not find some KeyCardReaderBlockEntity");
        }
        return new KeyCardReaderBlockEntityUD(be);
    }
}
