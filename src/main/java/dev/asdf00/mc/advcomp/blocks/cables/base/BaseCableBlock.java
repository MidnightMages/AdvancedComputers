package dev.asdf00.mc.advcomp.blocks.cables.base;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.cables.ConnectionDir;
import dev.asdf00.mc.advcomp.types.IAcDevCableConnectableEntity;
import dev.asdf00.mc.advcomp.types.cluster.IAcBaseCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// a lot of stuff taken from https://www.mcjty.eu/docs/1.20/ep5; Thank you :)
public abstract class BaseCableBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty NETWORK_ERROR = BooleanProperty.create("networkerror");

    public static final EnumProperty<ConnectionDir> NORTH = EnumProperty.create("north", ConnectionDir.class);
    public static final EnumProperty<ConnectionDir> SOUTH = EnumProperty.create("south", ConnectionDir.class);
    public static final EnumProperty<ConnectionDir> WEST = EnumProperty.create("west", ConnectionDir.class);
    public static final EnumProperty<ConnectionDir> EAST = EnumProperty.create("east", ConnectionDir.class);
    public static final EnumProperty<ConnectionDir> UP = EnumProperty.create("up", ConnectionDir.class);
    public static final EnumProperty<ConnectionDir> DOWN = EnumProperty.create("down", ConnectionDir.class);
//
//    private Property<Boolean> WATERLOGGED;

    private static VoxelShape[] shapeCache = null;
    private static final VoxelShape SHAPE_CABLE_NORTH = Shapes.box(.4, .4, 0, .6, .6, .4);
    private static final VoxelShape SHAPE_CABLE_SOUTH = Shapes.box(.4, .4, .6, .6, .6, 1);
    private static final VoxelShape SHAPE_CABLE_WEST = Shapes.box(0, .4, .4, .4, .6, .6);
    private static final VoxelShape SHAPE_CABLE_EAST = Shapes.box(.6, .4, .4, 1, .6, .6);
    private static final VoxelShape SHAPE_CABLE_UP = Shapes.box(.4, .6, .4, .6, 1, .6);
    private static final VoxelShape SHAPE_CABLE_DOWN = Shapes.box(.4, 0, .4, .6, .4, .6);

    private static final VoxelShape SHAPE_BLOCK_NORTH = Shapes.box(.2, .2, 0, .8, .8, .1);
    private static final VoxelShape SHAPE_BLOCK_SOUTH = Shapes.box(.2, .2, .9, .8, .8, 1);
    private static final VoxelShape SHAPE_BLOCK_WEST = Shapes.box(0, .2, .2, .1, .8, .8);
    private static final VoxelShape SHAPE_BLOCK_EAST = Shapes.box(.9, .2, .2, 1, .8, .8);
    private static final VoxelShape SHAPE_BLOCK_UP = Shapes.box(.2, .9, .2, .8, 1, .8);
    private static final VoxelShape SHAPE_BLOCK_DOWN = Shapes.box(.2, 0, .2, .8, .1, .8);
    private final Capability<IAcDevCableConnectableEntity> cableConnectableCapability; // TODO split caps among derived classes

    private int calculateShapeIndex(ConnectionDir north, ConnectionDir south, ConnectionDir west, ConnectionDir east, ConnectionDir up, ConnectionDir down) {
        int l = ConnectionDir.values().length;
        return ((((south.ordinal() * l + north.ordinal()) * l + west.ordinal()) * l + east.ordinal()) * l + up.ordinal()) * l + down.ordinal();
    }

    private void makeShapes() {
        if (shapeCache == null) {
            int length = ConnectionDir.values().length;
            shapeCache = new VoxelShape[length * length * length * length * length * length];

            for (ConnectionDir up : ConnectionDir.VALUES) {
                for (ConnectionDir down : ConnectionDir.VALUES) {
                    for (ConnectionDir north : ConnectionDir.VALUES) {
                        for (ConnectionDir south : ConnectionDir.VALUES) {
                            for (ConnectionDir east : ConnectionDir.VALUES) {
                                for (ConnectionDir west : ConnectionDir.VALUES) {
                                    int idx = calculateShapeIndex(north, south, west, east, up, down);
                                    shapeCache[idx] = makeShape(north, south, west, east, up, down);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private VoxelShape makeShape(ConnectionDir north, ConnectionDir south, ConnectionDir west, ConnectionDir east, ConnectionDir up, ConnectionDir down) {
        VoxelShape shape = Shapes.box(.4, .4, .4, .6, .6, .6);
        shape = combineShape(shape, north, SHAPE_CABLE_NORTH, SHAPE_BLOCK_NORTH);
        shape = combineShape(shape, south, SHAPE_CABLE_SOUTH, SHAPE_BLOCK_SOUTH);
        shape = combineShape(shape, west, SHAPE_CABLE_WEST, SHAPE_BLOCK_WEST);
        shape = combineShape(shape, east, SHAPE_CABLE_EAST, SHAPE_BLOCK_EAST);
        shape = combineShape(shape, up, SHAPE_CABLE_UP, SHAPE_BLOCK_UP);
        shape = combineShape(shape, down, SHAPE_CABLE_DOWN, SHAPE_BLOCK_DOWN);
        return shape;
    }

    private VoxelShape combineShape(VoxelShape shape, ConnectionDir connectorType, VoxelShape cableShape, VoxelShape blockShape) {
        if (connectorType == ConnectionDir.CABLE) {
            return Shapes.join(shape, cableShape, BooleanOp.OR);
        } else if (connectorType == ConnectionDir.BLOCK) {
            return Shapes.join(shape, Shapes.join(blockShape, cableShape, BooleanOp.OR), BooleanOp.OR);
        } else {
            return shape;
        }
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        ConnectionDir north = getConnectorType(world, pos, Direction.NORTH);
        ConnectionDir south = getConnectorType(world, pos, Direction.SOUTH);
        ConnectionDir west = getConnectorType(world, pos, Direction.WEST);
        ConnectionDir east = getConnectorType(world, pos, Direction.EAST);
        ConnectionDir up = getConnectorType(world, pos, Direction.UP);
        ConnectionDir down = getConnectorType(world, pos, Direction.DOWN);
        int index = calculateShapeIndex(north, south, west, east, up, down);
        return shapeCache[index];
    }


    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighbourState, @NotNull LevelAccessor world, @NotNull BlockPos current, @NotNull BlockPos offset) {
        if (state.getValue(WATERLOGGED)) {
            world.getFluidTicks().schedule(new ScheduledTick<>(Fluids.WATER, current, Fluids.WATER.getTickDelay(world), 0L));   // @todo 1.18 what is this last parameter exactly?
        }
        return calculateState(world, current, state);
    }

    public BaseCableBlock(Capability<IAcDevCableConnectableEntity> cableConnectableCapability, Properties pProperties) {
        super(pProperties);
        this.cableConnectableCapability = cableConnectableCapability;

        makeShapes();
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(NETWORK_ERROR, false));
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        } else {
            return (lvl, pos, st, be) -> {
                if (be instanceof BaseCableBlockEntity cable) {
                    cable.tickServer();
                }
            };
        }
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BaseCableBlockEntity cable) {
            cable.markDirty();
        }
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BaseCableBlockEntity cable) {
            cable.markDirty();
        }
        BlockState blockState = calculateState(level, pos, state);
        if (state != blockState) {
            level.setBlockAndUpdate(pos, blockState);
        }
    }

    // Return the connector type for the given position and facing direction
    private ConnectionDir getConnectorType(BlockGetter world, BlockPos connectorPos, Direction facing) {
        BlockPos pos = connectorPos.relative(facing);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block.getDescriptionId().equals(this.getDescriptionId())) {
            return ConnectionDir.CABLE;
        } else if (!(block instanceof BaseCableBlock) && isConnectable(world, connectorPos, facing)) {
            return ConnectionDir.BLOCK;
        } else {
            return ConnectionDir.NONE;
        }
    }

    // Return true if the block at the given position is connectable to a cable. This is the
    // case if the block supports forge energy
    public boolean isConnectable(BlockGetter world, BlockPos connectorPos, Direction facing) {
        BlockPos pos = connectorPos.relative(facing);
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        BlockEntity te = world.getBlockEntity(pos);
        if (!(te instanceof IAcBaseCableConnectableEntity bcce)) {
            return false;
        }
        var thisTe = world.getBlockEntity(connectorPos);
        if (thisTe instanceof BaseCableBlockEntity bcbe) {
            return bcce.canConnectTo(bcbe, facing.getOpposite());
        }
        AdvancedComputers.LOGGER.warn("BaseCableBlock has no tile entity? %s".formatted(connectorPos));
        return false;
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED, NORTH, SOUTH, EAST, WEST, UP, DOWN, NETWORK_ERROR);
    }


    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return calculateState(world, pos, defaultBlockState())
                .setValue(WATERLOGGED, world.getFluidState(pos).getType() == Fluids.WATER)
                .setValue(NETWORK_ERROR, false);
    }

    private @NotNull BlockState calculateState(LevelAccessor world, BlockPos pos, BlockState state) {
        ConnectionDir north = getConnectorType(world, pos, Direction.NORTH);
        ConnectionDir south = getConnectorType(world, pos, Direction.SOUTH);
        ConnectionDir west = getConnectorType(world, pos, Direction.WEST);
        ConnectionDir east = getConnectorType(world, pos, Direction.EAST);
        ConnectionDir up = getConnectorType(world, pos, Direction.UP);
        ConnectionDir down = getConnectorType(world, pos, Direction.DOWN);

        return state
                .setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(WEST, west)
                .setValue(EAST, east)
                .setValue(UP, up)
                .setValue(DOWN, down);
    }


    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}

