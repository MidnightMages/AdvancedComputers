package dev.asdf00.mc.advcomp.blocks.net_router;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class NetRouterBlockEntity extends BaseCableConnectableBlockEntity {
    public NetRouterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.NET_ROUTER_BE.get(), pPos, pBlockState, Collections.singletonList(AdvancedComputers.CLUSTER_TYPE_NETWORK));
    }
}
