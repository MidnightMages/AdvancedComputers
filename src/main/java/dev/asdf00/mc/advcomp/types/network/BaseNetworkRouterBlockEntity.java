package dev.asdf00.mc.advcomp.types.network;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.CableClusterHandler;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class BaseNetworkRouterBlockEntity extends BaseCableConnectableBlockEntity implements AcNetworkParticipant {
    private AcNetworkHandler.NetworkNode netNode;

    public BaseNetworkRouterBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, boolean isWanRouter) {
        super(pType, pPos, pBlockState, Collections.singletonList(AdvancedComputers.CLUSTER_TYPE_NETWORK));
        netNode = AcNetworkHandler.INSTANCE.registerNewNode(isWanRouter, this);
    }

    @Override
    public AcNetworkHandler.NetworkNode getNetworkNode() {
        return netNode;
    }

    @Override
    public void onNetworkUpdated() {
        super.onNetworkUpdated();
        netNode.computeConnectedNodes(this.connectedNetworks);
    }

    @Override
    public boolean actsAsCable(ClusterType clusterType) {
        return false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        netNode.deleteAndDeregisterNode();
        netNode = null;
        assert level != null;
        CableClusterHandler.markBlockPosForUpdate(level, this.getBlockPos());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        assert level != null;
        if (!level.isClientSide())
            CableClusterHandler.markBlockPosForUpdate(level, this.getBlockPos());
    }
}
