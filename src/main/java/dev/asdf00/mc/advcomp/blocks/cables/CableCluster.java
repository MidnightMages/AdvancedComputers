package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.api.ClusterHostEntity;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.types.BaseCableBlock;
import dev.asdf00.mc.advcomp.types.cluster.CableConnectableBlockOrEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Consumer;

public class CableCluster {
    public final BaseCableConnectableBlockEntity[] connectedEntities; // contains all connected devices
    private final ClusterHostEntity[] connectedHostEntities; // contains all connected devices that implement the interface ClusterHostEntity
    public final ClusterType clusterType;

    /**
     * Gets the host of this cluster if this cluster is valid. Otherwise, this method returns {@code null}.
     */
    public ClusterHostEntity getHost() {
        if (connectedHostEntities.length != 1) {
            return null;
        }
        return connectedHostEntities[0];
    }

    public int getHostCount() {
        return connectedHostEntities.length;
    }

    public int getEntityCount() {
        return connectedEntities.length;
    }

    public ClusterType getClusterType() {
        return this.clusterType;
    }

    public BaseCableConnectableBlockEntity getEntity(int index) {
        return connectedEntities[index];
    }

    private CableCluster(BaseCableConnectableBlockEntity[] connectedEntities, ClusterHostEntity[] connectedHostEntities, ClusterType clusterType) {
        Objects.requireNonNull(connectedEntities);
        Objects.requireNonNull(connectedHostEntities);
        this.connectedEntities = connectedEntities;
        this.connectedHostEntities = connectedHostEntities;
        this.clusterType = clusterType;
    }


    public static void onBlockPosChanged(Level level, BlockPos initialBp) {
        var clusterTypes = AdvancedComputers.AC_CLUSTER_TYPE_MANAGER.getNetworkTypes();
        boolean blockWasRemoved = level.getBlockState(initialBp).isAir();

        Consumer<BlockPos> rebuildBp = (BlockPos bp) -> {
            for (var cType : clusterTypes.values()) {
                CableCluster.onBlockPosChangedInternal(level, bp, cType);
            }
        };
        if (blockWasRemoved) {
            for (var dir : Direction.values())
                rebuildBp.accept(initialBp.relative(dir));
        } else {
            rebuildBp.accept(initialBp);
        }
    }

    public static void onBlockPosChangedInternal(Level level, BlockPos initialBp, ClusterType clusterType) {
        // what this does is:
        // assume the given blockpos initialBp has been changing, meaining one of the following:
        // computer, cable or CableConnectableBlockOrEntity was added, removed or somehow replaced
        // approach:
        //    (not implemented yet) if added: simply rebuild network from current initialBp
        //    otherwise: this network and let the host know that it has changed
        // whenever a network rebuild discovers a AcBaseCableConnectableBlockEntity, the network is read from that block and it is wiped off of any devices on that network
        var blockPosesToCheck = new ArrayDeque<BlockPos>();
        Consumer<BlockPos> addNeighbors = (BlockPos bp) -> {
            for (var dir : Direction.values())
                blockPosesToCheck.push(bp.relative(dir));
        };
        blockPosesToCheck.push(initialBp);

        HashSet<BlockPos> alreadyCheckedPoses = new HashSet<>();
        HashSet<BlockPos> foundActualCableBlocksOfThisType = new HashSet<>();
        HashSet<BlockPos> foundCableLikeBlocksOfThisType = new HashSet<>();
        HashMap<BaseCableConnectableBlockEntity, BlockPos> foundEntities = new HashMap<>();
        while (!blockPosesToCheck.isEmpty()) {
            var currentBlockPos = blockPosesToCheck.remove(); // startpoint for an initial rebuild

            if (alreadyCheckedPoses.contains(currentBlockPos))
                continue;
            alreadyCheckedPoses.add(currentBlockPos);

            // do not check positions that are not loaded as that would forcefully load the chunk
            if (!level.isLoaded(currentBlockPos))
                continue;

            var block = level.getBlockState(currentBlockPos).getBlock();
            if (block instanceof BaseCableBlock cableBlock) { // if this is an actual cable
                if (!cableBlock.clusterType.equals(clusterType)) { // if wrong type, ignore it
                    continue;
                }
                foundActualCableBlocksOfThisType.add(currentBlockPos);
                addNeighbors.accept(currentBlockPos);
                foundCableLikeBlocksOfThisType.add(currentBlockPos); // for keeping track of which faces of a block we are connecting to

                continue; // if this is a cable, it cannot be a computer or peripheral, etc.
            }

            var currentBlockEntity = level.getBlockEntity(currentBlockPos);
            if (currentBlockEntity == null) // if there is no tileentity then we can skip this startpoint
                continue;

            // if this block does not support interacting with cables then we are done
            if (!(currentBlockEntity instanceof CableConnectableBlockOrEntity baseCableConnectableEntity))
                continue;

            // if this startpoint does not support this cluster type then we are done
            if (!baseCableConnectableEntity.canBePartOfCluster(clusterType))
                continue;

            foundCableLikeBlocksOfThisType.add(currentBlockPos); // for keeping track of which faces of a block we are connecting to


            if (baseCableConnectableEntity instanceof BaseCableConnectableBlockEntity cableConnectableBe) {
                // TODO TODO postpone this via foundEntities
                //cableConnectableBe.getNetworkList().clear(); // TODO let the block know if a face was cleared and not actually re-discovered and restored
                foundEntities.put(cableConnectableBe, currentBlockPos);

                if (cableConnectableBe.actsAsCable()) {
                    addNeighbors.accept(currentBlockPos);
                }
            }
        }

        var newCluster = new CableCluster(foundEntities.keySet().toArray(BaseCableConnectableBlockEntity[]::new),
                foundEntities.keySet().stream()
                        .filter(x -> x instanceof ClusterHostEntity host && host.isHostForNetwork(clusterType))
                        .map(x -> ((ClusterHostEntity) x)).toArray(ClusterHostEntity[]::new),
                clusterType);

        for (var foundBlockEntity : foundEntities.keySet()) {
            var entityBlockPos = foundEntities.get(foundBlockEntity);
            var netList = foundBlockEntity.getNetworkList();
            if (foundBlockEntity.actsAsCable()) { // if this acts as a cable then we can simply clear all networks
                netList.clear();
            } else { // otherwise we need to clear networks as they disconnect
                throw new IllegalStateException("not implemented yet");
            }

            for (var dir : Direction.values()) {
                if (foundCableLikeBlocksOfThisType.contains(entityBlockPos.relative(dir))) {
                    netList.put(dir, newCluster);
                }
            }
            foundBlockEntity.onNetworkUpdated();
        }

        UpdateCableBlockStates(new ArrayList<>(List.of(foundActualCableBlocksOfThisType.toArray(BlockPos[]::new))), newCluster.getHostCount() <= 1, level);
    }

    private static void UpdateCableBlockStates(ArrayList<BlockPos> connectedCables, boolean netIsOk, Level level) {
        for (var c : connectedCables) {
            var bs = level.getBlockState(c).setValue(BaseCableBlock.NETWORK_ERROR, !netIsOk);
            level.setBlock(c, bs, 2); // flags: 2 = sendToClient (NO block update)
        }
    }
}
