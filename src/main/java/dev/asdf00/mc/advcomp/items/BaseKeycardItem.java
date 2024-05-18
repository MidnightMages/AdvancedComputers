package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.types.IAcDyableItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class BaseKeycardItem extends Item implements IAcDyableItem {
    public BaseKeycardItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull Component getName(ItemStack pStack) {
        var comp = pStack.getTag();
        String dyeName = "White";
        if (comp != null) {
            var dyeColor = DyeColor.byFireworkColor(comp.getInt("color"));
            if (dyeColor != null) {
                dyeName = Component.translatable("item.minecraft.firework_star." + dyeColor).getString();
            }
        }

        return Component.literal(super.getName(pStack).getString().formatted(dyeName));
    }

    public abstract void onKeycardSwiped(BaseKeycardItem is);
}
