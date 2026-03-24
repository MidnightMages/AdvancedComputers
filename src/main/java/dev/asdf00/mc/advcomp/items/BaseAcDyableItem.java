package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.types.DyableItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class BaseAcDyableItem extends Item implements DyableItem {
    public BaseAcDyableItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull Component getName(ItemStack pStack) {
        var comp = pStack.getTag();
        String dyeName = "White";
        String label = "";
        if (comp != null) {
            var dyeColor = DyeColor.byFireworkColor(comp.getInt("color"));
            if (dyeColor != null) {
                dyeName = Component.translatable("item.minecraft.firework_star." + dyeColor).getString();
            }

            label = comp.getString("label");
        }

        var itemName = super.getName(pStack).getString().formatted(dyeName);
        if (!label.isEmpty())
            itemName += " (%s)".formatted(label);
        return Component.literal(itemName);
    }
}
