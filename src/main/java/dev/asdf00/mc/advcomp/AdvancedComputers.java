package dev.asdf00.mc.advcomp;

import com.mojang.logging.LogUtils;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockMenu;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockScreen;
import dev.asdf00.mc.advcomp.blocks.screen.Screen;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenMenu;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenScreen;
import dev.asdf00.mc.advcomp.items.StorageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AdvancedComputers.MODID)
public class AdvancedComputers {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "advancedcomputers";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
//    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    public static final RegistryBlockItemPair<Block> EXAMPLE_BLOCK = registerBlockWithItem("example_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));

    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK = registerBlockWithItem("computer_block",
            () -> new ComputerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> SCREEN_BLOCK = registerBlockWithItem("screen_block",
            () -> new Screen(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_BE = BLOCK_ENTITY_TYPES.register("computer_be",
            () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, COMPUTER_BLOCK.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<ScreenEntity>> SCREEN_BE = BLOCK_ENTITY_TYPES.register("screen_be",
            () -> BlockEntityType.Builder.of(ScreenEntity::new, SCREEN_BLOCK.block().get()).build(null));

    public static final RegistryObject<MenuType<ComputerBlockMenu>> COMPUTER_MENU =
            registerMenuType("computer_menu", ComputerBlockMenu::new);

    public static final RegistryObject<MenuType<ScreenMenu>> SCREEN_MENU =
            registerMenuType("screen_menu", ScreenMenu::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, ()-> IForgeMenuType.create(factory));
    }

    private static <T extends Block> RegistryBlockItemPair<T> registerBlockWithItem(String name, Supplier<T> blockBuilder) {
        var rv = BLOCKS.register(name, blockBuilder);
        var i = ModItems.registerBlockItem(name, () -> new BlockItem(rv.get(), new Item.Properties()));
        return new RegistryBlockItemPair<T>(rv, i);
    }

    private static <T extends Item> RegistryObject<T> RegisterItem(String name, Supplier<T> itemBuilder) {
        return ITEMS.register(name, itemBuilder);
    }

    public static final RegistryObject<CreativeModeTab> creativeTab = CREATIVE_MODE_TABS.register("advanced_computers",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group." + MODID + ".tab_name"))
                    .icon(() -> new ItemStack(COMPUTER_BLOCK.blockItem().get()))
                    .displayItems((parameters, output) -> {
                        for (var item : ModItems.getRegisteredItems())
                            output.accept(item.get());
                    })
                    .build()
    );

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));

    public static final RegistryObject<Item> DiskTier1 = RegisterItem("disk_tier1", () -> new StorageItem(Constants.MiB));
    public static final RegistryObject<Item> DiskTier2 = RegisterItem("disk_tier2", () -> new StorageItem(5*Constants.MiB));
    public static final RegistryObject<Item> DiskTier3 = RegisterItem("disk_tier3", () -> new StorageItem(10*Constants.MiB));

    public AdvancedComputers() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
//            ModItems.registerCreativeTabItems(event);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            MenuScreens.register(COMPUTER_MENU.get(), ComputerBlockScreen::new);
            MenuScreens.register(SCREEN_MENU.get(), ScreenScreen::new);
        }
    }
}
