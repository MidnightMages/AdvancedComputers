package dev.asdf00.mc.advcomp.blocks.wan_router;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import dev.asdf00.mc.advcomp.types.network.BaseNetworkRouterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class WanRouterBlockEntityLowTier extends BaseNetworkRouterBlockEntity {
    public WanRouterBlockEntityLowTier(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.WAN_ROUTER_BE_LOWTIER.get(), pPos, pBlockState, true);
    }


    @Override
    public boolean canConnectTo(ClusterType clusterType, Direction side) {
        var lvl = this.getLevel();
        assert lvl != null;
        var goodFace = lvl.getBlockState(worldPosition).getValue(WanRouterBlockLowTier.FACING);
        if (goodFace == Direction.UP)
            goodFace = Direction.DOWN;
        else if (goodFace == Direction.DOWN)
            goodFace = Direction.UP;
        return side == goodFace;
    }
}
