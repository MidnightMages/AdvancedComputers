package dev.asdf00.mc.advcomp.types.cluster;

import net.minecraft.core.Direction;

public interface IAcClusterHostEntity extends IAcBaseCableConnectableBlockEntity {
    boolean isNetworkValid(Direction dir);
    boolean isHostForNetwork(Direction dir, AcClusterType type);
}
