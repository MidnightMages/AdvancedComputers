package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LuaHelpers {
    public static BlockPos getNeighborBlockPosFromSideArgument(BlockEntity ourBlockEntity, int sideArgument, int sideArgumentIndex) {
        return getNeighborBlockPosFromSideArgument(ourBlockEntity.getBlockPos(), sideArgument, sideArgumentIndex);
    }

    private static BlockPos getNeighborBlockPosFromSideArgument(BlockPos ourBlockPos, int sideArgument, int sideArgumentIndex) {

        if (sideArgument < 0 || sideArgument > 5)
            throw new LuaJavaError("sides argument (%s) is out of range: %s. Expected integer in range [0,5]"
                    .formatted(sideArgumentIndex + 1, sideArgument));

        var direction = Direction.from3DDataValue(sideArgument).getOpposite();
        return ourBlockPos.offset(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
