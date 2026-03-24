package dev.asdf00.mc.advcomp.blocks.cables.types;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// a lot of stuff taken from https://www.mcjty.eu/docs/1.20/ep5; Thank you :)
public enum ConnectionDir implements StringRepresentable {
    NONE,
    CABLE,
    BLOCK;

    public static final ConnectionDir[] VALUES = values();

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase();
    }
}
