package dev.asdf00.mc.advcomp.items.punchcard;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class PunchcardItemMenu extends AbstractContainerMenu {
    private final InteractionHand usedHand;
    private final Level level;

    public PunchcardItemMenu(int pContainerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(pContainerId, playerInv, extraData.readEnum(InteractionHand.class));
    }

    public PunchcardItemMenu(int pContainerId, Inventory playerInv, InteractionHand pUsedHand) {
        super(AdvancedComputers.PUNCHCARD_MENU.get(), pContainerId);
        level = playerInv.player.level();
        usedHand = pUsedHand;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return pPlayer.getItemInHand(usedHand).is(AdvancedComputers.PUNCHCARD_ITEM.get());
    }
}
