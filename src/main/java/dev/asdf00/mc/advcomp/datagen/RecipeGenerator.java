package dev.asdf00.mc.advcomp.datagen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.datagen.util.ShapelessNbtRecipeBuilder;
import dev.asdf00.mc.advcomp.items.FloppyDiskItem;
import dev.asdf00.mc.advcomp.types.DyeCustomRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
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

        var floppyDiskitem = AdvancedComputers.FLOPPY_DISK_ITEM.get();
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, floppyDiskitem)
                .requires(floppyDiskitem)
                .requires(Tags.Items.DYES)
                .unlockedBy("item", has(floppyDiskitem))
                .save(pWriter, "advancedcomputers:floppy_disk_item_dye");

        shaped(floppyDiskitem)
                .pattern("III")
                .pattern("IPI")
                .pattern("ICI")
                .define('I', Tags.Items.NUGGETS_IRON)
                .define('P', paper)
                .define('C', Tags.Items.INGOTS_COPPER)
                .unlockedBy("item", has(paper))
                .save(pWriter);

        var stone = getVanillaItem("stone");
        var logs = ItemTags.create(new ResourceLocation("minecraft", "logs"));
        var planks = ItemTags.create(new ResourceLocation("minecraft", "planks"));
        var buttonsWood = ItemTags.create(new ResourceLocation("minecraft", "wooden_buttons"));



        shaped(AdvancedComputers.COMPUTER_BLOCK_WOOD.blockItem().get())
                .pattern("WWW")
                .pattern("WSW")
                .pattern("WGW")
                .define('W', logs)
                .define('S', buttonsWood)
                .define('G', stone)
                .unlockedBy("item", has(stone))
                .save(pWriter);

        shaped(AdvancedComputers.COMPUTER_BLOCK.blockItem().get())
                .pattern("III")
                .pattern("IRI")
                .pattern("IGI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .define('G', Tags.Items.STORAGE_BLOCKS_COPPER)
                .unlockedBy("item", has(Tags.Items.INGOTS_IRON))
                .save(pWriter);

        shaped(AdvancedComputers.COMPUTER_BLOCK_DIAMOND.blockItem().get())
                .pattern("DDD")
                .pattern("DID")
                .pattern("DCD")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('I', Tags.Items.STORAGE_BLOCKS_IRON)
                .define('C', getVanillaItem("crying_obsidian"))
                .unlockedBy("item", has(Tags.Items.GEMS_DIAMOND))
                .save(pWriter);

        shaped(AdvancedComputers.COMPUTER_BLOCK_NETHERITE.blockItem().get())
                .pattern("NNN")
                .pattern("NDN")
                .pattern("NRN")
                .define('N', Tags.Items.INGOTS_NETHERITE)
                .define('D', Tags.Items.STORAGE_BLOCKS_DIAMOND)
                .define('R', getVanillaItem("end_rod"))
                .unlockedBy("item", has(Tags.Items.INGOTS_NETHERITE))
                .save(pWriter);

        shaped(AdvancedComputers.SCREEN_BLOCK_WOOD.blockItem().get())
                .pattern("PPP")
                .pattern("PGP")
                .pattern("PWP")
                .define('P', planks)
                .define('W', logs)
                .define('G', Tags.Items.GLASS)
                .unlockedBy("item", has(planks))
                .save(pWriter);

        shaped(AdvancedComputers.SCREEN_BLOCK.blockItem().get())
                .pattern("PPP")
                .pattern("PGP")
                .pattern("HWH")
                .define('P', Tags.Items.INGOTS_IRON)
                .define('H', Tags.Items.INGOTS_GOLD)
                .define('W', stone)
                .define('G', Items.REDSTONE_LAMP)
                .unlockedBy("item", has(planks))
                .save(pWriter);

        shaped(AdvancedComputers.SCREEN_BLOCK_DIAMOND.blockItem().get())
                .pattern("PPP")
                .pattern("PGP")
                .pattern("HWH")
                .define('P', Tags.Items.GEMS_DIAMOND)
                .define('W', Items.PURPUR_BLOCK)
                .define('H', Tags.Items.STORAGE_BLOCKS_GOLD)
                .define('G', Items.ENDER_EYE)
                .unlockedBy("item", has(planks))
                .save(pWriter);

        shaped(AdvancedComputers.WAN_ROUTER_BLOCK_LOWTIER.blockItem().get())
                .pattern("IWI")
                .pattern("WCW")
                .pattern("IWI")
                .define('W', planks)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.COMPASS)
                .unlockedBy("item", has(planks))
                .save(pWriter);

        shaped(AdvancedComputers.WAN_ROUTER_BLOCK.blockItem().get())
                .pattern("IBI")
                .pattern("WCW")
                .pattern("IBI")
                .define('W', Tags.Items.GEMS_DIAMOND)
                .define('I', Tags.Items.OBSIDIAN)
                .define('B', Tags.Items.RODS_BLAZE)
                .define('C', Items.ENDER_EYE)
                .unlockedBy("item", has(planks))
                .save(pWriter);

        shaped(AdvancedComputers.NET_ROUTER_BLOCK.blockItem().get())
                .pattern("WNW")
                .pattern("NIN")
                .pattern("WNW")
                .define('W', planks)
                .define('N', AdvancedComputers.NETWORK_CABLE_BLOCK.blockItem().get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("item", has(AdvancedComputers.NETWORK_CABLE_BLOCK.blockItem().get()))
                .save(pWriter);

        shaped(AdvancedComputers.MAINBOARD_TIER_1_ITEM.get())
                .pattern("CXI")
                .pattern("XXI")
                .pattern("IIX")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('X', planks)
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("item", has(Tags.Items.INGOTS_COPPER))
                .save(pWriter);
        shaped(AdvancedComputers.MAINBOARD_TIER_2_ITEM.get())
                .pattern("CXI")
                .pattern("XXI")
                .pattern("IIX")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('X', Tags.Items.INGOTS_GOLD)
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("item", has(Tags.Items.INGOTS_COPPER))
                .save(pWriter);
        shaped(AdvancedComputers.MAINBOARD_TIER_3_ITEM.get())
                .pattern("CXI")
                .pattern("XXI")
                .pattern("IIX")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('X', Tags.Items.GEMS_DIAMOND)
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("item", has(Tags.Items.INGOTS_COPPER))
                .save(pWriter);
        shaped(AdvancedComputers.DEVICE_CABLE_BLOCK.blockItem().get(), 16)
                .pattern("WWW")
                .pattern("CRC")
                .pattern("WWW")
                .define('W', Items.BLUE_WOOL)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("item", has(Tags.Items.INGOTS_IRON))
                .save(pWriter);
        shaped(AdvancedComputers.NETWORK_CABLE_BLOCK.blockItem().get(), 16)
                .pattern("WWW")
                .pattern("CRC")
                .pattern("WWW")
                .define('W', Items.CYAN_WOOL)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("item", has(Tags.Items.INGOTS_IRON))
                .save(pWriter);

        var hddResults = new RegistryObject[]{AdvancedComputers.HDD_TIER_1_ITEM, AdvancedComputers.HDD_TIER_2_ITEM,
                AdvancedComputers.HDD_TIER_3_ITEM, AdvancedComputers.HDD_TIER_4_ITEM, AdvancedComputers.HDD_TIER_5_ITEM};

        var hddIngredients = new TagKey[]{Tags.Items.INGOTS_IRON, Tags.Items.INGOTS_COPPER, Tags.Items.INGOTS_GOLD,
                Tags.Items.GEMS_DIAMOND, Tags.Items.INGOTS_NETHERITE};

        for (int i = 0; i < hddResults.length; i++) {
            //noinspection unchecked
            shaped(((RegistryObject<Item>) hddResults[i]).get())
                    .pattern("NXN")
                    .pattern("XWX")
                    .pattern("NCN")
                    .define('N', Tags.Items.NUGGETS_IRON)
                    .define('X', hddIngredients[i])
                    .define('C', Tags.Items.INGOTS_COPPER)
                    .define('W', planks)
                    .unlockedBy("item", has(Tags.Items.INGOTS_COPPER))
                    .save(pWriter);
        }

        for (PremadeFloppyInfo info : getAllPremadeFloppies()) {
            addPremadeFloppy(pWriter, info);
        }

        shaped(AdvancedComputers.REDSTONE_IO_BLOCK.blockItem().get())
                .pattern("NCN")
                .pattern("CRC")
                .pattern("NDN")
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Blocks.REDSTONE_BLOCK)
                .define('D', AdvancedComputers.DEVICE_CABLE_BLOCK.blockItem().get())
                .unlockedBy("item", has(Tags.Items.INGOTS_COPPER))
                .save(pWriter);

        shaped(AdvancedComputers.ITEM_INTERFACE_BLOCK.blockItem().get())
                .pattern("CRC")
                .pattern("RLR")
                .pattern("CDC")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('L', Tags.Items.LEATHER)
                .define('D', AdvancedComputers.DEVICE_CABLE_BLOCK.blockItem().get())
                .unlockedBy("item", has(Tags.Items.INGOTS_COPPER))
                .save(pWriter);

        shaped(AdvancedComputers.MAINBOARD_PROGRAMMER_BLOCK.blockItem().get())
                .pattern("NCN")
                .pattern("CMC")
                .pattern("NDN")
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('M', AdvancedComputers.MAINBOARD_TIER_2_ITEM.get())
                .define('D', AdvancedComputers.DEVICE_CABLE_BLOCK.blockItem().get())
                .unlockedBy("item", has(Tags.Items.INGOTS_COPPER))
                .save(pWriter);

        shaped(AdvancedComputers.PUNCHCARD_READER_BLOCK.blockItem().get())
                .pattern("PPP")
                .pattern("PAP")
                .pattern("PTP")
                .define('P', planks)
                .define('A', paper)
                .define('T', Items.TORCH)
                .unlockedBy("item", has(Items.TORCH))
                .save(pWriter);

        shaped(AdvancedComputers.PUNCHCARD_MACHINE_BLOCK.blockItem().get())
                .pattern("PLP")
                .pattern("PFP")
                .pattern("PPP")
                .define('P', planks)
                .define('L', Items.LEVER)
                .define('F', Items.FLINT)
                .unlockedBy("item", has(Items.LEVER))
                .save(pWriter);

        shaped(AdvancedComputers.DEBUGGER_ITEM.get())
                .pattern(" R ")
                .pattern("DCN")
                .pattern("III")
                .define('R', Items.REDSTONE_TORCH)
                .define('D', AdvancedComputers.DEVICE_CABLE_BLOCK.blockItem().get())
                .define('N', AdvancedComputers.NETWORK_CABLE_BLOCK.blockItem().get())
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("item", has(Tags.Items.INGOTS_COPPER))
                .save(pWriter);

        shaped(AdvancedComputers.AUTO_CRAFTER_BLOCK.blockItem().get())
                .pattern("ICI")
                .pattern("IGI")
                .pattern("IOI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.CRAFTING_TABLE)
                .define('G', Items.GOLD_BLOCK)
                .define('O', Items.OBSIDIAN)
                .unlockedBy("item", has(Items.OBSIDIAN))
                .save(pWriter);
    }

    public record PremadeFloppyInfo(String folderId, String resultFloppyLabel, ItemLike otherIngredient, int color) {}
    public static PremadeFloppyInfo[] getAllPremadeFloppies() {
        return new PremadeFloppyInfo[] {
            new PremadeFloppyInfo("acos", "AdvancedOS", Items.BOOK, DyeColor.LIME.getFireworkColor())
        };
    }

    @SuppressWarnings("SameParameterValue")
    private void addPremadeFloppy(Consumer<FinishedRecipe> pWriter, PremadeFloppyInfo info) {
        if(!FloppyDiskItem.IsValidPremadeFloppyName(info.folderId))
            throw new RuntimeException("Improperly formatted premade floppy name: '%s'".formatted(info.folderId));

        var floppyDiskitem = AdvancedComputers.FLOPPY_DISK_ITEM.get();
        ShapelessNbtRecipeBuilder.shapeless(RecipeCategory.MISC, floppyDiskitem)
                .addNbtToResult("desiredDiskData", info.folderId)
                .addNbtToResult("color", info.color)
                .addNbtToResult("label", info.resultFloppyLabel)
                .requires(floppyDiskitem)
                .requires(info.otherIngredient)
                .unlockedBy("item",has(floppyDiskitem))
                .save(pWriter,"advancedcomputers:floppy_disk_item_acos");
    }

    private ItemLike getVanillaItem(String name) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse("minecraft:" + name));
    }

    private ShapedRecipeBuilder shaped(ItemLike item, int count) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item, count);
    }

    private ShapedRecipeBuilder shaped(ItemLike item) {
        return shaped(item, 1);
    }
}
