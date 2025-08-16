package dev.asdf00.mc.advcomp.datagen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.types.DyeCustomRecipe;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.minecraft.tags.TagEntry.tag;

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


        var diamond = getVanillaItem("diamond");
        var advKeyCarditem = AdvancedComputers.KEYCARD_ADVANCED_ITEM.get();
        shaped(advKeyCarditem)
                .pattern("IDI")
                .pattern("III")
                .pattern("III")
                .define('I', Tags.Items.NUGGETS_IRON)
                .define('D', diamond)
                .unlockedBy("item", has(diamond))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, advKeyCarditem)
                .requires(advKeyCarditem)
                .requires(Tags.Items.DYES)
                .unlockedBy("item", has(advKeyCarditem))
                .save(pWriter, "advancedcomputers:keycard_advanced_item_dye");


        var woodComputer = AdvancedComputers.COMPUTER_BLOCK.blockItem().get();
        var stone = getVanillaItem("stone");
        shaped(woodComputer)
                .pattern("WWW")
                .pattern("WGW")
                .pattern("WSW")
                .define('W', ItemTags.create(new ResourceLocation("minecraft", "logs")))
                .define('S', stone)
                .define('G', Tags.Items.GLASS_PANES)
                .unlockedBy("item", has(stone))
                .save(pWriter);

    }

    private ItemLike getVanillaItem(String name) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse("minecraft:" + name));
    }

    private ShapedRecipeBuilder shaped(ItemLike item) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item);
    }
}
