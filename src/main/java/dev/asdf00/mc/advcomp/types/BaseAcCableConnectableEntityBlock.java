package dev.asdf00.mc.advcomp.types;

import dev.asdf00.mc.advcomp.CableNetworkHandler;
import dev.asdf00.mc.advcomp.blocks.cables.CableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseAcCableConnectableEntityBlock extends BaseAcCableEntityBlock implements IAcCableConnectableEntity {
    public Set<CableNetwork> connectedNetworks;

    public BaseAcCableConnectableEntityBlock(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        connectedNetworks = new HashSet<>();
    }

    @Override
    public final Set<CableNetwork> getNetworkList() {
        return connectedNetworks;
    }
}
