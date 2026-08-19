package dev.asdf00.mc.advcomp;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ModItems {

    private static final ArrayList<Supplier<BlockItem>> registeredBlockItems = new ArrayList<>();
    private static final ArrayList<Supplier<Item>> registeredItems = new ArrayList<>();

    static <T extends BlockItem> Supplier<BlockItem> registerBlockItem(String name, Supplier<BlockItem> itemSupplier) {
        var r = AdvancedComputers.ITEMS.register(name, itemSupplier);
        registeredBlockItems.add(r);
        return r;
    }

    static void registerItem(Supplier<Item> il) {
        registeredItems.add(il);
    }

    public static void registerCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        for (var i : registeredBlockItems)
            event.accept(i.get());

        for (var i : registeredItems)
            event.accept(i.get());
    }

    public static ArrayList<Supplier<BlockItem>> getRegisteredBlockItems() {
        return registeredBlockItems;
    }

    public static ArrayList<Supplier<Item>> getRegisteredItems() {
        return registeredItems;
    }
}
