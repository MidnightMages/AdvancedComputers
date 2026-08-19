package dev.asdf00.mc.advcomp;

import com.mojang.logging.LogUtils;
import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.internals.javac.PersistentJavaCompilationCache;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.api.ClusterTypeManager;
import dev.asdf00.mc.advcomp.blocks.adapter.AdapterBlock;
import dev.asdf00.mc.advcomp.blocks.adapter.AdapterBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.DeviceCableBlock;
import dev.asdf00.mc.advcomp.blocks.cables.NetworkCableBlock;
import dev.asdf00.mc.advcomp.blocks.cables.model.CableModelLoader;
import dev.asdf00.mc.advcomp.blocks.computer.*;
import dev.asdf00.mc.advcomp.blocks.item_Interface.ItemInterfaceBlock;
import dev.asdf00.mc.advcomp.blocks.item_Interface.ItemInterfaceBlockEntity;
import dev.asdf00.mc.advcomp.blocks.keycard_reader.KeyCardReaderBlock;
import dev.asdf00.mc.advcomp.blocks.keycard_reader.KeyCardReaderBlockEntity;
import dev.asdf00.mc.advcomp.blocks.mainboard_programmer.MainboardProgrammerBlock;
import dev.asdf00.mc.advcomp.blocks.mainboard_programmer.MainboardProgrammerBlockEntity;
import dev.asdf00.mc.advcomp.blocks.mainboard_programmer.MainboardProgrammerBlockMenu;
import dev.asdf00.mc.advcomp.blocks.mainboard_programmer.MainboardProgrammerBlockScreen;
import dev.asdf00.mc.advcomp.blocks.net_router.NetRouterBlock;
import dev.asdf00.mc.advcomp.blocks.net_router.NetRouterBlockEntity;
import dev.asdf00.mc.advcomp.blocks.punchcard_machine.PunchcardMachineBlock;
import dev.asdf00.mc.advcomp.blocks.punchcard_machine.PunchcardMachineBlockEntity;
import dev.asdf00.mc.advcomp.blocks.punchcard_machine.PunchcardMachineBlockMenu;
import dev.asdf00.mc.advcomp.blocks.punchcard_machine.PunchcardMachineBlockScreen;
import dev.asdf00.mc.advcomp.blocks.punchcard_reader.PunchcardReaderBlock;
import dev.asdf00.mc.advcomp.blocks.punchcard_reader.PunchcardReaderBlockEntity;
import dev.asdf00.mc.advcomp.blocks.punchcard_reader.PunchcardReaderBlockMenu;
import dev.asdf00.mc.advcomp.blocks.punchcard_reader.PunchcardReaderBlockScreen;
import dev.asdf00.mc.advcomp.blocks.redstone_io.RedstoneIoBlock;
import dev.asdf00.mc.advcomp.blocks.redstone_io.RedstoneIoBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.*;
import dev.asdf00.mc.advcomp.blocks.wan_router.*;
import dev.asdf00.mc.advcomp.datagen.*;
import dev.asdf00.mc.advcomp.items.*;
import dev.asdf00.mc.advcomp.types.DualLayerItemColorHandler;
import dev.asdf00.mc.advcomp.types.DyeCustomRecipe;
import dev.asdf00.mc.advcomp.types.GlobalDataStorage;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import dev.asdf00.mc.advcomp.utils.AcPaths;
import dev.asdf00.mc.advcomp.utils.ResourceUtil;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
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
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AdvancedComputers.MODID)
public class AdvancedComputers {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "advancedcomputers";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ClusterTypeManager AC_CLUSTER_TYPE_MANAGER = ClusterTypeManager.getInstance();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MODID);
    private static String MOD_VERSION = "";
    private static String MINECRAFT_VERSION = "";

    public static String getModVersion() {
        return MOD_VERSION;
    }

    public static String getMinecraftVersion() {
        return MINECRAFT_VERSION;
    }

    public static GlobalDataStorage globalDataStorage;

    public static final ClusterType CLUSTER_TYPE_DEVICE = AdvancedComputers.AC_CLUSTER_TYPE_MANAGER.registerNewClusterType("device");
    public static final ClusterType CLUSTER_TYPE_NETWORK = AdvancedComputers.AC_CLUSTER_TYPE_MANAGER.registerNewClusterType("network");

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
//    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK_WOOD = registerBlockWithItem("computer_block_wood",
            () -> new ComputerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS), ComputerTier.Wood));
    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK = registerBlockWithItem("computer_block",
            () -> new ComputerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), ComputerTier.Iron));
    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK_DIAMOND = registerBlockWithItem("computer_block_diamond",
            () -> new ComputerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), ComputerTier.Diamond));
    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK_NETHERITE = registerBlockWithItem("computer_block_netherite",
            () -> new ComputerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), ComputerTier.Netherite));
    public static final RegistryBlockItemPair<Block> COMPUTER_BLOCK_CREATIVE = registerBlockWithItem("computer_block_creative",
            () -> new ComputerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), ComputerTier.Creative));

    public static final RegistryBlockItemPair<Block> SCREEN_BLOCK_WOOD = registerBlockWithItem("screen_block_wood",
            () -> new ScreenBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final RegistryBlockItemPair<Block> SCREEN_BLOCK = registerBlockWithItem("screen_block",
            () -> new ScreenBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final RegistryBlockItemPair<Block> SCREEN_BLOCK_DIAMOND = registerBlockWithItem("screen_block_diamond",
            () -> new ScreenBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> KEYCARD_READER_BLOCK = registerBlockWithItem("keycard_reader_block",
            () -> new KeyCardReaderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> MAINBOARD_PROGRAMMER_BLOCK = registerBlockWithItem("mainboard_programmer_block",
            () -> new MainboardProgrammerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> PUNCHCARD_MACHINE_BLOCK = registerBlockWithItem("punchcard_machine_block",
            () -> new PunchcardMachineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> PUNCHCARD_READER_BLOCK = registerBlockWithItem("punchcard_reader_block",
            () -> new PunchcardReaderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> ITEM_INTERFACE_BLOCK = registerBlockWithItem("item_interface_block",
            () -> new ItemInterfaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> REDSTONE_IO_BLOCK = registerBlockWithItem("redstone_io_block",
            () -> new RedstoneIoBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final RegistryBlockItemPair<Block> DEVICE_CABLE_BLOCK = registerBlockWithItem("device_cable_block",
            () -> new DeviceCableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_WOOL)));

    public static final RegistryBlockItemPair<Block> NETWORK_CABLE_BLOCK = registerBlockWithItem("network_cable_block",
            () -> new NetworkCableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CYAN_WOOL)));

    public static final RegistryBlockItemPair<Block> WAN_ROUTER_BLOCK = registerBlockWithItem("wan_router",
            () -> new WanRouterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryBlockItemPair<Block> WAN_ROUTER_BLOCK_LOWTIER = registerBlockWithItem("wan_router_lowtier",
            () -> new WanRouterBlockLowTier(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryBlockItemPair<Block> NET_ROUTER_BLOCK = registerBlockWithItem("net_router",
            () -> new NetRouterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryBlockItemPair<Block> ADAPTER_BLOCK = registerBlockWithItem("adapter_block",
            () -> new AdapterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerBlockEntity>> COMPUTER_BE = BLOCK_ENTITY_TYPES.register("computer_be",
            () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, COMPUTER_BLOCK.block().get(), COMPUTER_BLOCK_WOOD.block().get(),
                    COMPUTER_BLOCK_DIAMOND.block().get(), COMPUTER_BLOCK_NETHERITE.block().get(),
                    COMPUTER_BLOCK_CREATIVE.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScreenBlockEntity>> SCREEN_BE = BLOCK_ENTITY_TYPES.register("screen_be",
            () -> BlockEntityType.Builder.of(ScreenBlockEntity::new, SCREEN_BLOCK.block().get(), SCREEN_BLOCK_WOOD.block().get(),
                    SCREEN_BLOCK_DIAMOND.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KeyCardReaderBlockEntity>> KEYCARD_READER_BE = BLOCK_ENTITY_TYPES.register("keycard_reader_be",
            () -> BlockEntityType.Builder.of(KeyCardReaderBlockEntity::new, KEYCARD_READER_BLOCK.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainboardProgrammerBlockEntity>> MAINBOARD_PROGRAMMER_BE = BLOCK_ENTITY_TYPES.register("mainboard_programmer_be",
            () -> BlockEntityType.Builder.of(MainboardProgrammerBlockEntity::new, MAINBOARD_PROGRAMMER_BLOCK.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PunchcardMachineBlockEntity>> PUNCHCARD_MACHINE_BE = BLOCK_ENTITY_TYPES.register("punchcard_machine_be",
            () -> BlockEntityType.Builder.of(PunchcardMachineBlockEntity::new, PUNCHCARD_MACHINE_BLOCK.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PunchcardReaderBlockEntity>> PUNCHCARD_READER_BE = BLOCK_ENTITY_TYPES.register("punchcard_reader_be",
            () -> BlockEntityType.Builder.of(PunchcardReaderBlockEntity::new, PUNCHCARD_READER_BLOCK.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemInterfaceBlockEntity>> ITEM_INTERFACE_BE = BLOCK_ENTITY_TYPES.register("item_interface_be",
            () -> BlockEntityType.Builder.of(ItemInterfaceBlockEntity::new, ITEM_INTERFACE_BLOCK.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneIoBlockEntity>> REDSTONE_IO_BE = BLOCK_ENTITY_TYPES.register("redstone_io_be",
            () -> BlockEntityType.Builder.of(RedstoneIoBlockEntity::new, REDSTONE_IO_BLOCK.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WanRouterBlockEntity>> WAN_ROUTER_BE = BLOCK_ENTITY_TYPES.register("wan_router_be",
            () -> BlockEntityType.Builder.of(WanRouterBlockEntity::new, WAN_ROUTER_BLOCK.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WanRouterBlockEntityLowTier>> WAN_ROUTER_BE_LOWTIER = BLOCK_ENTITY_TYPES.register("wan_router_be_lowtier",
            () -> BlockEntityType.Builder.of(WanRouterBlockEntityLowTier::new, WAN_ROUTER_BLOCK_LOWTIER.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetRouterBlockEntity>> NET_ROUTER_BE = BLOCK_ENTITY_TYPES.register("net_router_be",
            () -> BlockEntityType.Builder.of(NetRouterBlockEntity::new, NET_ROUTER_BLOCK.block().get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdapterBlockEntity>> ADAPTER_BE = BLOCK_ENTITY_TYPES.register("adapter_be",
            () -> BlockEntityType.Builder.of(AdapterBlockEntity::new, ADAPTER_BLOCK.block().get()).build(null));

    public static final Supplier<MenuType<ComputerBlockMenu>> COMPUTER_MENU =
            registerMenuType("computer_menu", ComputerBlockMenu::new);

    public static final Supplier<MenuType<MainboardProgrammerBlockMenu>> MAINBOARD_PROGRAMMER_MENU =
            registerMenuType("mainboard_programmer_menu", MainboardProgrammerBlockMenu::new);

    public static final Supplier<MenuType<PunchcardMachineBlockMenu>> PUNCHCARD_MACHINE_MENU =
            registerMenuType("punchcard_machine_menu", PunchcardMachineBlockMenu::new);

    public static final Supplier<MenuType<PunchcardReaderBlockMenu>> PUNCHCARD_READER_MENU =
            registerMenuType("punchcard_reader_menu", PunchcardReaderBlockMenu::new);

    public static final Supplier<MenuType<ScreenMenu>> SCREEN_MENU =
            registerMenuType("screen_menu", ScreenMenu::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DyeCustomRecipe>> DYE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("dye_item", () -> DyeCustomRecipe.serializer);


//    public static final RegistryObject<RecipeType<DyeCustomRecipe>> DYE_RECIPE =
//            RECIPE_TYPES.register("dye_item", DyeCustomRecipe::new);

    private static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS));
    }

    private static <T extends Block> RegistryBlockItemPair<T> registerBlockWithItem(String name, Supplier<T> blockBuilder) {
        Supplier<T> rv = BLOCKS.register(name, blockBuilder);
        Supplier<BlockItem> i = ModItems.registerBlockItem(name, () -> new BlockItem(rv.get(), new Item.Properties()));
        return new RegistryBlockItemPair<>(rv, i);
    }

    private static Supplier<Item> RegisterItem(String name, Supplier<Item> itemBuilder) {
        var ro = ITEMS.register(name, itemBuilder);
        ModItems.registerItem(ro);
        return ro;
    }

    public static final Supplier<Item> HDD_TIER_1_ITEM = RegisterItem("hdd_tier1_item", () -> new DiskItem(500 * Constants.kiB));
    public static final Supplier<Item> HDD_TIER_2_ITEM = RegisterItem("hdd_tier2_item", () -> new DiskItem(2 * Constants.MiB));
    public static final Supplier<Item> HDD_TIER_3_ITEM = RegisterItem("hdd_tier3_item", () -> new DiskItem(4 * Constants.MiB));
    public static final Supplier<Item> HDD_TIER_4_ITEM = RegisterItem("hdd_tier4_item", () -> new DiskItem(16 * Constants.MiB));
    public static final Supplier<Item> HDD_TIER_5_ITEM = RegisterItem("hdd_tier5_item", () -> new DiskItem(64 * Constants.MiB));

    public static final Supplier<Item> KEYCARD_BASIC_ITEM = RegisterItem("keycard_basic_item", () -> new KeycardBasicItem(new Item.Properties()));
    public static final Supplier<Item> KEYCARD_ADVANCED_ITEM = RegisterItem("keycard_advanced_item", () -> new KeycardAdvancedItem(new Item.Properties()));
    public static final Supplier<Item> FLOPPY_DISK_ITEM = RegisterItem("floppy_disk_item", () -> new FloppyDiskItem(new Item.Properties(), 500 * Constants.kiB));

    public static final Supplier<Item> MAINBOARD_TIER_1_ITEM = RegisterItem("mainboard_tier1_item", () -> new MainboardItem(MainboardItem.MainboardTier.T1));
    public static final Supplier<Item> MAINBOARD_TIER_2_ITEM = RegisterItem("mainboard_tier2_item", () -> new MainboardItem(MainboardItem.MainboardTier.T2));
    public static final Supplier<Item> MAINBOARD_TIER_3_ITEM = RegisterItem("mainboard_tier3_item", () -> new MainboardItem(MainboardItem.MainboardTier.T3));
    public static final Supplier<Item> PUNCHCARD_ITEM = RegisterItem("punchcard_item", () -> new PunchcardItem(new Item.Properties()));
    public static final Supplier<Item> DEBUGGER_ITEM = RegisterItem("debugger_item", () -> new DebuggerItem(new Item.Properties()));

    public static final Supplier<CreativeModeTab> creativeTab = CREATIVE_MODE_TABS.register("advanced_computers",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group." + MODID + ".tab_name"))
                    .icon(() -> new ItemStack(COMPUTER_BLOCK.blockItem().get()))
                    .displayItems((parameters, output) -> {
                        for (var item : ModItems.getRegisteredBlockItems())
                            output.accept(item.get());
                        for (var item : ModItems.getRegisteredItems())
                            output.accept(item.get());

                        for (RecipeGenerator.PremadeFloppyInfo info : RecipeGenerator.getAllPremadeFloppies()) {
                            var is = new ItemStack(FLOPPY_DISK_ITEM.get());
                            var nbt = is.getOrCreateTag();
                            nbt.putString("desiredDiskData", info.folderId());
                            nbt.putInt("color", info.color());
                            nbt.putString("label", info.resultFloppyLabel());
                            output.accept(is);
                        }
                    })
                    .build()
    );

    private static MinecraftServer serverReference;

    public AdvancedComputers(IEventBus modEventBus) {
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
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(CableClusterHandler.class);
        NeoForge.EVENT_BUS.register(AudioHandler.class);
        NeoForge.EVENT_BUS.register(ScreenBlockScreen.class);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerDatagen);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register server client communication messages
        NetCodeUtils.registerMessage(ComputerBlockEntity.ClientOriginatingUiEvent.class, ComputerBlockEntity.ClientOriginatingUiEvent::decode);
        NetCodeUtils.registerMessage(AudioHandler.PlaySoundToClientEvent.class, AudioHandler.PlaySoundToClientEvent::decode);
        NetCodeUtils.registerMessage(ScreenBlockEntity.ScreenInputToServerEvent.class, ScreenBlockEntity.ScreenInputToServerEvent::decode);
        NetCodeUtils.registerMessage(ScreenBlockEntity.ScreenContentToClientEvent.class, ScreenBlockEntity.ScreenContentToClientEvent::decode);
        NetCodeUtils.registerMessage(PunchcardMachineBlockEntity.SyncToServerEvent.class, PunchcardMachineBlockEntity.SyncToServerEvent::decode);
        NetCodeUtils.registerMessage(PunchcardMachineBlockEntity.SyncToClientEvent.class, PunchcardMachineBlockEntity.SyncToClientEvent::decode);
        NetCodeUtils.registerMessage(DebuggerItem.ToClientEvent.class, DebuggerItem.ToClientEvent::decode);

        MOD_VERSION = ModList.get().getModContainerById(MODID).orElseThrow().getModInfo().getVersion().toString();
        MINECRAFT_VERSION = ModList.get().getModContainerById("minecraft").orElseThrow().getModInfo().getVersion().toString();

        modEventBus.addListener((RegisterCapabilitiesEvent event) -> { // register caps
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ADAPTER_BE.get(), (o, direction) -> o.getItemHandler());
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
    }

    private void registerDatagen(final GatherDataEvent event) {
        var gen = event.getGenerator();
        var packOut = gen.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        gen.addProvider(event.includeServer(), new RecipeGenerator(packOut));

        gen.addProvider(event.includeClient(), new BlockModelGenerator(packOut, MODID, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(), new ItemModelGenerator(packOut, MODID, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(), new BlockStateGenerator(packOut, MODID, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(), new LootTableProvider(packOut, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(AcLootTableProvider::new, LootContextParamSets.BLOCK))
        ));

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
        serverReference = event.getServer();
        globalDataStorage = GlobalDataStorage.loadOrCreate(serverReference.overworld().getDataStorage());

        AcPaths.createPathsIfNecessary();

        // get rid of the previous cache instance, e.g. if we switch worlds in singleplayer
        if (PersistentJavaCompilationCache.isCacheActive())
            PersistentJavaCompilationCache.deactivateCache();

        if(Config.luaVmCache2Enabled)
            PersistentJavaCompilationCache.enableCache(AcPaths.getCompilationCachePath(), Config.luaVmCache2MaxFiles);

        if (Config.luaVmPrecompileUefiAndOs)
            triggerLuaPrecompilation();
    }

    private void triggerLuaPrecompilation() {
        new Thread(() -> {
            for (var path : "uefi.lua;premade_floppies/acos/boot.lua;premade_floppies/acos/sys/kernel.lua".split(";")) {
                try {
                    LuaVM.load(ResourceUtil.loadLuaScript(path), LuaObject.table());
                    LOGGER.info("Precompilation of '%s' finished.".formatted(path));
                } catch (Exception ignored) {
                    LOGGER.warn("Precompilation of '%s' failed. This is not a problem, though likely regular compilation will fail too.".formatted(path));
                }
            }
        }).start();
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
            event.enqueueWork(() -> {
                MenuScreens.register(COMPUTER_MENU.get(), ComputerBlockScreen::new);
                MenuScreens.register(MAINBOARD_PROGRAMMER_MENU.get(), MainboardProgrammerBlockScreen::new);
                MenuScreens.register(PUNCHCARD_MACHINE_MENU.get(), PunchcardMachineBlockScreen::new);
                MenuScreens.register(PUNCHCARD_READER_MENU.get(), PunchcardReaderBlockScreen::new);
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
            event.registerBlockEntityRenderer(AdvancedComputers.SCREEN_BE.get(), ScreenBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(AdvancedComputers.WAN_ROUTER_BE.get(), WanRouterBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(AdvancedComputers.WAN_ROUTER_BE_LOWTIER.get(), WanRouterBlockEntityRendererLowTier::new);
        }

        @SubscribeEvent
        public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
            event.register(new DualLayerItemColorHandler(), KEYCARD_BASIC_ITEM.get());
            event.register(new DualLayerItemColorHandler(), KEYCARD_ADVANCED_ITEM.get());
            event.register(new DualLayerItemColorHandler(), FLOPPY_DISK_ITEM.get());
        }
    }
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeClientModEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                AudioHandler.onClientTick();
            }
        }
    }
}
