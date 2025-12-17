package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.api.itemCanBeInitialized;
import dev.asdf00.mc.advcomp.types.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.AcPaths;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;

import static dev.asdf00.mc.advcomp.utils.ResourceUtil.loadLuaScript;

public class MainboardItem extends Item implements itemCanBeInitialized {
    private final static String UEFI_TAG_NAME = "acUefiId";
    private final MainboardTier tier;

    public MainboardItem(MainboardTier tier) {
        super(new Item.Properties());
        this.tier = tier;
    }

    public String readUefiScript(ItemStack is) {
        var tag = is.getTag();
        RuntimeAssert.RuntimeAssert(tag != null, "mainboard had no nbt?");
        RuntimeAssert.RuntimeAssert(tag.contains(UEFI_TAG_NAME), "mainboard had no id nbt tag?");
        var id = tag.getInt(UEFI_TAG_NAME);
        try {
            return Files.readString(AcPaths.getUefiFilePath(id));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void Initialize(ItemStack is) {
        // writes the default uefi onto it, if it is currently empty
        RuntimeAssert.RuntimeAssert(is.getItem() instanceof MainboardItem,
                "Passed item stack was of type: %s".formatted(is));

        var nbt = is.getOrCreateTag();
        if (!nbt.contains(UEFI_TAG_NAME)) {
            var id = AdvancedComputers.globalDataStorage.getNextUefiId();
            nbt.putInt(UEFI_TAG_NAME, id);
            var uefiFilePath = AcPaths.getUefiFilePath(id);
            var defaultUefi = loadLuaScript("uefi.lua");
            try {
                Files.writeString(uefiFilePath, defaultUefi);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public enum MainboardTier {
        T1, // just uefi
        T2, // +nvram
        T3  // +tpm
    }
}
