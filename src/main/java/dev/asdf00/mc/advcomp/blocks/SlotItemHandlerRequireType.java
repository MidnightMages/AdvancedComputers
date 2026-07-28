package dev.asdf00.mc.advcomp.blocks;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SlotItemHandlerRequireType extends SlotItemHandler {
    private final Function<ItemStack, Boolean> mayPlace;
    private int maxStackSize = 1;

    public SlotItemHandlerRequireType(IItemHandler itemHandler, int index, int xPosition, int yPosition, Class<?> requiredType) {
        super(itemHandler, index, xPosition, yPosition);
        this.mayPlace = stack -> isItemDerivedFromType(stack, requiredType);
    }

    public SlotItemHandlerRequireType(IItemHandler itemHandler, int index, int xPosition, int yPosition, Function<ItemStack, Boolean> mayPlace) {
        super(itemHandler, index, xPosition, yPosition);
        this.mayPlace = mayPlace;
    }

    public static SlotItemHandlerRequireType fromTypeConstraints(IItemHandler itemHandler, int index, int xPosition, int yPosition,
                                                                 Class<?> mustInheritFrom, Class<?>[] andMustNotInheritFrom) {
        return new SlotItemHandlerRequireType(itemHandler, index, xPosition, yPosition, stack -> {
            if (!isItemDerivedFromType(stack, mustInheritFrom))
                return false;

            for (Class<?> e : andMustNotInheritFrom) {
                if (isItemDerivedFromType(stack, e))
                    return false;
            }

            return true;
        });
    }

    private static boolean isItemDerivedFromType(ItemStack stack, Class<?> requiredType) {
        return requiredType.isAssignableFrom(stack.getItem().getClass());
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return mayPlace.apply(stack) && super.mayPlace(stack);
    }

    @Override
    public int getMaxStackSize() {
        return maxStackSize;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return maxStackSize;
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        super.set(stack);
    }

    public Slot withMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }
}
