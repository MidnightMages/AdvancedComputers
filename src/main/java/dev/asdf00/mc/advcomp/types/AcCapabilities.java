package dev.asdf00.mc.advcomp.types;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class AcCapabilities {
    public static final Capability<IAcCableConnectableEntity> CABLE_CONNECTABLE = CapabilityManager.get(new CapabilityToken<>() {
    });
}
