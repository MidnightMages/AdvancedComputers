package dev.asdf00.mc.advcomp.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
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

        basicItem("example");

        basicItem("uefi");
        basicItem("uefi_tpm");
        for (int i = 1; i < 6; i++) {
            basicItem("hdd_tier" + i);
        }

        this.withExistingParent("computer_block", rl("block/computer_block"));
        this.withExistingParent("screen_block", rl("block/screen_block"));
        this.withExistingParent("keycard_reader_block", rl("block/keycard_reader_block"));
        this.withExistingParent("example_block", rl("block/example_block"));
        this.withExistingParent("wan_router", rl("block/wan_router"));
        this.withExistingParent("wan_router_lowtier", rl("block/wan_router_lowtier"));
        this.withExistingParent("net_router", rl("block/net_router"));

        this.getBuilder("device_cable_block").parent(new ModelFile.UncheckedModelFile(rl("block/tcable/device")));
        this.getBuilder("network_cable_block").parent(new ModelFile.UncheckedModelFile(rl("block/tcable/network")));
    }

    private void basicItem(String itemName) {
        this.withExistingParent(itemName + "_item", "item/generated").texture("layer0", rl("item/" + itemName + "_item"));
    }

    static ResourceLocation rl(String s) {
        return new ResourceLocation("advancedcomputers:" + s);
    }

    static ResourceLocation rl_mc(String s) {
        return new ResourceLocation("minecraft:" + s);
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