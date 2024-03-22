package dev.asdf00.mc.advcomp.computers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPCBlockEntity extends BlockEntity implements MenuProvider {

    public AbstractPCBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
