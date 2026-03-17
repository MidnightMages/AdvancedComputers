package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.api.ItemCanBeInitialized;
import dev.asdf00.mc.advcomp.types.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.AcPaths;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static dev.asdf00.mc.advcomp.utils.ResourceUtil.loadLuaScript;

public class MainboardItem extends Item implements ItemCanBeInitialized {
    public final static String UEFI_TAG_NAME = "acUefiId";
    public final MainboardTier tier;

    public MainboardItem(MainboardTier tier) {
        super(new Item.Properties());
        this.tier = tier;
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

    public record MainboardInfo(MainboardTier tier, int uefiId) {
    }

    private static int getUefiId(ItemStack is) {
        var tag = is.getTag();
        RuntimeAssert.RuntimeAssert(tag != null, "mainboard had no nbt?");
        RuntimeAssert.RuntimeAssert(tag.contains(UEFI_TAG_NAME), "mainboard had no id nbt tag?");
        return tag.getInt(UEFI_TAG_NAME);
    }

    public MainboardInfo getInfo(ItemStack is) {
        return new MainboardInfo(tier, getUefiId(is));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.advancedcomputers.item.mainboard_%s".formatted(this.tier.toString().toLowerCase())));
        if (this.tier.equals(MainboardTier.T3)) // TPM isnt in yet
            pTooltipComponents.add(Component.translatable("tooltip.advancedcomputers.shared.notimplemented"));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
