package dev.asdf00.mc.advcomp.types.network;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.management.openmbean.InvalidOpenTypeException;
import java.util.*;
import java.util.stream.Collectors;

public class AcNetworkHandler {
    public static final AcNetworkHandler INSTANCE = new AcNetworkHandler();
    private final HashSet<NetworkNode> nodes = new HashSet<>();

    public NetworkNode registerNewNode(boolean isWanRouter, BlockEntity baseNetworkRouterBlockEntity) {
        var rv = new NetworkNode(isWanRouter, baseNetworkRouterBlockEntity);
        nodes.add(rv);
        return rv;
    }

    public record NetworkPath(NetworkNode[] nodePath, double length) {
    }

    public class NetworkNode {
        HashSet<NetworkNode> connectedTo = new HashSet<>();
        boolean isWanRouter;
        private final BlockEntity blockEntity;

        public NetworkNode(boolean isWanRouter, BlockEntity blockEntity) {
            this.isWanRouter = isWanRouter;
            this.blockEntity = blockEntity;
            nodes.add(this);
        }

        public BlockPos getPos() {
            return blockEntity.getBlockPos();
        }

        // non-wan distance for a*
        public double getActualDistanceTo(NetworkNode other) {
            if (this.connectedTo.contains(other)) {
                return Math.sqrt(this.getPos().distSqr(other.getPos()));
            }
            throw new IllegalStateException("this node is not connected to destination node");
        }

        // for computing gameplay delay
        public double getGameplayDistanceTo(NetworkNode other) {
            if (this.connectedTo.contains(other)) {
                return Math.sqrt(this.getPos().distSqr(other.getPos())) * (Math.random() * 0.5 + 1);
            } else if (this.isWanRouter && other.isWanRouter) {
                var distance = Math.sqrt(this.getPos().distSqr(other.getPos()));
                var isCrossDimension = !Objects.equals(this.blockEntity.getLevel(), other.blockEntity.getLevel());
                distance += isCrossDimension ? 5000 : 1000;
                return distance * (Math.random() + 1);
            } else {
                throw new IllegalStateException("this node is not connected to destination node, and at least one node is not a wan router");
            }
        }

        public void setConnections(ArrayList<NetworkNode> newConnections) {
            for (var n : connectedTo)
                n.connectedTo.remove(this);
            connectedTo.clear();

            connectedTo.addAll(newConnections);

            for (var n : newConnections)
                n.connectedTo.add(this);

            connectedTo.remove(this); // dont connect to self
        }

        public void deleteAndDeregisterNode() {
            for (var n : connectedTo)
                n.connectedTo.remove(this);

            nodes.remove(this);
            connectedTo = null;
        }


        public NetworkPath getShortestPathTo(NetworkNode other) {
            if (this == other) { // shortcut if this is the same node
                return new NetworkPath(new NetworkNode[]{this, other}, 2);
            }
            var nodePath = new ArrayList<NetworkNode>();

            if (true) throw new InvalidOpenTypeException("not implemented"); // TODO implement A* search using euclidean distance


            double gameplayDistance = 0;
            for (int i = 0; i < nodePath.size() - 1; i++) {
                gameplayDistance += nodePath.get(i).getGameplayDistanceTo(nodePath.get(i + 1));
            }
            return new NetworkPath(nodePath.toArray(NetworkNode[]::new), gameplayDistance);
        }

        public void computeConnectedNodes(HashMap<Direction, CableCluster> connectedNetworks) {
            this.setConnections(connectedNetworks.values().stream()
                    .filter(x -> x.getClusterType() == AdvancedComputers.CLUSTER_TYPE_NETWORK)
                    .flatMap(x -> Arrays.stream(x.connectedEntities)
                            .filter(y -> y instanceof AcNetworkParticipant)
                            .map(y -> ((AcNetworkParticipant) y).getNetworkNode())
                    ).collect(Collectors.toCollection(ArrayList::new)));
        }
    }

}
