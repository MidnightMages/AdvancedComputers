package dev.asdf00.mc.advcomp.types.cluster;

import net.minecraft.core.Direction;

/**
 *  Represents a block entity or cable which can connect to cables and clusters.

 *  FOR MODDERS: DO NOT USE THIS INTERFACE DIRECTLY. INSTEAD USE A DERIVATIVE OF THIS INTERFACE.
 *  Each derived interface represents a network/cable-cluster kind (peripheral cable cluster / network cable cluster)
 */
public interface IAcBaseCableConnectableEntity {
    boolean canBePartOfCluster(AcClusterType networkType);
    boolean canConnectTo(IAcBaseCableConnectableEntity entity, Direction side);
    boolean actsAsCable();
}
