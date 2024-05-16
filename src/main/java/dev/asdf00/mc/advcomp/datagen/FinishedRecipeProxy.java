package dev.asdf00.mc.advcomp.datagen;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FinishedRecipeProxy implements FinishedRecipe {
    private final FinishedRecipe f;
    private final JsonObject resultNbt;

    public FinishedRecipeProxy(FinishedRecipe f, JsonObject resultNbt) {
        this.f=f;
        this.resultNbt = resultNbt;
    }

    @Override
    public void serializeRecipeData(@NotNull JsonObject pJson) {
        f.serializeRecipeData(pJson);
        pJson.getAsJsonObject("result").add("nbt", resultNbt);
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return f.getId();
    }

    @Override
    public @NotNull RecipeSerializer<?> getType() {
        return f.getType();
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement() {
        return f.serializeAdvancement();
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementId() {
        return f.getAdvancementId();
    }
}
