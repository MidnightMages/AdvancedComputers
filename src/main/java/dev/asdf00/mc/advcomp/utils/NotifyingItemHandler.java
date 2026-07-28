package dev.asdf00.mc.advcomp.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class NotifyingItemHandler extends ItemStackHandler {
    private final BlockEntity be;
    private final Consumer<Integer> onItemSlotChanged;
    private final BiFunction<Integer, ItemStack, Integer> itemstackMaxAcceptSize;

    public NotifyingItemHandler(BlockEntity be, int size, BiFunction<Integer, ItemStack, Integer> itemstackMaxAcceptSize) {
        this(be, size, itemstackMaxAcceptSize, null);
    }

    public NotifyingItemHandler(BlockEntity be, int size, BiFunction<Integer, ItemStack, Integer> itemstackMaxAcceptSize, Consumer<Integer> onItemSlotChanged) {
        super(size);
        this.be = be;
        this.onItemSlotChanged = onItemSlotChanged;
        this.itemstackMaxAcceptSize = itemstackMaxAcceptSize;
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        be.setChanged();
        if(onItemSlotChanged != null)
            onItemSlotChanged.accept(slot);
    }

    public CompoundTag serializeNBT() {
        throw new UnsupportedOperationException();
    }

    public void deserializeNBT(CompoundTag t) {
        throw new UnsupportedOperationException();
    }

    public void saveContents(@NotNull CompoundTag nbt) {
        var serialized = super.serializeNBT();
        if (!serialized.getList("Items", Tag.TAG_COMPOUND).isEmpty())
            nbt.put("inventory", serialized);
    }

    public void loadContents(@NotNull CompoundTag nbt) {
        super.deserializeNBT(nbt.getCompound("inventory"));
    }

    public boolean containsAnyItem() {
        for (int i = 0; i < getSlots(); i++) {
            if (!getStackInSlot(i).isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        var maxSizeToInsert = itemstackMaxAcceptSize.apply(slot, stack);
        if (maxSizeToInsert == -1)
            maxSizeToInsert = stack.getMaxStackSize();


        var canInsertThisMuch = maxSizeToInsert - this.getStackInSlot(slot).getCount();
        var extraItemsToReturn = stack.getCount() - canInsertThisMuch;

        if (canInsertThisMuch <= 0) {
            return stack;
        }


        var stackToInsert = stack.copyWithCount(canInsertThisMuch);
        var leftoverStack = super.insertItem(slot, stackToInsert, simulate);
        return leftoverStack.copyWithCount(leftoverStack.getCount() + extraItemsToReturn);
    }
}
