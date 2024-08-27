package dev.asdf00.mc.advcomp.blocks.cables.device;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.cables.BaseCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DeviceCableBlockEntity extends BaseCableBlockEntity {
    protected DeviceCableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DeviceCableBlockEntity(BlockPos pos, BlockState state) {
        super(AdvancedComputers.DEV_CABLE_BE.get(), pos, state);
    }
}
