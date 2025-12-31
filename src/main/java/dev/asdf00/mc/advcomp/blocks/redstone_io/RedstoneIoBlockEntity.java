package dev.asdf00.mc.advcomp.blocks.redstone_io;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.components.AcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.types.RuntimeAssert;
import dev.asdf00.mc.advcomp.types.cluster.BaseAcCableConnectableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Collections;

public class RedstoneIoBlockEntity extends BaseAcCableConnectableBlockEntity implements AcBlockEntityComponent {
    // TODO save this array when saving the world
    private final int[] outputStrengths = new int[]{0, 0, 0, 0, 0, 0}; // DOWN UP NORTH SOUTH WEST EAST

    public RedstoneIoBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.REDSTONE_IO_BE.get(), pPos, pBlockState, Collections.singletonList(AdvancedComputers.CLUSTER_TYPE_DEVICE));
    }

    @Override
    public LuaUserDataComponent CreateUserdata() {
        return new RedstoneIoBlockUD(this);
    }

    public int getSignal(Direction pDirection) {
        return outputStrengths[pDirection.getOpposite().get3DDataValue()];
    }

    public void setSignal(int direction, int value) {
        RuntimeAssert.RuntimeAssert(value >= 0 && value <= 15, "Redstone output value %s was out of range".formatted(value));
        outputStrengths[direction] = value;
        ServerLifecycleHooks.getCurrentServer().executeBlocking(() -> {
            updateNeighbours(Direction.from3DDataValue(direction).getOpposite());
        });
    }

    private void updateNeighbours(Direction faceToUpdate) {
        var blk = AdvancedComputers.REDSTONE_IO_BLOCK.block().get();
        assert level != null;
        level.updateNeighborsAt(getBlockPos(), blk);
        //level.updateNeighborsAt(getBlockPos().relative(faceToUpdate), blk);
    }
}
