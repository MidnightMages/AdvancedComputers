package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private static final RegistryObject<Block>[] validScreenBlocks = (RegistryObject<Block>[]) new RegistryObject<?>[]{
            AdvancedComputers.SCREEN_BLOCK_WOOD.block(),
            AdvancedComputers.SCREEN_BLOCK.block(),
            AdvancedComputers.SCREEN_BLOCK_DIAMOND.block()
    };

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        for (var b : validScreenBlocks)
            if (stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer, b.get()))
                return true;

        return false;
    }
}
