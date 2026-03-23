package dev.asdf00.mc.advcomp.blocks.item_Interface;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.components.AcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.AcDevCableConnectableEntity;
import dev.asdf00.mc.advcomp.types.cluster.BaseAcCableConnectableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.common.capabilities.Capability;
import net.neoforged.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ItemInterfaceBlockEntity extends BaseAcCableConnectableBlockEntity implements AcBlockEntityComponent {
    private final LazyOptional<AcDevCableConnectableEntity> lazyCableConnectable;

    ConcurrentLinkedQueue<Runnable> tickThreadQueue = new ConcurrentLinkedQueue<>();
    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        while (true) {
            var newItem = tickThreadQueue.poll();
            if (newItem == null)
                return;
            newItem.run();
        }
    }

    public ItemInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.ITEM_INTERFACE_BE.get(), pPos, pBlockState, List.of(AdvancedComputers.CLUSTER_TYPE_DEVICE));
        this.lazyCableConnectable = LazyOptional.of(() -> this);
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


    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public LuaUserDataComponent CreateUserdata() {
        return new ItemInterfaceBlockEntityUD(this);
    }
}
