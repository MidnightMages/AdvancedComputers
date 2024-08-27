package dev.asdf00.mc.advcomp.types;

import dev.asdf00.mc.advcomp.blocks.cables.BaseCableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import net.minecraft.core.Direction;

import java.util.Set;

/**
    FOR MODDERS: DO NOT USE THIS INTERFACE DIRECTLY. INSTEAD USE A DERIVATIVE OF THIS INTERFACE.
    Each derived interface represents a network/cable-cluster kind (peripheral cable cluster / network cable cluster)
 */
public interface IAcBaseCableConnectableEntity {
    Set<CableCluster> getNetworkList();
    /** more speific --> only applies to blocks */
    boolean canConnectTo(IAcBaseCableConnectableEntity entity, Direction side);
    /** less specific --> applies to blocks and cables */
    boolean canConnectTo(BaseAcCableEntityBlock entity, Direction side);
}
