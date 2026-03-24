package dev.asdf00.mc.advcomp.datagen;

import com.google.gson.JsonObject;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.RegistryBlockItemPair;
import dev.asdf00.mc.advcomp.blocks.cables.model.CableModelLoader;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;

import static dev.asdf00.mc.advcomp.datagen.ItemModelGenerator.rl;
import static dev.asdf00.mc.advcomp.datagen.ItemModelGenerator.rl_mc;

public class BlockStateGenerator extends BlockStateProvider {
    private final ExistingFileHelper exFileHelper;

    public BlockStateGenerator(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, modid, exFileHelper);
        this.exFileHelper = exFileHelper;
    }

    @Override
    protected void registerStatesAndModels() {
        orientedBlock(AdvancedComputers.SCREEN_BLOCK_WOOD);
        orientedBlock(AdvancedComputers.SCREEN_BLOCK);
        orientedBlock(AdvancedComputers.SCREEN_BLOCK_DIAMOND);
        for (var i : new RegistryBlockItemPair<?>[]{
                AdvancedComputers.COMPUTER_BLOCK_WOOD, AdvancedComputers.COMPUTER_BLOCK,
                AdvancedComputers.COMPUTER_BLOCK_DIAMOND, AdvancedComputers.COMPUTER_BLOCK_NETHERITE,
                AdvancedComputers.COMPUTER_BLOCK_CREATIVE
        }) {
            //noinspection unchecked
            orientedBlock((RegistryBlockItemPair<Block>) i, new Property[]{ComputerBlock.RUN_STATE});
        }
        orientedBlock(AdvancedComputers.KEYCARD_READER_BLOCK);
        orientedBlock(AdvancedComputers.MAINBOARD_PROGRAMMER_BLOCK);

        cable(AdvancedComputers.DEVICE_CABLE_BLOCK, "device");
        cable(AdvancedComputers.NETWORK_CABLE_BLOCK, "network");

        simpleModel(AdvancedComputers.WAN_ROUTER_BLOCK);
        orientedModel6(AdvancedComputers.WAN_ROUTER_BLOCK_LOWTIER);
        orientedModel6(AdvancedComputers.NET_ROUTER_BLOCK);
        simpleModel(AdvancedComputers.REDSTONE_IO_BLOCK);

        simpleBlock(AdvancedComputers.ITEM_INTERFACE_BLOCK.block().get());
    }

    private void cable(RegistryBlockItemPair<Block> cableRegDef, String variantName) {
        BlockModelBuilder model = models().getBuilder("block/tcable/" + variantName)
                .parent(models().getExistingFile(rl_mc("cube")))
                .customLoader((builder, existingFileHelper) -> new CableLoaderBuilder(CableModelLoader.GENERATOR_LOADER, builder, existingFileHelper, variantName, false) {
                    @Override
                    public JsonObject toJson(JsonObject json) {
                        return super.toJson(json);
                    }
                })
                .end();
        simpleBlock(cableRegDef.block().get(), model);
    }

    private void orientedBlock(RegistryBlockItemPair<Block> b) {
        orientedBlock(b, new Property[]{});
    }

    private void orientedBlock6(RegistryBlockItemPair<Block> b) {
        orientedBlock46(b, new Property[]{}, true);
    }

    private void orientedBlock(RegistryBlockItemPair<Block> b, Property<?>[] ignoredBlockStateProperties) {
        orientedBlock46(b, ignoredBlockStateProperties, false);
    }

    private String removeModPrefix(Block block) {
        var prefix = "block." + AdvancedComputers.MODID + ".";
        var fullBlockName = block.getDescriptionId();
        if (!fullBlockName.startsWith(prefix))
            throw new RuntimeException("Block name %s did not start with %s".formatted(fullBlockName, prefix));

        return fullBlockName.substring(prefix.length());
    }

    private void orientedBlock46(RegistryBlockItemPair<Block> breg, Property<?>[] ignoredBlockStateProperties, boolean is6Facing) {
        var block = breg.block().get();
        var blockName = removeModPrefix(block);
        getVariantBuilder(block)
                .forAllStatesExcept(state -> {
                            var b = ConfiguredModel.builder()
                                    .modelFile(mf("block/" + blockName));
                            if (is6Facing) {
                                var facing = state.getValue(BlockStateProperties.FACING);
                                b = b.rotationX(facing.getStepX());
                                b = b.rotationX(facing.getStepY());
                            } else {
                                b = b.rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360);
                            }

                            return b.build();
                        },
                        ignoredBlockStateProperties);
    }

    private void orientedModel6(RegistryBlockItemPair<Block> reg) {
        var block = reg.block().get();
        var blockName = removeModPrefix(block);

        var mdl = new ModelFile.UncheckedModelFile(rl("block/" + blockName));
//        var rotModels = new ConfiguredModel[6];
//        for (int i = 0; i < 6; i++) {
//            if (i < 4) { // horiz
//                rotModels[i] = new ConfiguredModel(mdl, i * 90, 0, false, ConfiguredModel.DEFAULT_WEIGHT);
//            } else { // vert for i=4 and i=5
//                rotModels[i] = new ConfiguredModel(mdl, 0, (i - 4) * 180 - 90, false, ConfiguredModel.DEFAULT_WEIGHT);
//            }
//        }

        getVariantBuilder(block).forAllStates(state -> {
            var facing = state.getValue(BlockStateProperties.FACING);
            return ConfiguredModel.builder().modelFile(mdl)
                    .rotationX(facing.getStepY() * 90)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
    }

    private void simpleModel(RegistryBlockItemPair<Block> reg) {
        var block = reg.block().get();
        var blockName = removeModPrefix(block);
        simpleBlock(block, new ModelFile.UncheckedModelFile(rl("block/" + blockName)));
    }

    private ModelFile.ExistingModelFile mf(String s) {
        return new ModelFile.ExistingModelFile(rl(s), this.exFileHelper);
    }

    private static class CableLoaderBuilder extends CustomLoaderBuilder<BlockModelBuilder> {
        private final String cableVariant;
        private final boolean facade;

        public CableLoaderBuilder(ResourceLocation loader, BlockModelBuilder parent, ExistingFileHelper existingFileHelper,
                                  String cableVariant, boolean facade) {
            super(loader, parent, existingFileHelper);
            this.cableVariant = cableVariant;
            this.facade = facade;
        }

        @Override
        public JsonObject toJson(JsonObject json) {
            JsonObject obj = super.toJson(json);
            obj.addProperty("facade", facade);
            obj.addProperty("cableVariant", cableVariant);
            return obj;
        }
    }
}
