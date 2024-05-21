package dev.asdf00.mc.advcomp.blocks.cables;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// a lot of stuff taken from https://www.mcjty.eu/docs/1.20/ep5; Thank you :)
public enum ConnectorType  implements StringRepresentable {
    NONE,
    CABLE,
    BLOCK;

    public static final ConnectorType[] VALUES = values();

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase();
    }
}
