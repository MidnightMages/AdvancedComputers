package dev.asdf00.mc.advcomp.blocks.wan_router;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.items.BaseKeycardItem;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.IAcDevCableConnectableEntity;
import dev.asdf00.mc.advcomp.types.cluster.BaseAcCableConnectableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class WanRouterBlockEntity extends BaseAcCableConnectableBlockEntity {
    private final LazyOptional<IAcDevCableConnectableEntity> lazyCableConnectable;

    public WanRouterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.WAN_ROUTER_BE.get(), pPos, pBlockState, Collections.singletonList(AdvancedComputers.CLUSTER_TYPE_NETWORK));

        this.lazyCableConnectable = LazyOptional.of(() -> this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == AcCapabilities.CABLE_CONNECTABLE)
            return lazyCableConnectable.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyCableConnectable.invalidate();
    }

    public void tick(Level pLevel1, BlockPos pPos, BlockState pState1) {

    }
}
