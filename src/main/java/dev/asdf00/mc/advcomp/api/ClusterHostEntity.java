package dev.asdf00.mc.advcomp.api;

import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

public interface ClusterHostEntity extends IForgeBlockEntity {

    boolean isHostForNetwork(ClusterType type);
}
