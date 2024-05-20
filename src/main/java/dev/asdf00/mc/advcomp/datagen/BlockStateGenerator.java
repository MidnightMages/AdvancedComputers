package dev.asdf00.mc.advcomp.datagen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.RegistryBlockItemPair;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import static dev.asdf00.mc.advcomp.datagen.ItemModelGenerator.rl;

public class BlockStateGenerator extends BlockStateProvider {
    private final ExistingFileHelper exFileHelper;

    public BlockStateGenerator(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, modid, exFileHelper);
        this.exFileHelper = exFileHelper;
    }

    @Override
    protected void registerStatesAndModels() {
        //ock(AdvancedComputers.SCREEN_BLOCK.block().get(), mf("block/screen_block"));
        orientedBlock(AdvancedComputers.SCREEN_BLOCK);
        orientedBlock(AdvancedComputers.COMPUTER_BLOCK);
        orientedBlock(AdvancedComputers.KEYCARD_READER_BLOCK);
    }

    private void orientedBlock(RegistryBlockItemPair<Block> b) {
        var block = b.block().get();

        var prefix = "block." + AdvancedComputers.MODID + ".";
        var fullBlockName = block.getDescriptionId();
        if (!fullBlockName.startsWith(prefix))
            throw new RuntimeException("Block name %s did not start with %s".formatted(fullBlockName, prefix));

        var blockName = fullBlockName.substring(prefix.length());

        getVariantBuilder(block)
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(mf("block/" + blockName))
                        .rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360)
                        .build()
                );
    }

    private ModelFile.ExistingModelFile mf(String s) {
        return new ModelFile.ExistingModelFile(rl(s), this.exFileHelper);
    }
}
