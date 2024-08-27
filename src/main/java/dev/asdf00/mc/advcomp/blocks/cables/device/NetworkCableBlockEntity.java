package dev.asdf00.mc.advcomp.blocks.cables.device;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.cables.BaseCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkCableBlockEntity extends BaseCableBlockEntity {
    protected NetworkCableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public NetworkCableBlockEntity(BlockPos pos, BlockState state) {
        super(AdvancedComputers.NET_CABLE_BE.get(), pos, state);
    }
}
