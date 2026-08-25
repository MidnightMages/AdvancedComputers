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
        for (var type : "wood,iron,diamond,netherite,creative".split(",")) {
            var blockType = type.equals("iron") ? "" : ("_" + type);
            this.orientable("computer_block" + blockType, rl("block/machine_base_" + type),
                    rl("block/computer_block_front_" + type), rl("block/machine_base_" + type));
        }

        for (var type : "wood,iron,diamond".split(",")) {
            var blockType = type.equals("iron") ? "" : ("_" + type);
            this.orientable("screen_block" + blockType, rl("block/machine_base_" + type),
                    rl("block/screen_block_front_" + type), rl("block/machine_base_" + type));
        }
        this.orientable("screen_block_wood", rl("block/machine_base_wood"),
                rl("block/screen_block_front_wood"), rl("block/machine_base_wood"));

        this.orientable("keycard_reader_block", rl("block/machine_base"),
                rl("block/keycard_reader_block_front"), rl("block/machine_base"));

        this.orientable("mainboard_programmer_block", rl("block/machine_base"),
                rl("block/mainboard_programmer_block_front"), rl("block/machine_base"));

        this.cubeBottomTop("digital_crafter_block", rl("block/digital_crafter_block_side"),
                rl("block/machine_base"), rl("block/digital_crafter_block_top"));

        this.orientable("punchcard_machine_block", rl("block/machine_base_wood"),
                rl("block/punchcard_machine_block_front"), rl("block/machine_base_wood"));

        this.orientable("punchcard_reader_block", rl("block/machine_base_wood"),
                rl("block/punchcard_reader_block_front"), rl("block/machine_base_wood"));

        this.cubeAll("item_interface_block", rl("block/item_interface_block"));

        this.cubeAll("redstone_io_block", rl("block/redstone_io_block"));
    }
}
