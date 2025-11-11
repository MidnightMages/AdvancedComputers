package dev.asdf00.mc.advcomp.blocks.cables.base;

import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.AcDevCableConnectableEntity;
import dev.asdf00.mc.advcomp.types.cluster.AcClusterType;
import dev.asdf00.mc.advcomp.types.cluster.BaseAcCableEntityBlock;
import dev.asdf00.mc.advcomp.types.cluster.IAcBaseCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

// a lot of stuff taken from https://www.mcjty.eu/docs/1.20/ep5; Thank you :)
public abstract class BaseCableBlockEntity extends BaseAcCableEntityBlock {

    public static final String ENERGY_TAG = "Energy";

    public static final int MAXTRANSFER = 100;
    public static final int CAPACITY = 1000;

    private final EnergyStorage energy = createEnergyStorage();
    private final LazyOptional<AcDevCableConnectableEntity> lazyCableConnectable = null; // TODO also split caps here
    private final AcClusterType cableType;

    protected BaseCableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, AcClusterType cableType) {
        super(type, pos, state);
        this.cableType = cableType;
    }

    // Cached outputs
    private Set<BlockPos> outputs = null;

    // This function will cache all outputs for this cable network. It will do this
    // by traversing all cables connected to this cable and then check for all energy
    // receivers around those cables.
    private void checkOutputs() {
        if (outputs == null) {
            outputs = new HashSet<>();
            traverse(worldPosition, cable -> {
                // Check for all energy receivers around this position (ignore cables)
                for (Direction direction : Direction.values()) {
                    BlockPos p = cable.getBlockPos().relative(direction);
                    BlockEntity te = level.getBlockEntity(p);
                    if (te != null && !(te instanceof BaseCableBlockEntity)) {
                        te.getCapability(AcCapabilities.CABLE_CONNECTABLE).ifPresent(handler -> {
                            // TODO add network logic
                            outputs.add(p);
//                            if (handler.canReceive()) {
//                                outputs.add(p);
//                            }
                        });
                    }
                }
            });
        }
    }

    public void markDirty() {
        traverse(worldPosition, cable -> cable.outputs = null);
    }

    // This is a generic function that will traverse all cables connected to this cable
    // and call the given consumer for each cable.
    private void traverse(BlockPos pos, Consumer<BaseCableBlockEntity> consumer) {
        Set<BlockPos> traversed = new HashSet<>();
        traversed.add(pos);
        consumer.accept(this);
        traverse(pos, traversed, consumer);
    }

    private void traverse(BlockPos pos, Set<BlockPos> traversed, Consumer<BaseCableBlockEntity> consumer) {
        for (Direction direction : Direction.values()) {
            BlockPos p = pos.relative(direction);
            if (!traversed.contains(p)) {
                traversed.add(p);
                if (level.getBlockEntity(p) instanceof BaseCableBlockEntity cable) {
                    consumer.accept(cable);
                    cable.traverse(p, traversed, consumer);
                }
            }
        }
    }

    public void tickServer() {
        if (energy.getEnergyStored() > 0) {
            // Only do something if we have energy
            checkOutputs();
            if (!outputs.isEmpty()) {
                // Distribute energy over all outputs
                int amount = energy.getEnergyStored() / outputs.size();
                for (BlockPos p : outputs) {
                    BlockEntity te = level.getBlockEntity(p);
                    if (te != null) {
                        te.getCapability(AcCapabilities.CABLE_CONNECTABLE).ifPresent(handler -> {
                            // TODO add network logic
//                            if (handler.canReceive()) {
//                                int received = handler.receiveEnergy(amount, false);
//                                energy.extractEnergy(received, false);
//                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ENERGY_TAG, energy.serializeNBT());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ENERGY_TAG)) {
            energy.deserializeNBT(tag.get(ENERGY_TAG));
        }
    }

    private @NotNull EnergyStorage createEnergyStorage() {
        return new EnergyStorage(CAPACITY, MAXTRANSFER, MAXTRANSFER);
    }


    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == AcCapabilities.CABLE_CONNECTABLE) {
            return lazyCableConnectable.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public boolean canBePartOfCluster(AcClusterType networkType) {
        return networkType.equals(cableType);
    }

    @Override
    public boolean canConnectTo(IAcBaseCableConnectableEntity entity, Direction side) {
        return entity.canBePartOfCluster(cableType);
    }

    @Override
    public boolean actsAsCable() {
        return true;
    }
}

