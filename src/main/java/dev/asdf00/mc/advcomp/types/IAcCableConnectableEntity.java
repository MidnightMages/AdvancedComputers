package dev.asdf00.mc.advcomp.types;

import dev.asdf00.mc.advcomp.blocks.cables.CableNetwork;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.Set;

@AutoRegisterCapability
public interface IAcCableConnectableEntity {
    Set<CableNetwork> getNetworkList();
}
