package dev.asdf00.mc.advcomp.types.cluster;

import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Set;

/**
 * Represents a block entity which can be connected to clusters (cable networks) and needs to be aware of other devices on this network.
 */
public interface IAcBaseCableConnectableBlockEntity extends IAcBaseCableConnectableEntity{
    HashMap<Direction, CableCluster> getNetworkList();
    void onNetworkUpdated(Direction dir);
}
