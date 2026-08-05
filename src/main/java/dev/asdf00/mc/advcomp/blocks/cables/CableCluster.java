package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.api.ClusterHostEntity;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.types.BaseCableBlock;
import dev.asdf00.mc.advcomp.blocks.cables.types.BpInfo;
import dev.asdf00.mc.advcomp.types.cluster.CableConnectableBlockOrEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import dev.asdf00.mc.advcomp.utils.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.BiConsumer;
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

        for (var cType : clusterTypes.values()) {
            if (blockWasRemoved) {
                for (var dir : Direction.values()) {
                    CableCluster.onBlockPosChangedInternal(level, initialBp.relative(dir), cType);
                }
            } else {
                CableCluster.onBlockPosChangedInternal(level, initialBp, cType);
            }
        }


    }

    private static BpInfo getInfoAboutBp(Level level, BlockPos bp, ClusterType clusterType) {
        var blockEntity = level.getBlockEntity(bp);
        var block = level.getBlockState(bp).getBlock();
        var isOrActsAsCable = block instanceof BaseCableBlock ||
                              (blockEntity instanceof CableConnectableBlockOrEntity && ((CableConnectableBlockOrEntity) blockEntity).actsAsCable(clusterType));
        return new BpInfo(block, blockEntity, isOrActsAsCable);
    }

    public static void onBlockPosChangedInternal(Level level, BlockPos initialBp, ClusterType clusterType) {
        // what this does is:
        // assume the given blockpos initialBp has been changing, meaining one of the following:
        // computer, cable or CableConnectableBlockOrEntity was added, removed or somehow replaced
        // approach:
        //    (not implemented yet) if added: simply rebuild network from current initialBp
        //    otherwise: this network and let the host know that it has changed
        // whenever a network rebuild discovers a AcBaseCableConnectableBlockEntity, the network is read from that block and it is wiped off of any devices on that network
        var blockDirectionsToCheck = new ArrayDeque<Tuple<BlockPos, Direction>>();
        var facesOnBlocksWithThisCluster = new ArrayList<Tuple<BlockPos, Direction>>();

        // if it doesnt act as a cable, placing it may result in up to 6 networks, so we need to rebuild each of them individually
        boolean initialPosActsAsNonCableBlockEntity = level.getBlockEntity(initialBp) instanceof BaseCableConnectableBlockEntity e
                                                      && e.canBePartOfCluster(clusterType)
                                                      && !e.actsAsCable(clusterType);

        for (var initialDir : Direction.values()) {
            assert blockDirectionsToCheck.isEmpty();
            blockDirectionsToCheck.push(new Tuple<>(initialBp, initialDir));

            // build the cluster for this direction
            HashSet<Tuple<BlockPos, Direction>> alreadyCheckedPoses = new HashSet<>();
            HashSet<BlockPos> foundActualCableBlocksOfThisType = new HashSet<>();
            HashSet<BlockPos> foundCableLikeBlocksOfThisType = new HashSet<>();
            HashMap<BaseCableConnectableBlockEntity, BlockPos> foundEntities = new HashMap<>();

            BiConsumer<BlockPos, Direction> checkAddToCluster = (srcBp, dir) -> {
                var destBp = srcBp.relative(dir);
                if (!alreadyCheckedPoses.add(new Tuple<>(srcBp, dir))) return; // skip already checked poses
                if (!level.isLoaded(destBp)) return; // do not check positions that are not loaded as that would forcefully load the chunk
                var destBpInfo = getInfoAboutBp(level, destBp, clusterType);
                var srcBpInfo = getInfoAboutBp(level, srcBp, clusterType);

                // we can traverse if src or dest are acting as cables while also requiring that both allow the connection

                var anyActsAsCable = srcBpInfo.isOrActsAsCable() || destBpInfo.isOrActsAsCable();
                var connectionIsAllowed = srcBpInfo.blockEntity() instanceof CableConnectableBlockOrEntity sourceCableOrBlockEnt && !sourceCableOrBlockEnt.canConnectTo(clusterType, dir) ||
                                          destBpInfo.blockEntity() instanceof CableConnectableBlockOrEntity destCableOrBlockEnt && !destCableOrBlockEnt.canConnectTo(clusterType, dir.getOpposite());

                if (anyActsAsCable && connectionIsAllowed){
                    // we can connect

                    blockDirectionsToCheck.add(new Tuple<>(srcBp, dir));
                }
            };

            checkAddToCluster.accept();

            while (!blockDirectionsToCheck.isEmpty()) {
                var currentBlockPos = blockDirectionsToCheck.remove(); // startpoint for an initial rebuild
                if (!alreadyCheckedPoses.add(currentBlockPos)) continue; // skip already checked poses
                if (!level.isLoaded(currentBlockPos)) continue; // do not check positions that are not loaded as that would forcefully load the chunk

                var block = level.getBlockState(currentBlockPos).getBlock();
                boolean tryAddNeighbors = false;
                if (block instanceof BaseCableBlock cableBlock) { // if this is an actual cable
                    if (!cableBlock.clusterType.equals(clusterType)) { // if wrong type, ignore it
                        continue;
                    }
                    foundActualCableBlocksOfThisType.add(currentBlockPos);
                    foundCableLikeBlocksOfThisType.add(currentBlockPos); // for keeping track of which faces of a block we are connecting to

                    tryAddNeighbors = true;
                }

                if (!tryAddNeighbors) {
                    var currentBlockEntity = level.getBlockEntity(currentBlockPos);
                    if (currentBlockEntity == null) continue; // if there is no tileentity then we can skip this

                    // if this block does not support interacting with cables then we are done
                    if (!(currentBlockEntity instanceof CableConnectableBlockOrEntity baseCableConnectableEntity)) continue;

                    // if this startpoint does not support this cluster type then we are done
                    if (!baseCableConnectableEntity.canBePartOfCluster(clusterType)) continue;

                    foundCableLikeBlocksOfThisType.add(currentBlockPos); // for keeping track of which faces of a block we are connecting to


                    if (baseCableConnectableEntity instanceof BaseCableConnectableBlockEntity cableConnectableBe) {
                        // TODO TODO postpone this via foundEntities
                        //cableConnectableBe.getNetworkList().clear(); // TODO let the block know if a face was cleared and not actually re-discovered and restored
                        foundEntities.put(cableConnectableBe, currentBlockPos);

                        if (cableConnectableBe.actsAsCable(clusterType)) {
                            tryAddNeighbors = true;
                        }
                    }
                }
                if (tryAddNeighbors) {
                    // a connection is made if, source or dest is a cable and all involved block entities want to connect

                    var srcBpInfo = getInfoAboutBp(level, currentBlockPos, clusterType);

                    for (var dir : Direction.values()) {
                        var destBp = currentBlockPos.relative(dir);
                        var destBpInfo = getInfoAboutBp(level, destBp, clusterType);

                        var traverseToThisNewBp = srcBpInfo.isOrActsAsCable() || destBpInfo.isOrActsAsCable();
                        var connectionIsAllowed = srcBpInfo.blockEntity() instanceof CableConnectableBlockOrEntity sourceCableOrBlockEnt && !sourceCableOrBlockEnt.canConnectTo(clusterType, dir) ||
                                                  destBpInfo.blockEntity() instanceof CableConnectableBlockOrEntity destCableOrBlockEnt && !destCableOrBlockEnt.canConnectTo(clusterType, dir.getOpposite());

                        if (!traverseToThisNewBp || !connectionIsAllowed)
                            continue;

                        // at this point we will traverse to it --> clear the old connection on this face

                        if (destBpInfo.blockEntity() instanceof CableConnectableBlockOrEntity destCableOrBlockEnt) {
                            destCableOrBlockEnt.getNetworkList().remove(dir.getOpposite());
                            facesOnBlocksWithThisCluster.add(new Tuple<>(destBp, dir.getOpposite())); // track so we can add the cluster object to it once we are done
                        }
                        blockDirectionsToCheck.push(destBp);
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
                if (foundBlockEntity.actsAsCable(clusterType)) { // if this acts as a cable then we can simply clear all networks
                    netList.clear();
                } // else we already cleared this face connection before

                for (var dir : Direction.values()) {
                    if (foundCableLikeBlocksOfThisType.contains(entityBlockPos.relative(dir))) {
                        netList.put(dir, newCluster);
                    }
                }
                foundBlockEntity.onNetworkUpdated();
            }

            UpdateCableBlockStates(new ArrayList<>(List.of(foundActualCableBlocksOfThisType.toArray(BlockPos[]::new))), newCluster.getHostCount() <= 1, level);
        }
    }

    private static void UpdateCableBlockStates(ArrayList<BlockPos> connectedCables, boolean netIsOk, Level level) {
        for (var c : connectedCables) {
            var bs = level.getBlockState(c).setValue(BaseCableBlock.NETWORK_ERROR, !netIsOk);
            level.setBlock(c, bs, 2); // flags: 2 = sendToClient (NO block update)
        }
    }
}
