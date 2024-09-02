package dev.asdf00.mc.advcomp.blocks.computer;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ComputerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final EnumProperty<ComputerRunState> RUN_STATE = EnumProperty.create("runstate", ComputerRunState.class);

    public ComputerBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        registerDefaultState(defaultBlockState().setValue(RUN_STATE, ComputerRunState.STOPPED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, RUN_STATE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ComputerBlockEntity(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState()
                .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
                .setValue(RUN_STATE, ComputerRunState.STOPPED);
    }


    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (pState.getBlock() != pNewState.getBlock()) {
            var be = pLevel.getBlockEntity(pPos);
            if (be instanceof ComputerBlockEntity cbe)
                cbe.drops();
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            var be = pLevel.getBlockEntity(pPos);
            if (be instanceof ComputerBlockEntity cbe) {
                if (pPlayer.isShiftKeyDown())
                    cbe.toggleLVMPowerState();
                else
                    NetworkHooks.openScreen((ServerPlayer) pPlayer, cbe, pPos); // will likely break in 1.20.2+
            } else
                throw new IllegalStateException("Tile entity missing?");
        }

        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide())
            return null;

        return createTickerHelper(pBlockEntityType, AdvancedComputers.COMPUTER_BE.get(),
                (pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
    }

    @Override
    public void destroy(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        var upPos = pPos.relative(Direction.UP);
        var upState = pLevel.getBlockState(upPos);
        if (upState.getBlock() instanceof ScreenBlock screenBlock) {
            screenBlock.destroy(pLevel, upPos, upState);
        }
        super.destroy(pLevel, pPos, pState);
    }

    public enum ComputerRunState implements StringRepresentable {
        STOPPED (new Color(0xFF0000), false, false),
        CRASHED (new Color(0xFF0000), true, false),
        RUNNING (new Color(0x00FF22), false, true),
        WORKING (new Color(0x00FF22), true, true);

        final Color color;
        final boolean blinking;
        final boolean isRunning;

        ComputerRunState(Color color, boolean blinking, boolean isRunning) {

            this.color = color;
            this.blinking = blinking;
            this.isRunning = isRunning;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.toString().toLowerCase();
        }

        public boolean isRunning() {
            return this.isRunning;
        }
    }
}
