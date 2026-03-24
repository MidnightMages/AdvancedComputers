package dev.asdf00.mc.advcomp.blocks.wan_router;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class WanRouterBlockEntityLowTier extends BaseCableConnectableBlockEntity {
    public WanRouterBlockEntityLowTier(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.WAN_ROUTER_BE_LOWTIER.get(), pPos, pBlockState, Collections.singletonList(AdvancedComputers.CLUSTER_TYPE_NETWORK));
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
