package dev.asdf00.mc.advcomp.types;

import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseAcDevCableConnectableEntityBlock extends BaseAcCableEntityBlock implements IAcDevCableConnectableEntity {
    public Set<CableCluster> connectedNetworks;

    public BaseAcDevCableConnectableEntityBlock(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        connectedNetworks = new HashSet<>();
    }

    @Override
    public final Set<CableCluster> getNetworkList() {
        return connectedNetworks;
    }
}
