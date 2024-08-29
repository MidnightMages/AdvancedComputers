package dev.asdf00.mc.advcomp.blocks.cables.device;

import dev.asdf00.mc.advcomp.blocks.cables.base.BaseCableBlock;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeviceCableBlock extends BaseCableBlock {
    public DeviceCableBlock(Properties pProperties) {
        super(AcCapabilities.CABLE_CONNECTABLE , pProperties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new DeviceCableBlockEntity(blockPos, blockState);
    }
}
