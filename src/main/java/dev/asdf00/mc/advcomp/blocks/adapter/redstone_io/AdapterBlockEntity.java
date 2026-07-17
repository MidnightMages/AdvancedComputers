package dev.asdf00.mc.advcomp.blocks.adapter.redstone_io;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.lua.components.AcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class AdapterBlockEntity extends BaseCableConnectableBlockEntity implements AcBlockEntityComponent {
    public AdapterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.ADAPTER_BE.get(), pPos, pBlockState, Collections.singletonList(AdvancedComputers.CLUSTER_TYPE_DEVICE));
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
}
