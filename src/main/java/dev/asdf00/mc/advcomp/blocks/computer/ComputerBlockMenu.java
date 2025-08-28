package dev.asdf00.mc.advcomp.blocks.computer;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ComputerBlockMenu extends AbstractContainerMenu {
    public final ComputerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public ComputerBlockMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(TE_INVENTORY_SLOT_COUNT));
    }

    public ComputerBlockMenu(int pContainerId, Inventory playerInv, BlockEntity be, ContainerData cd) {
        super(AdvancedComputers.COMPUTER_MENU.get(), pContainerId);
        checkContainerSize(playerInv, TE_INVENTORY_SLOT_COUNT);
        blockEntity = (ComputerBlockEntity) be;
        level = playerInv.player.level();
        data = cd;

        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 78, 10));
            addSlotRow(iItemHandler, 1, 98, 10, 4);
            addSlotRow(iItemHandler, 5, 98, 30, 4);
            addSlotRow(iItemHandler, 9, 62, 50, 6);
        });
    }

    void addSlotRow(IItemHandler iItemHandler, int indexStart, int xPos, int yPos, int count) {
        for (int i = 0; i < count; i++) {
            this.addSlot(new SlotItemHandler(iItemHandler, indexStart + i, xPos + 18 * i, yPos));
        }
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    public static final int TE_INVENTORY_SLOT_COUNT = 15;  // must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                                                                             + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @SuppressWarnings("unchecked")
    private static final RegistryObject<Block>[] validComputerBlocks = new RegistryObject[] {
            AdvancedComputers.COMPUTER_BLOCK_WOOD.block(),
            AdvancedComputers.COMPUTER_BLOCK.block()
    };

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        for (var b : validComputerBlocks)
            if (stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer, b.get()))
                return true;

        return false;
    }

    private static final int inventoryPosX = 8;
    private static final int inventoryPosY = 84;
    private static final int hotbarPosY = inventoryPosY + 58;

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + (i + 1) * 9, inventoryPosX + j * 18, inventoryPosY + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, inventoryPosX + i * 18, hotbarPosY));
        }
    }
}
