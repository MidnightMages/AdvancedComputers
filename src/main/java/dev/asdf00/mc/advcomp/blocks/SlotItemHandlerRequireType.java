package dev.asdf00.mc.advcomp.blocks;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class SlotItemHandlerRequireType extends SlotItemHandler {
    private final Class<?> requiredType;

    public SlotItemHandlerRequireType(IItemHandler itemHandler, int index, int xPosition, int yPosition, Class<?> requiredType) {
        super(itemHandler, index, xPosition, yPosition);
        this.requiredType = requiredType;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return requiredType.isAssignableFrom(stack.getItem().getClass()) && super.mayPlace(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        super.set(stack.copyWithCount(1));
    }
}
