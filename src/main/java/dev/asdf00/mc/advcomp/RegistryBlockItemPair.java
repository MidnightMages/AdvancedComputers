package dev.asdf00.mc.advcomp;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public record RegistryBlockItemPair<T extends Block>(RegistryObject<T> block,
                                                     RegistryObject<BlockItem> blockItem) {
}
