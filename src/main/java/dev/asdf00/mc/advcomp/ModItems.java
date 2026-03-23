package dev.asdf00.mc.advcomp;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.registries.RegistryObject;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ModItems {

    private static final ArrayList<DeferredItem<BlockItem>> registeredBlockItems = new ArrayList<>();
    private static final ArrayList<DeferredItem<Item>> registeredItems = new ArrayList<>();

    static DeferredItem<BlockItem> registerBlockItem(String name, Supplier<BlockItem> itemSupplier) {
        var r = AdvancedComputers.ITEMS.register(name, itemSupplier);
        registeredBlockItems.add(r);
        return r;
    }

    static void registerItem(DeferredItem<Item>  il) {
        registeredItems.add(il);
    }

    public static ArrayList<DeferredHolder<Item, BlockItem>> getRegisteredBlockItems() {
        return registeredBlockItems;
    }

    public static ArrayList<DeferredHolder<Item, Item>> getRegisteredItems() {
        return registeredItems;
    }
}
