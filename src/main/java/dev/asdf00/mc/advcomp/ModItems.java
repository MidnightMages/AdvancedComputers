package dev.asdf00.mc.advcomp;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ModItems {

    private static final ArrayList<RegistryObject<BlockItem>> registeredBlockItems = new ArrayList<>();
    private static final ArrayList<RegistryObject<Item>> registeredItems = new ArrayList<>();

    static <T extends BlockItem> RegistryObject<BlockItem> registerBlockItem(String name, Supplier<BlockItem> itemSupplier) {
        var r = AdvancedComputers.ITEMS.register(name, itemSupplier);
        registeredBlockItems.add(r);
        return r;
    }

    static void registerItem(RegistryObject<Item> il) {
        registeredItems.add(il);
    }

    public static void registerCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        for (var i : registeredBlockItems)
            event.accept(i);

        for (var i : registeredItems)
            event.accept(i);
    }

    public static ArrayList<RegistryObject<BlockItem>> getRegisteredBlockItems() {
        return registeredBlockItems;
    }

    public static ArrayList<RegistryObject<Item>> getRegisteredItems() {
        return registeredItems;
    }
}
