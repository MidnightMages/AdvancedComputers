package dev.asdf00.mc.advcomp.blocks;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.api.ClusterHostEntity;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.exceptions.ACError;
import dev.asdf00.mc.advcomp.types.cluster.CableConnectableBlockOrEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static dev.asdf00.mc.advcomp.AdvancedComputers.CLUSTER_TYPE_DEVICE;

/**
 * Represents as block entity which can be connected to some AC cable. Not restricted to a specific type though.
 */
public abstract class BaseCableConnectableBlockEntity extends BlockEntity implements CableConnectableBlockOrEntity {
    private final List<ClusterType> supportedClusterTypes;
    public HashMap<Direction, CableCluster> connectedNetworks;

    public BaseCableConnectableBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, List<ClusterType> supportedClusterTypes) {
        super(pType, pPos, pBlockState);
        this.supportedClusterTypes = supportedClusterTypes;
        connectedNetworks = new HashMap<>();
    }

    @Override
    public final HashMap<Direction, CableCluster> getNetworkList() {
        return connectedNetworks;
    }

    @Override
    public boolean canBePartOfCluster(ClusterType networkType) {
        return supportedClusterTypes.contains(networkType);
    }

    /**
     * Currently implemented to allow the same, cluster types on all sides
     *
     * @param clusterType
     * @param side
     * @return
     */
    @Override
    public boolean canConnectTo(ClusterType clusterType, Direction side) {
        return supportedClusterTypes.contains(clusterType);
    }

    @Override
    public boolean actsAsCable(ClusterType clusterType) {
        return clusterType == AdvancedComputers.CLUSTER_TYPE_DEVICE;
    }

    @Override
    public void onNetworkUpdated() {
    }

    /**
     * Tries to grab the computer block entity that is the peripheral network host.
     * Returns the host if exactly one host is found, null otherwise.
     */
    protected ComputerBlockEntity getComputerBlockEntityOrNull() {
        var deviceClusterName = CLUSTER_TYPE_DEVICE.getClusterName();
        ACError.Assert(supportedClusterTypes.stream().anyMatch(x -> x.getClusterName().equals(deviceClusterName)),
                "Block entity seemingly does not support peripheral clusters, thus we cannot grab the computer block entity");

        var allHosts = new HashSet<ClusterHostEntity>();
        for (var net : connectedNetworks.values()) {
            var hostCnt = net.getHostCount();
            if (hostCnt > 1) {
                return null;
            } else if (hostCnt == 1) {
                allHosts.add(net.getHost());
            }
        }

        return allHosts.size() == 1 ? (ComputerBlockEntity) allHosts.iterator().next() : null;
    }
}
