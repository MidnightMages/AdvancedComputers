package dev.asdf00.mc.advcomp.types.network;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class BaseNetworkRouterBlockEntity extends BaseCableConnectableBlockEntity implements AcNetworkParticipant {
    private AcNetworkHandler.NetworkNode netNode;

    public BaseNetworkRouterBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, boolean isWanRouter) {
        super(pType, pPos, pBlockState, Collections.singletonList(AdvancedComputers.CLUSTER_TYPE_NETWORK));
        netNode = AcNetworkHandler.INSTANCE.registerNewNode(isWanRouter);
    }

    @Override
    public AcNetworkHandler.NetworkNode getNetworkNode() {
        return netNode;
    }

    @Override
    public void onNetworkUpdated() {
        super.onNetworkUpdated();
        netNode.setConnections(this.connectedNetworks.values().stream()
                .filter(x -> x.getClusterType() == AdvancedComputers.CLUSTER_TYPE_NETWORK)
                .flatMap(x -> Arrays.stream(x.connectedEntities)
                        .filter(y -> y instanceof AcNetworkParticipant)
                        .map(y -> ((AcNetworkParticipant) y).getNetworkNode())
                ).collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public boolean actsAsCable(ClusterType clusterType) {
        return false;
    }

    public void onDestroy() {
        netNode.deleteAndDeregisterNode();
        netNode = null;
    }
}
