package dev.asdf00.mc.advcomp.blocks.wan_router;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WanRouterBlockLowTier extends WanRouterBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

    public WanRouterBlockLowTier(Properties pProperties) {
        super(pProperties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new WanRouterBlockEntityLowTier(pPos, pState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext pContext) {
        var ply = pContext.getPlayer();
        var dir =  Direction.NORTH;
        if(ply != null){
            var delta = ply.getLookAngle();
            dir = Direction.getNearest(delta.x, -delta.y, delta.z);
        }
        return this.defaultBlockState().setValue(FACING, dir.getOpposite());
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public void destroy(LevelAccessor pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState) {
        if (pLevel.getBlockEntity(pPos) instanceof WanRouterBlockEntityLowTier router)
            router.onDestroy();
    }
}
