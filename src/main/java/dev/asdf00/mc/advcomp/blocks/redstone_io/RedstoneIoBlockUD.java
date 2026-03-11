package dev.asdf00.mc.advcomp.blocks.redstone_io;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponent;
import net.minecraft.core.Direction;

import java.util.Queue;

public class RedstoneIoBlockUD extends BaseAcBlockEntityComponent<RedstoneIoBlockEntity> {

    public RedstoneIoBlockUD(RedstoneIoBlockEntity redstoneIoBlockEntity) {
        super("redstone", redstoneIoBlockEntity);
    }

    private RedstoneIoBlockUD(LuaVirtualMachine acVm, boolean isAccessible, RedstoneIoBlockEntity redstoneIoBlockEntity) {
        super("screen", acVm, isAccessible, redstoneIoBlockEntity);
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

        blockEntity.setSignal(side, (int) level);
    }

    @LuaCallable
    public int getInput(int side) {
        var direction = Direction.from3DDataValue(side);
        return blockEntity.getLevel().getSignal(blockEntity.getBlockPos().relative(direction), direction);
    }

    @LuaDeserializer
    public static RedstoneIoBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(RedstoneIoBlockEntity.class, RedstoneIoBlockUD::new, objs, reader, postActions, additionalData);
    }
}
