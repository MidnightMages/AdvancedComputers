package dev.asdf00.mc.advcomp.blocks.computer;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.SlotItemHandlerRequireType;
import dev.asdf00.mc.advcomp.items.BaseDataStorageItem;
import dev.asdf00.mc.advcomp.items.DiskItem;
import dev.asdf00.mc.advcomp.items.FloppyDiskItem;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.lua.components.AcItemComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ComputerBlockMenu extends AbstractContainerMenu {
    public final ComputerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public ComputerBlockMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, (ComputerBlockEntity) (inv.player.level().getBlockEntity(extraData.readBlockPos())));
    }

    private ComputerBlockMenu(int pContainerId, Inventory inv, ComputerBlockEntity cbe) {
        this(pContainerId, inv, cbe, new SimpleContainerData(TE_INVENTORY_SLOT_COUNT(cbe.getTier())));
    }

    public ComputerBlockMenu(int pContainerId, Inventory playerInv, ComputerBlockEntity be, ContainerData cd) {
        super(AdvancedComputers.COMPUTER_MENU.get(), pContainerId);
        var tier = be.getTier();
        var teInvSlotCount = TE_INVENTORY_SLOT_COUNT(tier);
        checkContainerSize(playerInv, teInvSlotCount);
        blockEntity = be;
        level = playerInv.player.level();
        data = cd;

        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            int currSlotIndex = 0;
            this.addSlot(new SlotItemHandlerRequireType(iItemHandler, currSlotIndex++, 78, 10,
                    MainboardItem.class));

            for (int i = 0; i < tier.diskSlotCount; i++) {
                this.addSlot(new SlotItemHandlerRequireType(iItemHandler, currSlotIndex++, 98 + 18 * i, 10, DiskItem.class));
            }
            this.addSlot(new SlotItemHandlerRequireType(iItemHandler, currSlotIndex++, 98 + 18 * 3, 10, FloppyDiskItem.class));

            addSlotRow(iItemHandler, currSlotIndex, 62, 50, tier.componentSlotCount);
        });
    }

    void addSlotRow(IItemHandler iItemHandler, int indexStart, int xPos, int yPos, int count) {
        for (int i = 0; i < count; i++) {
            this.addSlot(SlotItemHandlerRequireType.fromTypeConstraints(iItemHandler, indexStart + i, xPos + 18 * i, yPos,
                    AcItemComponent.class, new Class[]{
                            MainboardItem.class, BaseDataStorageItem.class
                    }));
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
    static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    public static int TE_INVENTORY_SLOT_COUNT(ComputerTier tier) {
        return 1 + // mainboard
               tier.diskSlotCount +
               1 + // floppy
               tier.componentSlotCount; // used to be 11 for max tier
    } // must be the number of slots you have!

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int pIndex) {
        var tier = blockEntity.getTier();
        Slot sourceSlot = slots.get(pIndex);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                                                                             + TE_INVENTORY_SLOT_COUNT(tier), false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT(tier)) {
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
    private static final RegistryObject<Block>[] validComputerBlocks = (RegistryObject<Block>[]) new RegistryObject<?>[]{
            AdvancedComputers.COMPUTER_BLOCK_WOOD.block(),
            AdvancedComputers.COMPUTER_BLOCK.block(),
            AdvancedComputers.COMPUTER_BLOCK_DIAMOND.block(),
            AdvancedComputers.COMPUTER_BLOCK_NETHERITE.block(),
            AdvancedComputers.COMPUTER_BLOCK_CREATIVE.block(),
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
