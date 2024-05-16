package dev.asdf00.mc.advcomp.datagen;

import com.google.gson.JsonObject;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.types.DyeCustomRecipe;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class RecipeGenerator extends RecipeProvider {
    public RecipeGenerator(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> pWriter) {
        var paper = getVanillaItem("paper");
        var keyCarditem = AdvancedComputers.KEYCARD_BASIC_ITEM.get();
        shaped(keyCarditem)
                .pattern("IIP")
                .pattern("IIP")
                .pattern("IIP")
                .define('I', Tags.Items.NUGGETS_IRON)
                .define('P', paper)
                .unlockedBy("item", has(paper))
                .save(pWriter);

        SpecialRecipeBuilder.special(DyeCustomRecipe.serializer).save(pWriter, "keycard_dyed");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, keyCarditem)
                .requires(keyCarditem)
                .requires(Tags.Items.DYES)
                .unlockedBy("item", has(keyCarditem))
                .save(pWriter, "advancedcomputers:keycard_basic_item_dye");
    }

    private ItemLike getVanillaItem(String name) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse("minecraft:" + name));
    }

    private ShapedRecipeBuilder shaped(ItemLike item) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item);
    }
}
