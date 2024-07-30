package dev.asdf00.mc.advcomp.types;

import dev.asdf00.mc.advcomp.blocks.cables.CableNetwork;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface IAcCableConnectable {

    void setNetwork(CableNetwork cableNetwork);
}
