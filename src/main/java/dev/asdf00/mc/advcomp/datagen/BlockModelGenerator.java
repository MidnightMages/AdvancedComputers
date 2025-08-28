package dev.asdf00.mc.advcomp.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import static dev.asdf00.mc.advcomp.datagen.ItemModelGenerator.rl;

public class BlockModelGenerator extends BlockModelProvider {
    public BlockModelGenerator(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        this.orientable("computer_block", rl("block/machine_base"),
                rl("block/computer_block_front"), rl("block/machine_base"));

        for (var type : "wood,diamond,netherite".split(","))
            this.orientable("computer_block_"+type, rl("block/machine_base_"+type),
                   rl("block/computer_block_front_"+type), rl("block/machine_base_"+type));

        this.orientable("screen_block", rl("block/machine_base"),
                rl("block/screen_block_front"), rl("block/machine_base"));

        this.orientable("keycard_reader_block", rl("block/machine_base"),
                rl("block/keycard_reader_block_front"), rl("block/machine_base"));

        this.cubeAll("example_block", rl("block/example_block"));
    }
}
