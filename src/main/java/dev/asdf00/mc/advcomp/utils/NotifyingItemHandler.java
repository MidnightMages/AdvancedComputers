package dev.asdf00.mc.advcomp.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class NotifyingItemHandler extends ItemStackHandler {
    private final BlockEntity be;

    public NotifyingItemHandler(BlockEntity be, int size) {
        super(size);
        this.be = be;
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        be.setChanged();
    }

    public CompoundTag serializeNBT() {
        throw new UnsupportedOperationException();
    }

    public void deserializeNBT(CompoundTag t) {
        throw new UnsupportedOperationException();
    }

    public void saveContents(@NotNull CompoundTag nbt) {
        nbt.put("inventory", super.serializeNBT());
    }

    public void loadContents(@NotNull CompoundTag nbt) {
        super.deserializeNBT(nbt.getCompound("inventory"));
    }
}
