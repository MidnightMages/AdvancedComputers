package dev.asdf00.mc.advcomp.types.network;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.blocks.wan_router.WanRouterBlockEntity;
import dev.asdf00.mc.advcomp.blocks.wan_router.WanRouterBlockEntityLowTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AcNetworkHandler {
    public static final AcNetworkHandler INSTANCE = new AcNetworkHandler();
    private final HashSet<NetworkNode> nodes = new HashSet<>();

    public NetworkNode registerNewNode(boolean isWanRouter, BaseCableConnectableBlockEntity baseNetworkRouterBlockEntity) {
        var rv = new NetworkNode(isWanRouter, baseNetworkRouterBlockEntity);
        nodes.add(rv);
        return rv;
    }

    public record NetworkPath(NetworkNode[] nodePath, double length) {
    }

    public class NetworkNode {
        HashSet<NetworkNode> connectedTo = new HashSet<>();
        boolean isWanRouter;
        private final BaseCableConnectableBlockEntity blockEntity;

        private NetworkNode(boolean isWanRouter, BaseCableConnectableBlockEntity blockEntity) {
            this.isWanRouter = isWanRouter;
            this.blockEntity = blockEntity;
        }

        public BlockPos getPos() {
            return blockEntity.getBlockPos();
        }

        // non-wan distance for a*
        public double getActualDistanceTo(NetworkNode other) {
            if (this.getPos().equals(other.getPos()))
                return 0;

            return Math.sqrt(this.getPos().distSqr(other.getPos()));
        }

        // for computing gameplay delay
        public double getGameplayDistanceTo(NetworkNode other) {
            if (this.getPos().equals(other.getPos()))
                return 0;
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

        private static ArrayList<NetworkNode> aStarFromTo(NetworkNode rootSource, NetworkNode rootDest) {
            boolean findClosestWanrouterInstead = rootDest == null;

            // A STAR -- https://en.wikipedia.org/wiki/A*_search_algorithm#Pseudocode
            var start = new AStarNode(rootSource, 0);
            var goal = findClosestWanrouterInstead ? null : new AStarNode(rootDest, 0);
            var openSet = new PriorityQueue<AStarNode>();
            openSet.add(start);
            var cameFrom = new HashMap<NetworkNode, NetworkNode>();
            var gScore = new HashMap<NetworkNode, Double>(); // non-existent means +infinity
            gScore.put(start.node, 0d);

            Function<NetworkNode, Double> h = src -> findClosestWanrouterInstead ? 1 : src.getActualDistanceTo(rootDest);
            BiFunction<NetworkNode, NetworkNode, Double> d = (src, dest) -> {
                assert src.connectedTo.contains(dest);
                assert dest.connectedTo.contains(src);
                return src.getActualDistanceTo(dest);
            };

            ArrayList<NetworkNode> foundPath = null;
            while (true) {
                var current = openSet.poll();
                if (current == null) break;

                if (findClosestWanrouterInstead ? (current.node.isWanRouter) : (current.equals(goal))) {
                    var currToReconstructFrom = current.node;
                    var foundPathReverse = new ArrayList<NetworkNode>();
                    foundPathReverse.add(currToReconstructFrom);
                    while (true) {
                        var src = cameFrom.get(currToReconstructFrom);
                        if (src == null) break;

                        foundPathReverse.add(src);
                        currToReconstructFrom = src;
                    }
                    Collections.reverse(foundPathReverse);
                    foundPath = foundPathReverse;
                    break;
                }

                for (var currNeighborRaw : current.node.connectedTo) {
                    var tentativeGScore = gScore.get(current.node) + d.apply(current.node, currNeighborRaw);
                    if (tentativeGScore < gScore.getOrDefault(currNeighborRaw, Double.POSITIVE_INFINITY)) {
                        cameFrom.put(currNeighborRaw, current.node);
                        gScore.put(currNeighborRaw, tentativeGScore);
                        var currNeighbor = new AStarNode(currNeighborRaw, tentativeGScore + h.apply(currNeighborRaw)); // = fscore
                        if (!openSet.contains(currNeighbor))
                            openSet.add(currNeighbor);
                    }
                }
            }

            return foundPath;
        }

        public boolean isConnectedToWan() {
            return aStarFromTo(this, null) != null;
        }

        public NetworkPath getShortestPathTo(NetworkNode goalNetNode) {
            if (this == goalNetNode) { // shortcut if this is the same node
                return new NetworkPath(new NetworkNode[]{this, goalNetNode}, 2);
            }

            var foundPath = aStarFromTo(this, goalNetNode);
            if (foundPath == null) { // if not connected, try wan-path
                var pathA = aStarFromTo(this, null); // null means wanrouter
                if (pathA == null)
                    return null;
                var pathB = aStarFromTo(goalNetNode, null);
                if (pathB == null)
                    return null;

                Collections.reverse(pathB);
                pathA.addAll(pathB);
                foundPath = pathA;
            }

            double gameplayDistance = 0;
            for (int i = 0; i < foundPath.size() - 1; i++) {
                gameplayDistance += foundPath.get(i).getGameplayDistanceTo(foundPath.get(i + 1));
            }
            return new NetworkPath(foundPath.toArray(NetworkNode[]::new), gameplayDistance);
        }

        public void computeConnectedNodes(HashMap<Direction, CableCluster> connectedNetworks) {
            this.setConnections(connectedNetworks.values().stream()
                    .filter(x -> x.getClusterType() == AdvancedComputers.CLUSTER_TYPE_NETWORK)
                    .flatMap(x -> Arrays.stream(x.connectedEntities)
                            .filter(y -> y instanceof AcNetworkParticipant)
                            .map(y -> ((AcNetworkParticipant) y).getNetworkNode())
                    ).collect(Collectors.toCollection(ArrayList::new)));
        }

        @Override
        public String toString() {
            String name;
            if (this.blockEntity instanceof ComputerBlockEntity)
                name = "Computer";
            else if (this.blockEntity instanceof BaseNetworkRouterBlockEntity baseRouter) {
                name = (baseRouter instanceof WanRouterBlockEntity || baseRouter instanceof WanRouterBlockEntityLowTier) ? "WanRouter" : "Router";
            } else {
                name = "Other";
            }
            return "%s at (%s)".formatted(name, this.getPos());
        }

        public BaseCableConnectableBlockEntity getBlockEntity() {
            return blockEntity;
        }

        private record AStarNode(NetworkNode node, double cost) implements Comparable<AStarNode> {
            @Override
            public int compareTo(@NotNull AcNetworkHandler.NetworkNode.AStarNode o) {
                return Double.compare(cost, o.cost);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof AStarNode obj2 && this.node == obj2.node;
            }
        }
    }

}
