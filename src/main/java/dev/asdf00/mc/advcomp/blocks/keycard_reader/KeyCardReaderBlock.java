package dev.asdf00.mc.advcomp.blocks.keycard_reader;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.items.BaseKeycardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KeyCardReaderBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    public KeyCardReaderBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new KeyCardReaderBlockEntity(pPos, pState);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (pState.getBlock() != pNewState.getBlock()) {
            var be = pLevel.getBlockEntity(pPos);
//            if (be instanceof ComputerScreenEntity cbe)
//                cbe.drops();
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    public InteractionResult use(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {
        var handItem = pPlayer.getItemInHand(pHand).getItem();
        if (!(handItem instanceof BaseKeycardItem bki)) // fail if player doesnt swipe with a keycard
            return InteractionResult.FAIL;

        if (!pLevel.isClientSide()) {
            if (pLevel.getBlockEntity(pPos) instanceof KeyCardReaderBlockEntity cbe) {
                cbe.onKeycardSwiped(bki);
            }
            else
                throw new IllegalStateException("Tile entity missing?");
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide())
            return null;

        return createTickerHelper(pBlockEntityType, AdvancedComputers.KEYCARD_READER_BE.get(),
                (pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
    }
}
