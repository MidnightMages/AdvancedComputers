package dev.asdf00.mc.advcomp.blocks.cables.types;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public record BpInfo(Block block, BlockEntity blockEntity, boolean isOrActsAsCable) {
}
