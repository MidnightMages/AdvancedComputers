package dev.asdf00.mc.advcomp.blocks.redstone_io;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockUD;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;
import dev.asdf00.mc.advcomp.utils.LuaSerializationUtils;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;
import java.util.Queue;

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

        redstoneIoBlockEntity.setSignal(side, (int) level);
    }

    @LuaCallable
    public int getInput(int side) {
        var direction = Direction.from3DDataValue(side);
        return redstoneIoBlockEntity.getLevel().getSignal(redstoneIoBlockEntity.getBlockPos().relative(direction), direction);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return LuaSerializationUtils.appendBlockEntity(new ByteArrayBuilder(Integer.BYTES * 3), redstoneIoBlockEntity).toArray();
    }

    @LuaDeserializer
    public static RedstoneIoBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var be = LuaSerializationUtils.<RedstoneIoBlockEntity>readBlockEntity(reader, ((LuaVirtualMachine) additionalData).cbe.getLevel());
        if (be == null) {
            throw new IllegalStateException("we did not find some RedstoneIoBlockEntity");
        }
        return new RedstoneIoBlockUD(be);
    }
}
