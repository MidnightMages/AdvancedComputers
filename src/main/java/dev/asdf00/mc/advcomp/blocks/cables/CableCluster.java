package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.api.ClusterHostEntity;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.types.BaseCableBlock;
import dev.asdf00.mc.advcomp.blocks.cables.types.BpInfo;
import dev.asdf00.mc.advcomp.types.cluster.CableConnectableBlockOrEntity;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

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
            var manyClusters = blockWasRemoved;
            if (!blockWasRemoved) {
                boolean initialBlockIsEntityAndNotActsAsCableThusManyClusters = level.getBlockEntity(initialBp) instanceof BaseCableConnectableBlockEntity e
                                                                                && e.canBePartOfCluster(cType)
                                                                                && !e.actsAsCable(cType); // in this case
                manyClusters = initialBlockIsEntityAndNotActsAsCableThusManyClusters;
            }

            if (blockWasRemoved) { // clear any leftover networks on the adjacent blocks
                for (var dir : Direction.values()) {
                    if (level.getBlockEntity(initialBp.relative(dir)) instanceof CableConnectableBlockOrEntity be) {
                        be.getNetworkList().remove(dir.getOpposite());
                    }
                }
            }

            if (manyClusters) {
                for (var dir : Direction.values()) {
                    CableCluster.buildSingleNet(level, initialBp.relative(dir), cType, null);
                }
            } else {
                CableCluster.buildSingleNet(level, initialBp, cType, null);
            }
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

        RuntimeAssert.RuntimeAssert(clusterType.equals(AdvancedComputers.CLUSTER_TYPE_DEVICE), "not supported for non-device net");
        buildSingleNet(level, initialBp, clusterType, null);
    }

    private static BpInfo getInfoAboutBp(Level level, BlockPos bp, ClusterType clusterType) {
        var blockEntity = level.getBlockEntity(bp);
        var block = level.getBlockState(bp).getBlock();
        var isOrActsAsCable = (block instanceof BaseCableBlock bcb && bcb.clusterType.equals(clusterType)) ||
                              (blockEntity instanceof CableConnectableBlockOrEntity ccbe && ccbe.actsAsCable(clusterType));

        boolean supportsCluster = (block instanceof BaseCableBlock bcb && bcb.clusterType.equals(clusterType)) ||
                                  (blockEntity instanceof CableConnectableBlockOrEntity ccbe && ccbe.canBePartOfCluster(clusterType));

        return new BpInfo(block, blockEntity, isOrActsAsCable, supportsCluster);
    }

    // builds and assigns a single network
    private static void buildSingleNet(Level level, BlockPos initialBp, ClusterType clusterType, Direction dir) {
        if (level.getBlockState(initialBp).isAir()) return; // nothing to do in this case

        // build a single network starting from either this block or from this block face if dir is nonnull
        var facesToTraverse = new ArrayDeque<Tuple<BlockPos, Direction>>();
        if (dir != null) {
            facesToTraverse.add(new Tuple<>(initialBp, dir));
        } else {
            for (var dir2 : Direction.values()) {
                facesToTraverse.add(new Tuple<>(initialBp, dir2));
            }
        }

        var facesToAssignClusterTo = new ArrayDeque<Tuple<BlockEntity, Direction>>();
        var alreadyTraversedFaces = new HashSet<Tuple<BlockPos, Direction>>();
        var clusterBlockEntities = new ArrayList<BaseCableConnectableBlockEntity>();
        var clusterHostBlockEntities = new ArrayList<ClusterHostEntity>();
        var foundActualCableBlocksOfThisType = new ArrayList<BlockPos>();
        var alreadyTrackedBlockEntities = new HashSet<BlockPos>();
        var passthroughBlocklist = new HashSet<BlockPos>();
        while (!facesToTraverse.isEmpty()) {
            var currentFaceToTraverse = facesToTraverse.remove();
            var srcBpos = currentFaceToTraverse.x();
            var dir3 = currentFaceToTraverse.y();

            if (!alreadyTraversedFaces.add(currentFaceToTraverse)) continue; // skip already checked poses
            var destBpos = srcBpos.relative(dir3);
            if (!level.isLoaded(destBpos)) continue; // do not check positions that are not loaded as that would forcefully load the chunk

            var srcBpInfo = getInfoAboutBp(level, srcBpos, clusterType);
            if (!srcBpInfo.supportsCurrentCluster()) continue;
            var destBpInfo = getInfoAboutBp(level, destBpos, clusterType);

            if (alreadyTrackedBlockEntities.add(srcBpos)) { // if curr srcBp is not yet tracked, track it
                if (srcBpInfo.blockEntity() instanceof BaseCableConnectableBlockEntity ent)
                    clusterBlockEntities.add(ent);

                if (srcBpInfo.blockEntity() instanceof ClusterHostEntity cEnt && cEnt.isHostForNetwork(clusterType))
                    clusterHostBlockEntities.add(cEnt);

                if (srcBpInfo.block() instanceof BaseCableBlock)
                    foundActualCableBlocksOfThisType.add(srcBpos);
            }

            if (passthroughBlocklist.contains(srcBpos))
                continue;

            var anyActsAsCable = srcBpInfo.isOrActsAsCable() || destBpInfo.isOrActsAsCable();
            var connectionIsAllowed = (
                                              srcBpInfo.isOrActsAsCable() ||
                                              srcBpInfo.blockEntity() instanceof CableConnectableBlockOrEntity sourceCableOrBlockEnt &&
                                              sourceCableOrBlockEnt.canConnectTo(clusterType, dir3)
                                      ) && (
                                              destBpInfo.isOrActsAsCable() ||
                                              destBpInfo.blockEntity() instanceof CableConnectableBlockOrEntity destCableOrBlockEnt &&
                                              destCableOrBlockEnt.canConnectTo(clusterType, dir3.getOpposite())
                                      );

            if (anyActsAsCable && connectionIsAllowed) {
                // connection is good, set cluster of both faces
                if (srcBpInfo.blockEntity() != null)
                    facesToAssignClusterTo.add(new Tuple<>(srcBpInfo.blockEntity(), dir3));
                if (destBpInfo.blockEntity() != null)
                    facesToAssignClusterTo.add(new Tuple<>(destBpInfo.blockEntity(), dir3.getOpposite()));

                if (srcBpInfo.isOrActsAsCable())
                    for (var dir4 : Direction.values()) {
                        if (dir4 != dir3.getOpposite())
                            facesToTraverse.add(new Tuple<>(destBpos, dir4));
                    }
                if (!destBpInfo.isOrActsAsCable()) // prevent going through blocks like netrouters
                    passthroughBlocklist.add(destBpos);
            }
        }


        var cluster = new CableCluster(clusterBlockEntities.toArray(BaseCableConnectableBlockEntity[]::new),
                clusterHostBlockEntities.toArray(ClusterHostEntity[]::new), clusterType);


        HashSet<CableConnectableBlockOrEntity> updatesToEmit = new HashSet<>();
        // technically clearing shouldnt be necessary, but lets do it just in case
        for (var tpl : facesToAssignClusterTo) {
            if (tpl.x() instanceof CableConnectableBlockOrEntity ent) {
                ent.getNetworkList().put(tpl.y(), cluster);
                updatesToEmit.add(ent);
            } else {
                throw new IllegalStateException("A discovered entity was not of type CableConnectableBlockOrEntity.");
            }
        }
        for (var ent : updatesToEmit) ent.onNetworkUpdated();
        updateCableBlockStates(foundActualCableBlocksOfThisType, cluster.getHostCount() <= 1, level);
    }

    private static void updateCableBlockStates(ArrayList<BlockPos> connectedCables, boolean netIsOk, Level level) {
        for (var c : connectedCables) {
            var bs = level.getBlockState(c).setValue(BaseCableBlock.NETWORK_ERROR, !netIsOk);
            level.setBlock(c, bs, 2); // flags: 2 = sendToClient (NO block update)
        }
    }
}
