package dev.asdf00.mc.advcomp.blocks.digital_crafter;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.Config;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Function;

public class DigitalCrafterBlockUD extends BaseAcBlockEntityComponentUD<DigitalCrafterBlockEntity> {

    public DigitalCrafterBlockUD(DigitalCrafterBlockEntity digitalCrafterBlockEntity) {
        super("digitalCrafter", digitalCrafterBlockEntity);
    }

    private DigitalCrafterBlockUD(LuaVirtualMachine acVm, boolean isAccessible, DigitalCrafterBlockEntity digitalCrafterBlockEntity) {
        super("digitalCrafter", acVm, isAccessible, digitalCrafterBlockEntity);
    }

    private long canCraftAgainAt = 0;

    private RecipeManager getRecipeManager() {
        var level = blockEntity.getLevel();
        assert level != null;
        var server = level.getServer();
        assert server != null;
        return server.getRecipeManager();
    }

    @SuppressWarnings("unused")
    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty cooldownRemaining = LuaProperty.ofDouble(() -> {
        var msRemaining = canCraftAgainAt - System.currentTimeMillis();
        return msRemaining > 0 ? Math.max(msRemaining / 1000d, 0.01) : 0;
    }, null);

    @SuppressWarnings("unused")
    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty baseCooldown = LuaProperty.ofDouble(() -> Config.componentCrafterCooldownMilliseconds, null);

    @LuaCallable
    public LuaObject searchRecipesWithIngredient(String ingredientName) {
        var baseIngredient = getItemFromNameOrThrow(ingredientName);

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
        var resultItem = getItemFromNameOrThrow(resultName);

        var recipeManager = getRecipeManager();
        var registryAccess = blockEntity.getLevel().registryAccess();
        LuaObject[] allRecipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(x -> x.getResultItem(registryAccess).getItem().equals(resultItem))
                .map(this::toLuaRecipe)
                .toArray(LuaObject[]::new);

        return LuaObject.tableFromArray(allRecipes);
    }

    @LuaCallable // returns how many items were actually crafted. Crafting requires a slot to be free or freed up during the crafting process.
    public int craft(LuaObject inputItemNames, String resultItemName, int maxAmount, boolean outputIntoOwnInventory) {
        // TODO water buckets probably wont return an empty bucket in the recipe
        var recipeManager = getRecipeManager();
        var registryAccess = blockEntity.getLevel().registryAccess();

        maxAmount = Math.min(maxAmount, 64);
        if (maxAmount <= 0)
            throw new LuaJavaError("Argument 3 'maxAmount' most be and integer larger than 0");

        // ------------ parse ingredients ------------
        if (!inputItemNames.isTable())
            throw new LuaJavaError("Expected argument 1 to be a table, but it was of type '%s'!".formatted(inputItemNames.getTypeAsString()));


        var inputItemArrayMapL = inputItemNames.asMap();
        var inputItemArray = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            var iLuaObject = inputItemArrayMapL.getOrDefault(LuaObject.of(i + 1), LuaObject.NIL);
            if (iLuaObject.isString() || iLuaObject.isNil()) {
                inputItemArray[i] = new ItemStack(iLuaObject.isNil() ? Items.AIR : getItemFromNameOrThrow(iLuaObject.getString()));
            } else {
                throw new LuaJavaError("Element at Lua index %s in argument 1 was not a string but of type '%s'!."
                        .formatted(i + 1, iLuaObject.getTypeAsString()));
            }
        }
        // -------------------------------------------

        var wantedResultItem = getItemFromNameOrThrow(resultItemName);

        var resultItemStack = new ItemStack(wantedResultItem);
        var maxStackSize = resultItemStack.isStackable() ? wantedResultItem.getMaxStackSize(resultItemStack) : 1;
        maxAmount = Math.min(maxAmount, maxStackSize);

        // first look for all crafting recipes that can output this item
        var matchingRecipesForOutput = new ArrayList<CraftingRecipe>();
        for (var recipe : recipeManager.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (recipe.getResultItem(registryAccess).getItem().equals(wantedResultItem))
                matchingRecipesForOutput.add(recipe);
        }

        if (matchingRecipesForOutput.isEmpty())
            throw new LuaJavaError("No crafting recipe found for this item.");

        // now find one that lists the same inputs as specified
        var fakeItems = NonNullList.of(new ItemStack(Items.AIR), inputItemArray);
        @SuppressWarnings("DataFlowIssue") // null as a first parameter should be fine for our purposes, even though it is unconventional
        var fakeInventory = new TransientCraftingContainer(null, 3, 3, fakeItems);
        CraftingRecipe match = null;
        for (var recipe : matchingRecipesForOutput) {
            if (recipe.matches(fakeInventory, getLevel())) {
                match = recipe;
            }
        }
        if (match == null)
            throw new LuaJavaError("Found at least one recipe for the requested result item, but none of the recipes matched the given input items.");

        maxAmount = Math.min(maxAmount, Math.min(resultItemStack.getMaxStackSize(), 64) / match.getResultItem(registryAccess).getCount()); // limit some more

        // once we have a recipe that matches, check if we actually have the listed ingredients

        var requiredCraftingIngredients = new HashMap<Item, Integer>();
        for (ItemStack itemStack : inputItemArray) {
            var item = itemStack.getItem();
            var oldCount = requiredCraftingIngredients.getOrDefault(item, 0);
            requiredCraftingIngredients.put(item, oldCount + maxAmount);
        }
        requiredCraftingIngredients.remove(Items.AIR);

        final var maxAmountFinal = maxAmount;
        final var matchFinal = match;
        this.blockEntity.runOnTickThread(() -> {
            var ih = this.blockEntity.itemHandler;
            Function<Boolean, Integer> checkOrConsume = ((Boolean dryRun) -> {
                // dryRun --> if true, do NOT consume any items, but just check if we have enough and how much space would be freed up
                //            returns 0 if we cannot craft this; 1 if we can craft it by throwing it out the top; 2 if we can craft and store in inventory
                //            3 --> we dont have enough room for storing intermediate outputs, e.g. empty buckets that originate from crafting a cake
                // not dry run --> removes the items and returns 0.

                var itemsLeftToConsume = new HashMap<>(requiredCraftingIngredients);
                int freeSlotsAfterOperation = 0;
                var remainingItems = matchFinal.getRemainingItems(fakeInventory);
                var remainingItemsStacked = new ArrayList<ItemStack>();
                for (var remainingStack : remainingItems) {
                    for (int i = 0; i < remainingItemsStacked.size(); i++) {
                        var candidateToStackWith = remainingItemsStacked.get(i);
                        if (ItemHandlerHelper.canItemStacksStack(remainingStack, candidateToStackWith)) {
                            var totalStackSize = candidateToStackWith.getCount() + remainingStack.getCount();
                            var existingCount = Math.min(candidateToStackWith.getMaxStackSize(), totalStackSize);
                            remainingItemsStacked.set(i, candidateToStackWith.copyWithCount(existingCount));
                            remainingStack = remainingStack.copyWithCount(totalStackSize - existingCount);
                        }

                        var remainingCount = remainingStack.getCount();
                        assert remainingCount >= 0;
                        if (remainingCount == 0)
                            break;
                    }

                    if (remainingStack.getCount() > 0) {
                        remainingItemsStacked.add(remainingStack);
                    }
                }

                var slotCnt = ih.getSlots();
                for (int i = slotCnt - 1; i >= 0; i--) {
                    var stack = ih.getStackInSlot(i);
                    if (stack.isEmpty()) {
                        freeSlotsAfterOperation++; // we could put the result here
                        continue;
                    }

                    var neededItemsOfThisType = itemsLeftToConsume.getOrDefault(stack.getItem(), 0);
                    if (neededItemsOfThisType > 0) {
                        var consumeThisManyItemsOfThisStack = Math.min(stack.getCount(), neededItemsOfThisType);
                        var newCount = stack.getCount() - consumeThisManyItemsOfThisStack;
                        if (!dryRun) {
                            ih.setStackInSlot(i, newCount > 0 ? stack.copyWithCount(newCount) : ItemStack.EMPTY);
                        }
                        if (newCount == 0) // a slot was freed up, so we could put the result here;
                            freeSlotsAfterOperation++;

                        var remainingItemsToConsumeOfThisType = neededItemsOfThisType - consumeThisManyItemsOfThisStack;
                        if (remainingItemsToConsumeOfThisType == 0)
                            itemsLeftToConsume.remove(stack.getItem());
                        else
                            itemsLeftToConsume.put(stack.getItem(), remainingItemsToConsumeOfThisType);
                    }
                }

                // left over items go back into inventory
                boolean hasEnoughFreeSlotsAfterOperation = freeSlotsAfterOperation > remainingItemsStacked.size();
                boolean hasEnoughFreeSlotsToStoreIngredients = freeSlotsAfterOperation >= remainingItemsStacked.size();
                if (dryRun) {
                    if (!itemsLeftToConsume.isEmpty())
                        return 0;
                    else if (!hasEnoughFreeSlotsToStoreIngredients) {
                        return 3;
                    } else { // now we need to know if theres a free slot or we can stack the result item
                        return hasEnoughFreeSlotsAfterOperation ? 2 : 1;
                    }
                } else {
                    var extraOutputsLeftToReturn = new ArrayList<>(remainingItemsStacked);
                    for (var currentStackToDistribute : extraOutputsLeftToReturn) {
                        for (int i = 0; i < slotCnt; i++) {
                            var slotStack = ih.getStackInSlot(i);
                            if (slotStack.isEmpty()) {
                                ih.setStackInSlot(i, currentStackToDistribute);
                                currentStackToDistribute = ItemStack.EMPTY;
                            } else if (ItemHandlerHelper.canItemStacksStack(currentStackToDistribute, slotStack)) {
                                var totalStackSize = slotStack.getCount() + currentStackToDistribute.getCount();
                                var existingCount = Math.min(slotStack.getMaxStackSize(), totalStackSize);
                                ih.setStackInSlot(i, slotStack.copyWithCount(existingCount));
                                currentStackToDistribute = currentStackToDistribute.copyWithCount(totalStackSize - existingCount);
                            }
                            assert currentStackToDistribute.getCount() >= 0;
                            if (currentStackToDistribute.isEmpty())
                                break;
                        }
                        assert currentStackToDistribute.isEmpty(); // if this triggers then we somehow ran out of inventory space
                    }

                    return 0;
                }
            });
            var dryRunResult = checkOrConsume.apply(true);
            switch (dryRunResult) {
                case 0 -> throw new LuaJavaError("Not all of the supplied ingredients in argument 1 are available in sufficient quantity.");
                case 1 -> { // we have all items but we dont have a free spot --> error if we cant throw it out the top
                    if (outputIntoOwnInventory)
                        throw new LuaJavaError("It was requested to output the crafting result into the own inventory, but there is no free slot.");
                }
                case 2 -> { // success
                }

                // not sure if we will ever encounter this, but we handle it just in case.
                case 3 -> throw new LuaJavaError("Not enough slots left to store intermediate outputs, e.g. empty buckets when crafting a cake.");
                default -> throw new IllegalStateException("unreachable");
            }

            // at this point all is well, so proceed with the crafting
            checkOrConsume.apply(false);

            // then spawn the result
            var itemstackToSpawn = resultItemStack.copyWithCount(maxAmountFinal * matchFinal.getResultItem(registryAccess).getCount());
            if (outputIntoOwnInventory) {
                for (int i = 0; i < ih.getSlots(); i++) {
                    itemstackToSpawn = ih.insertItem(i, itemstackToSpawn, false);
                    if (itemstackToSpawn.isEmpty()) break;
                }
            } else {
                var entityAbove = getLevel().getBlockEntity(blockEntity.getBlockPos().above());

                // first try to insert it into any chest-like blocks
                LazyOptional<IItemHandler> entityAbove_ItemHandlerCap;
                if (entityAbove != null && ((entityAbove_ItemHandlerCap = entityAbove.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)).isPresent())) {
                    var upperIh = entityAbove_ItemHandlerCap.orElseThrow(() -> new IllegalStateException("failed to get capability even though it should have been there"));
                    var slotCnt = upperIh.getSlots();
                    for (int i = 0; i < slotCnt; i++) {
                        // slot stack simulate
                        itemstackToSpawn = upperIh.insertItem(i, itemstackToSpawn, false);
                        if (itemstackToSpawn.isEmpty())
                            break;
                    }
                }

                // if anything is left, throw it into the air
                if (!itemstackToSpawn.isEmpty()) {
                    var spawnPos = blockEntity.getBlockPos().getCenter().add(0, 0.6, 0);
                    var itemEntity = new ItemEntity(getLevel(), spawnPos.x, spawnPos.y, spawnPos.z, itemstackToSpawn, 0, 0.25, 0);
                    getLevel().addFreshEntity(itemEntity);
                }
            }
        });

        canCraftAgainAt = System.currentTimeMillis() + (int) (Config.componentCrafterCooldownMilliseconds * Math.sqrt(maxAmount));
        return maxAmount;
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
        var recipeItems = new Ingredient[9];
        var ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            for (int x = 0; x < shaped.getWidth(); x++) {
                for (int y = 0; y < shaped.getHeight(); y++) {
                    recipeItems[x + y * 3] = ingredients.get(x + y * shaped.getWidth());
                }
            }
        } else {
            recipeItems = ingredients.toArray(Ingredient[]::new);
        }

        var rv = LuaObject.table();
        rv.set("ingredients", LuaObject.tableFromArray(
                Arrays.stream(recipeItems)
                        .map(slot -> slot == null ? LuaObject.NIL : LuaObject.tableFromArray(Arrays.stream(slot.getItems())
                                .map(possibleItem -> LuaObject.of(getRegistryNameForItem(possibleItem.getItem())))
                                .toArray(LuaObject[]::new))
                        ).toArray(LuaObject[]::new))
        );

        var resIs = recipe.getResultItem(getLevel().registryAccess());
        rv.set("result", LuaObject.of(getRegistryNameForItem(resIs.getItem())));
        rv.set("resultCount", LuaObject.of(resIs.getCount()));
        return rv;
    }

    private String getRegistryNameForItem(Item i) {
        return Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(i)).toString();
    }

    private Item getItemFromNameOrThrow(String name) {
        var rl = (name == null || name.isEmpty() || name.isBlank()) ? null : ResourceLocation.tryParse(name);
        var wantedResultItem = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        if (wantedResultItem == null || wantedResultItem.asItem().equals(Items.AIR))
            throw new LuaJavaError("Item '%s' was not found, needs to be in format 'minecraft:stick' for example.".formatted(name));
        return wantedResultItem;
    }

    private Level getLevel() {
        return Objects.requireNonNull(this.blockEntity.getLevel());
    }

    @LuaDeserializer
    public static DigitalCrafterBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(DigitalCrafterBlockEntity.class, DigitalCrafterBlockUD::new, objs, reader, postActions, additionalData);
    }
}
