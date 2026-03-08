package dev.asdf00.mc.advcomp.blocks.keycard_reader;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.blocks.redstone_io.RedstoneIoBlockEntity;
import dev.asdf00.mc.advcomp.blocks.redstone_io.RedstoneIoBlockUD;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockUD;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;
import dev.asdf00.mc.advcomp.utils.LuaSerializationUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.function.TriFunction;

import java.util.List;
import java.util.Map;
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
