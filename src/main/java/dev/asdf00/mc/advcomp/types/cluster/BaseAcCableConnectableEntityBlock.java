package dev.asdf00.mc.advcomp.types.cluster;

import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.types.IAcDevCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;

public abstract class BaseAcCableConnectableEntityBlock extends BaseAcCableEntityBlock implements IAcBaseCableConnectableBlockEntity, IAcDevCableConnectableEntity {
    private final List<AcClusterType> supportedClusterTypes;
    public HashMap<Direction, CableCluster> connectedNetworks;

    public BaseAcCableConnectableEntityBlock(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, List<AcClusterType> supportedClusterTypes) {
        super(pType, pPos, pBlockState);
        this.supportedClusterTypes = supportedClusterTypes;
        connectedNetworks = new HashMap<>();
    }

    @Override
    public final HashMap<Direction, CableCluster> getNetworkList() {
        return connectedNetworks;
    }

    @Override
    public boolean canBePartOfCluster(AcClusterType networkType) {
        return supportedClusterTypes.contains(networkType);
    }

    @Override
    public boolean canConnectTo(IAcBaseCableConnectableEntity entity, Direction side) {
        for (var t : supportedClusterTypes) {
            if (entity.canBePartOfCluster(t))
                return true;
        }
        return false;
    }

    @Override
    public boolean actsAsCable() {
        return true;
    }

    @Override
    public void onNetworkUpdated(Direction dir) {
    }
}
