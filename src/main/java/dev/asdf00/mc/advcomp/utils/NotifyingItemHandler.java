package dev.asdf00.mc.advcomp.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class NotifyingItemHandler extends ItemStackHandler {
    private final BlockEntity be;
    private final Consumer<Integer> onItemSlotChanged;

    public NotifyingItemHandler(BlockEntity be, int size) {
        this(be, size, null);
    }
    public NotifyingItemHandler(BlockEntity be, int size, Consumer<Integer> onItemSlotChanged) {
        super(size);
        this.be = be;
        this.onItemSlotChanged = onItemSlotChanged;
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
}
