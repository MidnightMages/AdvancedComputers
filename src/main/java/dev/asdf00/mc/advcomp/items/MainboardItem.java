package dev.asdf00.mc.advcomp.items;

import net.minecraft.world.item.Item;

public class MainboardItem extends Item {
    private final MainboardTier tier;

    public MainboardItem(MainboardTier tier) {
        super(new Item.Properties());
        this.tier = tier;
    }

    public enum MainboardTier {
        T1, // just uefi
        T2, // +nvram
        T3  // +tpm
    }
}
