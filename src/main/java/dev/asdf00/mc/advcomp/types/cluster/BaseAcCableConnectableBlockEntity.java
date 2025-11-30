package dev.asdf00.mc.advcomp.types.cluster;

import dev.asdf00.mc.advcomp.api.AcBaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.api.AcClusterHostEntity;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.exceptions.ACError;
import dev.asdf00.mc.advcomp.types.AcDevCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static dev.asdf00.mc.advcomp.AdvancedComputers.CLUSTER_TYPE_DEVICE;

public abstract class BaseAcCableConnectableBlockEntity extends BaseAcCableEntityBlock implements AcBaseCableConnectableBlockEntity, AcDevCableConnectableEntity {
    private final List<AcClusterType> supportedClusterTypes;
    public HashMap<Direction, CableCluster> connectedNetworks;

    public BaseAcCableConnectableBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, List<AcClusterType> supportedClusterTypes) {
        super(pType, pPos, pBlockState);
        this.supportedClusterTypes = supportedClusterTypes;
        connectedNetworks = new HashMap<>();
    }

    @Override
    public final HashMap<Direction, CableCluster> getNetworkList() {
        return connectedNetworks;
    }

    @Override
    public boolean canBePartOfCluster(AcClusterType networkType) {
        return supportedClusterTypes.contains(networkType);
    }

    @Override
    public boolean canConnectTo(IAcBaseCableConnectableEntity entity, Direction side) {
        for (var t : supportedClusterTypes) {
            if (entity.canBePartOfCluster(t))
                return true;
        }
        return false;
    }

    @Override
    public boolean actsAsCable() {
        return true;
    }

    @Override
    public void onNetworkUpdated(Direction dir) {
    }

    // Tries to grab the computer block entity that is the peripheral network host.
    // Returns true if exactly one host is found. Host is provided via first argument if return value is true.
    protected boolean TryGetComputerBlockEntity(ComputerBlockEntity[] outCbe) {
        var deviceClusterName = CLUSTER_TYPE_DEVICE.getClusterName();
        ACError.Assert(supportedClusterTypes.stream().anyMatch(x -> x.getClusterName().equals(deviceClusterName)),
                "Block entity seemingly does not support peripheral clusters. Why are we trying to emit an event there?");

        var allHosts = new HashSet<AcClusterHostEntity>();

        for (var net : connectedNetworks.values()) {
            var hostCnt = net.getHostCount();
            if (hostCnt > 1) {
                return false;
            } else if (hostCnt == 1) {
                allHosts.add(net.getHost());
            }
        }

        if (allHosts.size() == 1) {
            outCbe[0] = (ComputerBlockEntity) allHosts.iterator().next();
            return true;
        }

        return false;
    }
}
