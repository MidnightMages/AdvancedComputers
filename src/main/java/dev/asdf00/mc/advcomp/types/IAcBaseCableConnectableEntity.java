package dev.asdf00.mc.advcomp.types;

import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;

import java.util.Set;

/**
    FOR MODDERS: DO NOT USE THIS INTERFACE DIRECTLY. INSTEAD USE A DERIVATIVE OF THIS INTERFACE.
    Each derived interface represents a network/cable-cluster kind (peripheral cable cluster / network cable cluster)
 */
public interface IAcBaseCableConnectableEntity {
    Set<CableCluster> getNetworkList();
}
