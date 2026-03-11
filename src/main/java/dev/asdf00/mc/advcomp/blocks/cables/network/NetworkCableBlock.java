package dev.asdf00.mc.advcomp.blocks.cables.network;

import dev.asdf00.mc.advcomp.blocks.cables.base.BaseCableBlock;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NetworkCableBlock extends BaseCableBlock {
    public NetworkCableBlock(Properties pProperties) {
        super(AcCapabilities.CABLE_CONNECTABLE, pProperties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new NetworkCableBlockEntity(blockPos, blockState);
    }
}
