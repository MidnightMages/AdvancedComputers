package dev.asdf00.mc.advcomp.api;

import dev.asdf00.mc.advcomp.types.cluster.CableConnectableBlockOrEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

public interface ClusterHostEntity extends IForgeBlockEntity, CableConnectableBlockOrEntity {

    boolean isHostForNetwork(ClusterType type);
}
