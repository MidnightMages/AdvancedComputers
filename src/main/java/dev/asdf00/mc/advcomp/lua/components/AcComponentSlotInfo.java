package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.mc.advcomp.types.RuntimeAssert;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Item component --> slotindex != -1
 * Block component --> slotindex == -1; inventoryOwner is that block and NOT instance of ComputerBlockEntity
 * BuiltIn component --> slotindex == -1; inventoryOwner is that block and IS instance of ComputerBlockEntity
 */
public final class AcComponentSlotInfo {
    private final BlockPos inventoryOwnerPos;
    private final int slotIndex;

    /**
     * @param inventoryOwnerPos
     * @param slotIndex      -1 means that this is a component provided by the block itself.
     */
    private AcComponentSlotInfo(BlockPos inventoryOwnerPos, int slotIndex) {
        this.inventoryOwnerPos = inventoryOwnerPos;
        this.slotIndex = slotIndex;
    }

    public boolean isBlockOrBuiltInComponent() {
        return slotIndex == -1;
    }

    public boolean isItemComponent() {
        return !isBlockOrBuiltInComponent();
    }

    public static AcComponentSlotInfo ofBlockComponent(BlockPos inventoryOwnerPos) {
        RuntimeAssert.RuntimeAssert(inventoryOwnerPos != null, "inventory owner must be nonnull");
        return new AcComponentSlotInfo(inventoryOwnerPos, -1);
    }

    public static AcComponentSlotInfo ofBlockComponent(BlockEntity inventoryOwner) {
        return ofBlockComponent(inventoryOwner.getBlockPos());
    }

    public static AcComponentSlotInfo ofItemComponent(BlockPos inventoryOwnerPos, int slotIndex) {
        RuntimeAssert.RuntimeAssert(slotIndex >= 0, "slotIndex>=0");
        RuntimeAssert.RuntimeAssert(inventoryOwnerPos != null, "inventory owner must be nonnull");
        return new AcComponentSlotInfo(inventoryOwnerPos, slotIndex);
    }

    public static AcComponentSlotInfo parse(String data) {
        var splitted = data.split(":", 2);
        return new AcComponentSlotInfo(
                BlockPos.of(Long.parseLong(splitted[0])),
                Integer.parseInt(splitted[1])
        );
    }

    public String getParsableIdentifier() {
        return "%s:%s".formatted(inventoryOwnerPos.asLong(), slotIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AcComponentSlotInfo other) {
            return Objects.equals(other.getParsableIdentifier(), getParsableIdentifier());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        var l = inventoryOwnerPos.asLong();
        var accumulator = (int) l;
        accumulator ^= ((int) (l >> 32)) * 7;
        accumulator ^= slotIndex * 17;
        return accumulator;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public BlockPos getInventoryOwnerPos() {
        return inventoryOwnerPos;
    }
}
