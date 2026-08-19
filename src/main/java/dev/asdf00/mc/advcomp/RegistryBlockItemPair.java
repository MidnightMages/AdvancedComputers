package dev.asdf00.mc.advcomp;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public record RegistryBlockItemPair<T extends Block>(Supplier<T> block, Supplier<BlockItem> blockItem) {
}
