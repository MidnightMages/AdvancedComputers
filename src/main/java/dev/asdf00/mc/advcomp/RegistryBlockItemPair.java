package dev.asdf00.mc.advcomp;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public record RegistryBlockItemPair<T extends Block>(DeferredBlock<T> block, DeferredItem<BlockItem> blockItem) {
}
