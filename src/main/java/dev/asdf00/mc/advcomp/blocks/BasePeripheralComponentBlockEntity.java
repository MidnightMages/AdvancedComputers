package dev.asdf00.mc.advcomp.blocks;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.components.AcBlockEntityComponent;
import dev.asdf00.mc.advcomp.types.capabilities.Capabilities;
import dev.asdf00.mc.advcomp.types.capabilities.DeviceCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for block entities that can be connected via a peripheral cable and are represented by a userdata component
 */
public abstract class BasePeripheralComponentBlockEntity extends BaseCableConnectableBlockEntity implements AcBlockEntityComponent {
    private final LazyOptional<DeviceCableConnectableEntity> lazyCableConnectable;


    public <T extends BlockEntity> BasePeripheralComponentBlockEntity(BlockEntityType<T> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, List.of(AdvancedComputers.CLUSTER_TYPE_DEVICE));
        this.lazyCableConnectable = LazyOptional.of(() -> this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == Capabilities.CABLE_CONNECTABLE)
            return lazyCableConnectable.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyCableConnectable.invalidate();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }
}
