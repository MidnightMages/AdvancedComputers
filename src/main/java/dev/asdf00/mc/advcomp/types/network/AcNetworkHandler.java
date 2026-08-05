package dev.asdf00.mc.advcomp.types.network;

import java.util.ArrayList;
import java.util.HashSet;

public class AcNetworkHandler {
    public static final AcNetworkHandler INSTANCE = new AcNetworkHandler();
    private final HashSet<NetworkNode> nodes = new HashSet<>();
    public NetworkNode registerNewNode(boolean isWanRouter) {
        var rv = new NetworkNode(isWanRouter);
        nodes.add(rv);
        return rv;
    }

    public class NetworkNode {
        HashSet<NetworkNode> connectedTo = new HashSet<>();
        boolean isWanRouter;

        public NetworkNode(boolean isWanRouter) {
            this.isWanRouter = isWanRouter;
            nodes.add(this);
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
    }
}
