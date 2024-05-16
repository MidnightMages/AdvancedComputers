package dev.asdf00.mc.advcomp.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ItemModelGenerator extends ItemModelProvider {
    public ItemModelGenerator(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        scaleThirdPerson(this.withExistingParent("keycard_basic_item", "item/handheld"))
//                .transforms()
//                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0,90,0).end()
//                .end()
                .texture("layer0", rl("item/keycard_basic_item_0"))
                .texture("layer1", rl("item/keycard_basic_item_1"));

        scaleThirdPerson(this.withExistingParent("keycard_advanced_item", "item/handheld"))
//                .transforms()
//                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0,90,90).end()
//                .end()
                .texture("layer0", rl("item/keycard_advanced_item_0"))
                .texture("layer1", rl("item/keycard_advanced_item_1"));

        this.withExistingParent("example_item", "item/generated")
                .texture("layer0", rl("item/example_item"));

        this.withExistingParent("computer_block", rl("block/computer_block"));
        this.withExistingParent("screen_block", rl("block/screen_block"));
        this.withExistingParent("example_block", rl("block/example_block"));
    }

    static ResourceLocation rl(String s) {
        return new ResourceLocation("advancedcomputers:" + s);
    }

    static ItemModelBuilder scaleThirdPerson(ItemModelBuilder b) {
        final float scale = 0.5f;
        var b2 = b.transforms();
        for (var idc : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND})
            b2 = b2.transform(idc).scale(scale, scale, scale).rotation(180, -90, -90).end();

        return b2.end();
    }
}