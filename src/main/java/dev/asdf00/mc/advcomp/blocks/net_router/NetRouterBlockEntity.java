package dev.asdf00.mc.advcomp.blocks.net_router;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.types.network.BaseNetworkRouterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetRouterBlockEntity extends BaseNetworkRouterBlockEntity {

    public NetRouterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.NET_ROUTER_BE.get(), pPos, pBlockState, false);
    }
}
