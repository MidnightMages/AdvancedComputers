package dev.asdf00.mc.advcomp.blocks.cables.device;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.cables.base.BaseCableBlockEntity;
import dev.asdf00.mc.advcomp.types.cluster.AcClusterType;
import dev.asdf00.mc.advcomp.types.cluster.AcClusterTypeManager;
import dev.asdf00.mc.advcomp.types.cluster.IAcBaseCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DeviceCableBlockEntity extends BaseCableBlockEntity {
    protected DeviceCableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, AdvancedComputers.CLUSTER_TYPE_DEVICE);
    }

    public DeviceCableBlockEntity(BlockPos pos, BlockState state) {
        this(AdvancedComputers.DEV_CABLE_BE.get(), pos, state);
    }
}
