package dev.asdf00.mc.advcomp.datagen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ItemModelGenerator extends ItemModelProvider {
    public ItemModelGenerator(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        var keycard = AdvancedComputers.KEYCARD_BASIC_ITEM.get();

        this.withExistingParent("keycard_basic_item", "item/handheld")
                .texture("layer0", rl("item/keycard_basic_item_0"))
                .texture("layer1", rl("item/keycard_basic_item_1"));

        this.withExistingParent("example_item", "item/generated")
                .texture("layer0", rl("item/example_item"));

        this.withExistingParent("computer_block", rl("block/computer_block"));
        this.withExistingParent("screen_block", rl("block/screen_block"));
        this.withExistingParent("example_block", rl("block/example_block"));
    }

    static ResourceLocation rl(String s) {
        return new ResourceLocation("advancedcomputers:"+s);
    }
}
