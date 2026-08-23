package dev.asdf00.mc.advcomp.blocks.auto_crafter;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class AutoCrafterBlockUD extends BaseAcBlockEntityComponentUD<AutoCrafterBlockEntity> {

    public AutoCrafterBlockUD(AutoCrafterBlockEntity autoCrafterBlockEntity) {
        super("autoCrafter", autoCrafterBlockEntity);
    }

    private AutoCrafterBlockUD(LuaVirtualMachine acVm, boolean isAccessible, AutoCrafterBlockEntity autoCrafterBlockEntity) {
        super("autoCrafter", acVm, isAccessible, autoCrafterBlockEntity);
    }

    private RecipeManager getRecipeManager() {
        var level = blockEntity.getLevel();
        assert level != null;
        var server = level.getServer();
        assert server != null;
        return server.getRecipeManager();
    }

//    @SuppressWarnings("unused")
//    @LuaExposed(LuaExposed.Policy.READ)
//    public final LuaProperty containsMainboard = LuaProperty.ofBoolean(() -> !blockEntity.itemHandler.getStackInSlot(0).isEmpty(), null);

    @LuaCallable
    public LuaObject searchRecipesWithIngredient(String ingredientName) {
        if (ingredientName == null || ingredientName.isEmpty())
            throw new LuaJavaError("Item '' was not found, needs to be in format 'minecraft:stick' for example.");

        var baseIngredient = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(ingredientName));
        if (baseIngredient == null || baseIngredient.asItem().equals(Items.AIR))
            throw new LuaJavaError("Item '%s' was not found, needs to be in format 'minecraft:stick' for example.".formatted(ingredientName));

        var recipeManager = getRecipeManager();
        var registryAccess = blockEntity.getLevel().registryAccess();
        List<CraftingRecipe> allRecipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);

        ArrayList<LuaObject> survivingRecipes = new ArrayList<>();

        for (var recipe : allRecipes) {
            var ingredients = recipe.getIngredients();
            for (var ing : ingredients) {
                if (ing.test(new ItemStack(baseIngredient))) {
                    survivingRecipes.add(LuaObject.of(
                            getRegistryNameForItem(recipe.getResultItem(registryAccess).getItem())
                    ));
                    break;
                }
            }
        }

        return LuaObject.tableFromArray(survivingRecipes.toArray(LuaObject[]::new));
    }

    @LuaCallable
    public LuaObject searchRecipesWithResult(String resultName) {
        if (resultName == null)
            throw new LuaJavaError("Item '' was not found, needs to be in format 'minecraft:stick' for example.");

        var resultItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(resultName));
        if (resultItem == null)
            throw new LuaJavaError("Item '%s' was not found, needs to be in format 'minecraft:stick' for example.".formatted(resultName));

        var recipeManager = getRecipeManager();
        var registryAccess = blockEntity.getLevel().registryAccess();
        LuaObject[] allRecipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(x -> x.getResultItem(registryAccess).getItem().equals(resultItem))
                .limit(100)
                .map(this::toLuaRecipe)
                .toArray(LuaObject[]::new);

        return LuaObject.tableFromArray(allRecipes);
    }

    @LuaCallable
    public LuaObject searchItemsByName(String searchString) {
        if (searchString == null || searchString.isEmpty())
            throw new LuaJavaError("Search string cannot be empty");

        var result = ForgeRegistries.ITEMS.getKeys()
                .stream()
                .filter(x -> x.toString().contains(searchString))
                .limit(100)
                .map(x -> LuaObject.of(x.toString()))
                .toArray(LuaObject[]::new);

        return LuaObject.tableFromArray(result);
    }

    private LuaObject toLuaRecipe(CraftingRecipe recipe) {
        return LuaObject.of("{%s} -> %s".formatted(
                recipe.getIngredients()
                        .stream()
                        .map(i -> Arrays.stream(i.getItems())
                                .map(x -> getRegistryNameForItem(x.getItem()))
                                .collect(Collectors.joining(" ")))
                        .collect(Collectors.joining(";")),
                getRegistryNameForItem(recipe.getResultItem(this.blockEntity.getLevel().registryAccess()).getItem())
        ));
    }

    private String getRegistryNameForItem(Item i) {
        return ForgeRegistries.ITEMS.getKey(i).toString();
    }

    @LuaDeserializer
    public static AutoCrafterBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(AutoCrafterBlockEntity.class, AutoCrafterBlockUD::new, objs, reader, postActions, additionalData);
    }
}
