package dev.asdf00.mc.advcomp.api;

import dev.asdf00.mc.advcomp.blocks.adapter.AdapterBlockUD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public record AcAdapterContext(AdapterBlockUD adapter, Level lvl, BlockPos pos) {
}
