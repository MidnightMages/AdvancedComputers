package dev.asdf00.mc.advcomp.blocks.keycard_reader;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponent;

import java.util.Queue;

public class KeyCardReaderBlockEntityUD extends BaseAcBlockEntityComponent<KeyCardReaderBlockEntity> {

    public KeyCardReaderBlockEntityUD(KeyCardReaderBlockEntity keyCardReaderBlockEntity) {
        super("keycardReader", keyCardReaderBlockEntity);
    }

    private KeyCardReaderBlockEntityUD(LuaVirtualMachine luaVirtualMachine, boolean isAccessible, KeyCardReaderBlockEntity blockEntity) {
        super("keycardReader", luaVirtualMachine, isAccessible, blockEntity);
    }

    @LuaCallable
    public String test(LuaObject[] args) {
        return "keycard reader works!";
    }

    @LuaDeserializer
    public static KeyCardReaderBlockEntityUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(KeyCardReaderBlockEntity.class, KeyCardReaderBlockEntityUD::new, objs, reader, postActions, additionalData);
    }
}
