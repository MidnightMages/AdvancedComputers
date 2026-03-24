package dev.asdf00.mc.advcomp.types.cluster;

import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import net.minecraft.core.Direction;

import java.util.HashMap;

/**
 *  Represents a block entity or cable which can connect to cables and clusters.

 *  FOR MODDERS: DO NOT USE THIS INTERFACE DIRECTLY. INSTEAD USE A DERIVATIVE OF THIS INTERFACE.
 *  Each derived interface represents a network/cable-cluster kind (peripheral cable cluster / network cable cluster)
 */
public interface CableConnectableBlockOrEntity {
    boolean canBePartOfCluster(ClusterType networkType);
    boolean canConnectTo(ClusterType type, Direction side);
    boolean actsAsCable();
    HashMap<Direction, CableCluster> getNetworkList();
    void onNetworkUpdated();
}
