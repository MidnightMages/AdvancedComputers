package dev.asdf00.mc.advcomp.utils;

import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LuaSerializationUtils {
    public static ByteArrayBuilder appendBlockEntity(ByteArrayBuilder bb, BlockEntity be) {
        // skip dimension because it is always in the same as the computer
        // append pos
        var pos = be.getBlockPos();
        bb.append(pos.getX()).append(pos.getY()).append(pos.getZ());
        return bb;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> T readBlockEntity(ByteArrayReader br, LevelAccessor level) {
        int x = br.readInt(), y = br.readInt(), z = br.readInt();
        return (T) level.getBlockEntity(new BlockPos(x, y, z));
    }
}
