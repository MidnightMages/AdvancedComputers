package dev.asdf00.mc.advcomp.types;

import net.neoforged.common.capabilities.Capability;
import net.neoforged.common.capabilities.CapabilityManager;
import net.neoforged.common.capabilities.CapabilityToken;

public class AcCapabilities {
    public static final Capability<AcDevCableConnectableEntity> CABLE_CONNECTABLE = CapabilityManager.get(new CapabilityToken<>() {
    });
}
