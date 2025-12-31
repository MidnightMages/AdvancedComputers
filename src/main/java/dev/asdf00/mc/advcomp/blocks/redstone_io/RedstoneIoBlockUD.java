package dev.asdf00.mc.advcomp.blocks.redstone_io;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;

public class RedstoneIoBlockUD extends BaseAcComponent {
    private final RedstoneIoBlockEntity redstoneIoBlockEntity;

    public RedstoneIoBlockUD(RedstoneIoBlockEntity redstoneIoBlockEntity) {
        super("redstone");
        this.redstoneIoBlockEntity = redstoneIoBlockEntity;
    }

    @LuaCallable
    public void setOutput(int side, LuaObject levelArg) {
        if (side < 0 || side > 5)
            throw new LuaJavaError("sides argument (#1) is out of range: %s. Expected integer in range [0,5]".formatted(side));

        if (!levelArg.isBoolean() && !levelArg.hasLongRepr())
            throw new LuaJavaError("level argument (2#) must either be bool or integer but was %s.".formatted(levelArg.getTypeAsString()));

        var level = levelArg.isBoolean() ? (levelArg.getBool() ? 15 : 0) : levelArg.asLong();
        if (level < 0 || level > 15)
            throw new LuaJavaError("level argument (#2) is out of range: %s. Expected integer in range [0, 15]".formatted(level));

        redstoneIoBlockEntity.setSignal(side, (int)level);
    }

    @LuaCallable
    public int getInput(int side) {
        return redstoneIoBlockEntity.getLevel().getSignal(redstoneIoBlockEntity.getBlockPos(), Direction.from3DDataValue(side).getOpposite());
    }

    @LuaDeserializer
    public static RedstoneIoBlockUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        return new byte[0];
    }
}
