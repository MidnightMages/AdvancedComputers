package dev.asdf00.mc.advcomp.blocks.adapter;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BasePeripheralComponentBlockEntity;
import dev.asdf00.mc.advcomp.lua.components.AcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdapterBlockEntity extends BasePeripheralComponentBlockEntity implements AcBlockEntityComponent {
    public AdapterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.ADAPTER_BE.get(), pPos, pBlockState);
    }

    @Override
    public LuaUserDataComponent createUserdata() {
        return new AdapterBlockUD(this);
    }

    @Override
    public boolean canConnectTo(ClusterType clusterType, Direction side) {
        if (side.equals(this.getBlockState().getValue(AdapterBlock.FACING))) // dont allow connecting on the measurement side
            return false;

        return super.canConnectTo(clusterType, side);
    }


    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (side == null || canConnectTo(AdvancedComputers.CLUSTER_TYPE_DEVICE, side))
            return super.getCapability(cap, side);

        return LazyOptional.empty();
    }
}
