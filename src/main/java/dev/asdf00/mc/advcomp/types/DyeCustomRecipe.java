package dev.asdf00.mc.advcomp.types;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

;

public class DyeCustomRecipe extends CustomRecipe {
    public final static RecipeSerializer<DyeCustomRecipe> serializer = new SimpleCraftingRecipeSerializer<>(DyeCustomRecipe::new);

    public DyeCustomRecipe(ResourceLocation pId, CraftingBookCategory pCategory) {
        super(pId, pCategory);
    }

    @Override
    public boolean matches(@NotNull CraftingContainer pContainer, @NotNull Level pLevel) {
        boolean hasOurItem = false;
        boolean hasDye = false;

        for (int i = 0; i < pContainer.getContainerSize(); i++) {
            var is = pContainer.getItem(i);
            if (is.isEmpty())
                continue;

            var item = is.getItem();

            if (item instanceof IAcDyableItem) {
                if (hasOurItem)
                    return false;
                hasOurItem = true;
            }
            else if (item instanceof DyeItem) {
                if (hasDye)
                    return false;
                hasDye = true;
            }
        }
        return hasOurItem && hasDye;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer pContainer, @NotNull RegistryAccess pRegistryAccess) {
        ItemStack itemToDye = null;
        DyeItem dyeItem = null;

        for (int i = 0; i < pContainer.getContainerSize(); i++) {
            var is = pContainer.getItem(i);
            if (is.isEmpty())
                continue;

            var item = is.getItem();

            if (item instanceof IAcDyableItem) {
                if (itemToDye != null)
                    return ItemStack.EMPTY;
                itemToDye = is;
            }
            else if (item instanceof DyeItem di) {
                if (dyeItem != null)
                    return ItemStack.EMPTY;
                dyeItem = di;
            }
        }
        if ((itemToDye != null) && (dyeItem != null)) { // success
            var rv = itemToDye.copyWithCount(1);
            rv.getOrCreateTag().putInt("color", dyeItem.getDyeColor().getFireworkColor());
            return rv;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth * pHeight >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return serializer;
    }

}
