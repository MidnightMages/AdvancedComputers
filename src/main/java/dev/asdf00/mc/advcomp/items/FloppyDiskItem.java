package dev.asdf00.mc.advcomp.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FloppyDiskItem extends BaseAcDyableItem {
    public FloppyDiskItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack pStack, @NotNull Level pLevel, @NotNull Player pPlayer) {
        super.onCraftedBy(pStack, pLevel, pPlayer);
        if (pLevel.isClientSide()) return;

        var nbt = pStack.getTag();
        if (nbt != null && nbt.contains("desiredDiskData")) {
            var desiredData = nbt.getString("desiredDiskData");

            // remove desiredDiskData tag, write data to disk, set id to reference it
        }
    }
}
