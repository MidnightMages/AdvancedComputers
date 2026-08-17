package dev.asdf00.mc.advcomp.blocks.wan_router;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.types.network.BaseNetworkRouterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class WanRouterBlockEntity extends BaseNetworkRouterBlockEntity {

    public WanRouterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.WAN_ROUTER_BE.get(), pPos, pBlockState, true);
    }
}
