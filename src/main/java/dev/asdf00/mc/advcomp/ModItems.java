package dev.asdf00.mc.advcomp;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ModItems {

    private static final ArrayList<RegistryObject<BlockItem>> registeredItems = new ArrayList<>();

    static <T extends BlockItem> RegistryObject<BlockItem> registerBlockItem(String name, Supplier<BlockItem> itemSupplier) {
        var r = AdvancedComputers.ITEMS.register(name, itemSupplier);
        registeredItems.add(r);
        return r;
    }

    public static void registerCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        for (var i : registeredItems){
            event.accept(i);
        }
    }

    public static  ArrayList<RegistryObject<BlockItem>> getRegisteredItems() {
        return registeredItems;
    }
}
