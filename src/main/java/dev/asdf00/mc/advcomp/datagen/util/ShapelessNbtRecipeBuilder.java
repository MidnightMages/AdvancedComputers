package dev.asdf00.mc.advcomp.datagen.util;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.Consumer;

public class ShapelessNbtRecipeBuilder extends ShapelessRecipeBuilder {
    public ShapelessNbtRecipeBuilder(RecipeCategory pCategory, ItemLike pResult, int pCount) {
        super(pCategory, pResult, pCount);
    }

    private final HashMap<String, String> nbtToAddStr = new HashMap<>();
    private final HashMap<String, Integer> nbtToAddNumber = new HashMap<>();

    public ShapelessNbtRecipeBuilder addNbtToResult(String key, String value) {
        nbtToAddStr.put(key, value);
        return this;
    }

    public ShapelessNbtRecipeBuilder addNbtToResult(String key, int value) {
        nbtToAddNumber.put(key, value);
        return this;
    }

    public static ShapelessNbtRecipeBuilder shapeless(@NotNull RecipeCategory pCategory, ItemLike pResult) {
        return new ShapelessNbtRecipeBuilder(pCategory, pResult, 1);
    }

    public static ShapelessNbtRecipeBuilder shapeless(@NotNull RecipeCategory pCategory, ItemLike pResult, int pCount) {
        return new ShapelessNbtRecipeBuilder(pCategory, pResult, pCount);
    }

    @Override
    public void save(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer, @NotNull ResourceLocation pRecipeId) {
        Consumer<FinishedRecipe> proxyConsumer = p -> pFinishedRecipeConsumer.accept(new Result(p));
        super.save(proxyConsumer, pRecipeId);
    }

    public class Result implements FinishedRecipe {

        private final FinishedRecipe original;

        public Result(FinishedRecipe original) {
            this.original = original;
        }

        public void serializeRecipeData(@NotNull JsonObject pJson) {
            original.serializeRecipeData(pJson);
            var nbt = new JsonObject();
            for (var k : nbtToAddStr.keySet())
                nbt.addProperty(k, nbtToAddStr.get(k));

            for (var k : nbtToAddNumber.keySet())
                nbt.addProperty(k, nbtToAddNumber.get(k));

            pJson.getAsJsonObject("result").add("nbt", nbt);
        }

        @Override
        public @NotNull ResourceLocation getId() {
            return original.getId();
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return original.getType();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return original.serializeAdvancement();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return original.getAdvancementId();
        }
    }
}
