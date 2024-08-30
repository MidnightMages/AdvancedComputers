package dev.asdf00.mc.advcomp.api;

import dev.asdf00.mc.advcomp.exceptions.ACError;
import dev.asdf00.mc.advcomp.types.cluster.AcClusterType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IAcClusterHostEntity extends IAcBaseCableConnectableBlockEntity {
    boolean isNetworkValid(Direction dir);

    boolean isHostForNetwork(Direction dir, AcClusterType type);

    Direction getWorldOrientation();

    default BlockEntity asBlockEntity() {
        if (this instanceof BlockEntity be) {
            return be;
        }
        throw ACError.shouldNotReach("%s implements IAcClusterHostEntity even though it is not a BlockEntity", this.getClass().getName());
    }
}
