package dev.asdf00.mc.advcomp;

import com.mojang.logging.LogUtils;
import dev.asdf00.mc.advcomp.api.AcClusterTypeManager;
import dev.asdf00.mc.advcomp.blocks.cables.CableModelLoader;
import dev.asdf00.mc.advcomp.blocks.cables.device.DeviceCableBlock;
import dev.asdf00.mc.advcomp.blocks.cables.device.DeviceCableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.network.NetworkCableBlock;
import dev.asdf00.mc.advcomp.blocks.cables.network.NetworkCableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.computer.*;
import dev.asdf00.mc.advcomp.blocks.keycard_reader.KeyCardReaderBlock;
import dev.asdf00.mc.advcomp.blocks.keycard_reader.KeyCardReaderBlockEntity;
import dev.asdf00.mc.advcomp.blocks.mainboard_programmer.MainboardProgrammerBlock;
import dev.asdf00.mc.advcomp.blocks.mainboard_programmer.MainboardProgrammerBlockEntity;
import dev.asdf00.mc.advcomp.blocks.mainboard_programmer.MainboardProgrammerBlockMenu;
import dev.asdf00.mc.advcomp.blocks.mainboard_programmer.MainboardProgrammerBlockScreen;
import dev.asdf00.mc.advcomp.blocks.net_router.NetRouterBlock;
import dev.asdf00.mc.advcomp.blocks.net_router.NetRouterBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlock;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockScreen;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenMenu;
import dev.asdf00.mc.advcomp.blocks.wan_router.*;
import dev.asdf00.mc.advcomp.datagen.*;
import dev.asdf00.mc.advcomp.items.KeycardAdvancedItem;
import dev.asdf00.mc.advcomp.items.KeycardBasicItem;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.items.StorageItemUD;
import dev.asdf00.mc.advcomp.types.DualLayerItemColorHandler;
import dev.asdf00.mc.advcomp.types.DyeCustomRecipe;
import dev.asdf00.mc.advcomp.types.cluster.AcClusterType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
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

import java.nio.file.Path;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AdvancedComputers.MODID)
public class AdvancedComputers {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "advancedcomputers";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final AcClusterTypeManager AC_CLUSTER_TYPE_MANAGER = AcClusterTypeManager.getInstance();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);


    public static final AcClusterType CLUSTER_TYPE_DEVICE = AdvancedComputers.AC_CLUSTER_TYPE_MANAGER.RegisterNewClusterType("device");
    public static final AcClusterType CLUSTER_TYPE_NETWORK = AdvancedComputers.AC_CLUSTER_TYPE_MANAGER.RegisterNewClusterType("network");

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
//    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    public static final RegistryBlockItemPair<Block> EXAMPLE_BLOCK = registerBlockWithItem("example_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));

    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK_WOOD = registerBlockWithItem("computer_block_wood",
            () -> new ComputerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK), ComputerTier.Wood));
    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK = registerBlockWithItem("computer_block",
            () -> new ComputerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK), ComputerTier.Iron));
    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK_DIAMOND = registerBlockWithItem("computer_block_diamond",
            () -> new ComputerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK), ComputerTier.Diamond));
    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK_NETHERITE = registerBlockWithItem("computer_block_netherite",
            () -> new ComputerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK), ComputerTier.Netherite));

    public static final RegistryBlockItemPair<Block> SCREEN_BLOCK_WOOD = registerBlockWithItem("screen_block_wood",
            () -> new ScreenBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryBlockItemPair<Block> SCREEN_BLOCK = registerBlockWithItem("screen_block",
            () -> new ScreenBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryBlockItemPair<Block> SCREEN_BLOCK_DIAMOND = registerBlockWithItem("screen_block_diamond",
            () -> new ScreenBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> KEYCARD_READER_BLOCK = registerBlockWithItem("keycard_reader_block",
            () -> new KeyCardReaderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> MAINBOARD_PROGRAMMER_BLOCK = registerBlockWithItem("mainboard_programmer_block",
            () -> new MainboardProgrammerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> DEVICE_CABLE_BLOCK = registerBlockWithItem("device_cable_block",
            () -> new DeviceCableBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> NETWORK_CABLE_BLOCK = registerBlockWithItem("network_cable_block",
            () -> new NetworkCableBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> WAN_ROUTER_BLOCK = registerBlockWithItem("wan_router",
            () -> new WanRouterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryBlockItemPair<Block> WAN_ROUTER_BLOCK_LOWTIER = registerBlockWithItem("wan_router_lowtier",
            () -> new WanRouterBlockLowTier(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryBlockItemPair<Block> NET_ROUTER_BLOCK = registerBlockWithItem("net_router",
            () -> new NetRouterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_BE = BLOCK_ENTITY_TYPES.register("computer_be",
            () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, COMPUTER_BLOCK.block().get(), COMPUTER_BLOCK_WOOD.block().get(),
                    COMPUTER_BLOCK_DIAMOND.block().get(), COMPUTER_BLOCK_NETHERITE.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<ScreenBlockEntity>> SCREEN_BE = BLOCK_ENTITY_TYPES.register("screen_be",
            () -> BlockEntityType.Builder.of(ScreenBlockEntity::new, SCREEN_BLOCK.block().get(), SCREEN_BLOCK_WOOD.block().get(),
                    SCREEN_BLOCK_DIAMOND.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<KeyCardReaderBlockEntity>> KEYCARD_READER_BE = BLOCK_ENTITY_TYPES.register("keycard_reader_be",
            () -> BlockEntityType.Builder.of(KeyCardReaderBlockEntity::new, KEYCARD_READER_BLOCK.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<MainboardProgrammerBlockEntity>> MAINBOARD_PROGRAMMER_BE = BLOCK_ENTITY_TYPES.register("mainboard_programmer_be",
            () -> BlockEntityType.Builder.of(MainboardProgrammerBlockEntity::new, MAINBOARD_PROGRAMMER_BLOCK.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<WanRouterBlockEntity>> WAN_ROUTER_BE = BLOCK_ENTITY_TYPES.register("wan_router_be",
            () -> BlockEntityType.Builder.of(WanRouterBlockEntity::new, WAN_ROUTER_BLOCK.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<WanRouterBlockEntityLowTier>> WAN_ROUTER_BE_LOWTIER = BLOCK_ENTITY_TYPES.register("wan_router_be_lowtier",
            () -> BlockEntityType.Builder.of(WanRouterBlockEntityLowTier::new, WAN_ROUTER_BLOCK_LOWTIER.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<NetRouterBlockEntity>> NET_ROUTER_BE = BLOCK_ENTITY_TYPES.register("net_router_be",
            () -> BlockEntityType.Builder.of(NetRouterBlockEntity::new, NET_ROUTER_BLOCK.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<DeviceCableBlockEntity>> DEV_CABLE_BE = BLOCK_ENTITY_TYPES.register("device_cable_be",
            () -> BlockEntityType.Builder.of(DeviceCableBlockEntity::new, DEVICE_CABLE_BLOCK.block().get()).build(null));

    public static final RegistryObject<BlockEntityType<NetworkCableBlockEntity>> NET_CABLE_BE = BLOCK_ENTITY_TYPES.register("network_cable_be",
            () -> BlockEntityType.Builder.of(NetworkCableBlockEntity::new, NETWORK_CABLE_BLOCK.block().get()).build(null));

    public static final RegistryObject<MenuType<ComputerBlockMenu>> COMPUTER_MENU =
            registerMenuType("computer_menu", ComputerBlockMenu::new);

    public static final RegistryObject<MenuType<MainboardProgrammerBlockMenu>> MAINBOARD_PROGRAMMER_MENU =
            registerMenuType("mainboard_programmer_menu", MainboardProgrammerBlockMenu::new);

    public static final RegistryObject<MenuType<ScreenMenu>> SCREEN_MENU =
            registerMenuType("screen_menu", ScreenMenu::new);

    public static final RegistryObject<RecipeSerializer<DyeCustomRecipe>> DYE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("dye_item", () -> DyeCustomRecipe.serializer);

//    public static final RegistryObject<RecipeType<DyeCustomRecipe>> DYE_RECIPE =
//            RECIPE_TYPES.register("dye_item", DyeCustomRecipe::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    private static <T extends Block> RegistryBlockItemPair<T> registerBlockWithItem(String name, Supplier<T> blockBuilder) {
        var rv = BLOCKS.register(name, blockBuilder);
        var i = ModItems.registerBlockItem(name, () -> new BlockItem(rv.get(), new Item.Properties()));
        return new RegistryBlockItemPair<T>(rv, i);
    }

    private static RegistryObject<Item> RegisterItem(String name, Supplier<Item> itemBuilder) {
        var ro = ITEMS.register(name, itemBuilder);
        ModItems.registerItem(ro);
        return ro;
    }

    public static final RegistryObject<CreativeModeTab> creativeTab = CREATIVE_MODE_TABS.register("advanced_computers",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group." + MODID + ".tab_name"))
                    .icon(() -> new ItemStack(COMPUTER_BLOCK.blockItem().get()))
                    .displayItems((parameters, output) -> {
                        for (var item : ModItems.getRegisteredBlockItems())
                            output.accept(item.get());
                        for (var item : ModItems.getRegisteredItems())
                            output.accept(item.get());
                    })
                    .build()
    );

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));

    public static final RegistryObject<Item> HDD_TIER_1_ITEM = RegisterItem("hdd_tier1_item", () -> new StorageItemUD(Constants.MiB));
    public static final RegistryObject<Item> HDD_TIER_2_ITEM = RegisterItem("hdd_tier2_item", () -> new StorageItemUD(5 * Constants.MiB));
    public static final RegistryObject<Item> HDD_TIER_3_ITEM = RegisterItem("hdd_tier3_item", () -> new StorageItemUD(10 * Constants.MiB));
    public static final RegistryObject<Item> HDD_TIER_4_ITEM = RegisterItem("hdd_tier4_item", () -> new StorageItemUD(15 * Constants.MiB));
    public static final RegistryObject<Item> HDD_TIER_5_ITEM = RegisterItem("hdd_tier5_item", () -> new StorageItemUD(20 * Constants.MiB));

    public static final RegistryObject<Item> KEYCARD_BASIC_ITEM = RegisterItem("keycard_basic_item", () -> new KeycardBasicItem(new Item.Properties()));
    public static final RegistryObject<Item> KEYCARD_ADVANCED_ITEM = RegisterItem("keycard_advanced_item", () -> new KeycardAdvancedItem(new Item.Properties()));

    public static final RegistryObject<Item> MAINBOARD_TIER_1_ITEM = RegisterItem("mainboard_tier1_item", () -> new MainboardItem(MainboardItem.MainboardTier.T1));
    public static final RegistryObject<Item> MAINBOARD_TIER_2_ITEM = RegisterItem("mainboard_tier2_item", () -> new MainboardItem(MainboardItem.MainboardTier.T2));
    public static final RegistryObject<Item> MAINBOARD_TIER_3_ITEM = RegisterItem("mainboard_tier3_item", () -> new MainboardItem(MainboardItem.MainboardTier.T3));

    private static MinecraftServer serverReference;

    public AdvancedComputers() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);

        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(CableClusterHandler.class);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerColorHandlers);
        modEventBus.addListener(this::registerDatagen);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register server client communication messages
        NetCodeUtils.registerMessage(ComputerBlockEntity.ClientOriginatingUiEvent.class, ComputerBlockEntity.ClientOriginatingUiEvent::decode);
        NetCodeUtils.registerMessage(ScreenBlockEntity.ScreenOriginatingEvent.class, ScreenBlockEntity.ScreenOriginatingEvent::decode);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    private void registerColorHandlers(final RegisterColorHandlersEvent.Item event) {
        event.register(new DualLayerItemColorHandler(), KEYCARD_BASIC_ITEM.get());
        event.register(new DualLayerItemColorHandler(), KEYCARD_ADVANCED_ITEM.get());
    }

    private void registerDatagen(final GatherDataEvent event) {
        var gen = event.getGenerator();
        var packOut = gen.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        gen.addProvider(event.includeServer(), new RecipeGenerator(packOut));

        gen.addProvider(event.includeClient(), new BlockModelGenerator(packOut, MODID, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(), new ItemModelGenerator(packOut, MODID, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(), new BlockStateGenerator(packOut, MODID, event.getExistingFileHelper()));

        gen.addProvider(event.includeServer(), new BlockTagGenerator(packOut, lookupProvider, MODID, event.getExistingFileHelper()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
//            ModItems.registerCreativeTabItems(event);
    }

    public static Path getAcWorldSaveSubFolder() {
        return serverReference.getWorldPath(LevelResource.ROOT).normalize().toAbsolutePath().resolve("advancedComputers");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        //LOGGER.info("HELLO from server starting");
        serverReference = event.getServer();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        AC_CLUSTER_TYPE_MANAGER.closeRegistration();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            event.enqueueWork(() -> {
                MenuScreens.register(COMPUTER_MENU.get(), ComputerBlockScreen::new);
                MenuScreens.register(MAINBOARD_PROGRAMMER_MENU.get(), MainboardProgrammerBlockScreen::new);
                MenuScreens.register(SCREEN_MENU.get(), ScreenBlockScreen::new);
            });
        }

        @SubscribeEvent
        public static void modelInit(ModelEvent.RegisterGeometryLoaders event) {
            CableModelLoader.register(event);
        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(AdvancedComputers.COMPUTER_BE.get(), ComputerBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(AdvancedComputers.WAN_ROUTER_BE.get(), WanRouterBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(AdvancedComputers.WAN_ROUTER_BE_LOWTIER.get(), WanRouterBlockEntityRendererLowTier::new);
        }
    }
}
