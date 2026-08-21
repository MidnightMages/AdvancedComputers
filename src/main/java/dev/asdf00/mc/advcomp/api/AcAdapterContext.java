package dev.asdf00.mc.advcomp.api;

import dev.asdf00.mc.advcomp.blocks.adapter.AdapterBlockUD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record AcAdapterContext(AdapterBlockUD adapter, Level lvl, BlockPos pos) {
    public  BlockEntity getBlockEntity() {
        return lvl.getBlockEntity(pos);
    }

    public Block getBlock() {
        return lvl.getBlockState(pos).getBlock();
    }

    public BlockState getBlockState() {
        return lvl.getBlockState(pos);
    }
}
