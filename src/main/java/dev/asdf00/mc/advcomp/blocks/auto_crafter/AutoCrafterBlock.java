package dev.asdf00.mc.advcomp.blocks.auto_crafter;

import dev.asdf00.mc.advcomp.blocks.BasePeripheralOrHostBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoCrafterBlock extends BasePeripheralOrHostBlock {

    public AutoCrafterBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(this.stateDefinition.any());
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new AutoCrafterBlockEntity(pPos, pState);
    }


    @Override
    public void onRemove(BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (pState.getBlock() != pNewState.getBlock()) {
            var be = pLevel.getBlockEntity(pPos);
            if (be instanceof AutoCrafterBlockEntity cbe)
                cbe.drops();
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            var be = pLevel.getBlockEntity(pPos);
            if (be instanceof AutoCrafterBlockEntity cbe) {
                NetworkHooks.openScreen((ServerPlayer) pPlayer, cbe, pPos); // will likely break in 1.20.2+
            } else
                throw new IllegalStateException("Tile entity missing?");
        }

        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }
}
