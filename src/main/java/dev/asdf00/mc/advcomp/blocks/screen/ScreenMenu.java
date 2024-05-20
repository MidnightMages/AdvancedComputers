package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ScreenMenu extends AbstractContainerMenu {
    public final ScreenBlockEntity blockEntity;
    private final Level level;

    public ScreenMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ScreenMenu(int pContainerId, Inventory playerInv, BlockEntity be) {
        super(AdvancedComputers.SCREEN_MENU.get(), pContainerId);
//        checkContainerSize(playerInv, 2);
        blockEntity = (ScreenBlockEntity) be;
        level = playerInv.player.level();
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer, AdvancedComputers.SCREEN_BLOCK.block().get());
    }
}
