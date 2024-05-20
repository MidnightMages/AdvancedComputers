package dev.asdf00.mc.advcomp.types;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class DualLayerItemColorHandler implements ItemColor {
    @Override
    public int getColor(@NotNull ItemStack stack, int tintIndex) {
        if (tintIndex != 1) return 0xFFFFFFFF;
        var compound = stack.getTag();
        if (compound == null) return 0xFFFFFFFF;
        return compound.getInt("color");
    }
}