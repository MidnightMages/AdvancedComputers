package dev.asdf00.mc.advcomp.types.capabilities;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class Capabilities {
    public static final Capability<DeviceCableConnectableEntity> CABLE_CONNECTABLE = CapabilityManager.get(new CapabilityToken<>() {
    });
}
