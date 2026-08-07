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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CableCluster {
    public final BaseCableConnectableBlockEntity[] connectedEntities; // contains all connected devices
    private final ClusterHostEntity[] connectedHostEntities; // contains all connected devices that implement the interface ClusterHostEntity
    public final ClusterType clusterType;
    private final int debugId;
    private static final AtomicInteger next_debugId = new AtomicInteger(0);

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
        debugId = next_debugId.getAndIncrement();
    }


    public static void onBlockPosChanged(Level level, BlockPos initialBp) {
        var clusterTypes = AdvancedComputers.AC_CLUSTER_TYPE_MANAGER.getNetworkTypes();
        boolean blockWasRemoved = level.getBlockState(initialBp).isAir();

        for (var cType : clusterTypes.values()) {
//            var manyClusters = blockWasRemoved;
//            if (!blockWasRemoved) {
//                boolean initialBlockIsEntityAndNotActsAsCableThusManyClusters = level.getBlockEntity(initialBp) instanceof BaseCableConnectableBlockEntity e
//                                                                                && e.canBePartOfCluster(cType)
//                                                                                && !e.actsAsCable(cType); // in this case
//                manyClusters = initialBlockIsEntityAndNotActsAsCableThusManyClusters;
//            }
//
//            if (blockWasRemoved) { // clear any leftover networks on the adjacent blocks
//                for (var dir : Direction.values()) {
//                    if (level.getBlockEntity(initialBp.relative(dir)) instanceof CableConnectableBlockOrEntity be) {
//                        be.getNetworkList().remove(dir.getOpposite());
//                    }
//                }
//            }

//            if (blockWasRemoved) {
            CableCluster.buildSingleNet2(level, initialBp, cType);
                for (var dir : Direction.values()) {
                    CableCluster.buildSingleNet2(level, initialBp.relative(dir), cType);
                }
//            }
//            if (manyClusters) {
//                for (var dir2 : Direction.values()) {
//                    CableCluster.buildSingleNet(level, initialBp, cType, dir2);
//                }
//            } else {
//                CableCluster.buildSingleNet(level, initialBp, cType, null);
//            }
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
        buildSingleNet2(level, initialBp, clusterType);
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

    private static void forAllDirs(Consumer<Direction> x) {
        for (var dir : Direction.values())
            x.accept(dir);
    }

    // builds and assigns a single network
    // Algorithm:
    // rebuild from blockpos:
    //	switch startposType:
    //		case cable, blockEntityActsAsCable: --> single network, keep traversing until find a block that doesnt act as cable,
    //						and assign to all crossed faces and to the face of the blocks that we didnt enter
    //		case blockEntityNotCable:
    //					--> 6 networks, start out in every direction. If block is cable then same as cable, otherwise create a net between just those 2 blocks
    private static void buildSingleNet2(Level level, BlockPos initialBp, ClusterType clusterType) {
        var initialInfo = getInfoAboutBp(level, initialBp, clusterType);
        if (!initialInfo.supportsCurrentCluster())
            return;

        if (initialInfo.isOrActsAsCable()) {
            ArrayList<Tuple<CableConnectableBlockOrEntity, Direction>> facesToAssignCurrentNetTo = new ArrayList<>();
            ArrayList<Tuple<CableConnectableBlockOrEntity, Direction>> facesThatAreNotConnectedToCurrentClusterType = new ArrayList<>();
            var clusterBaseCableConnectableEnts = new HashSet<BaseCableConnectableBlockEntity>();
            var clusterHostBlockEntities = new HashSet<ClusterHostEntity>();
            var foundActualCableBlocksOfThisType = new HashSet<BlockPos>();
            // the initial block is either a cable or of type CableConnectableBlockOrEntity
            var initialBe = initialInfo.blockEntity();
            var initialBlock = initialInfo.block();
            var initialIsActualCable = initialBe == null;
            RuntimeAssert.RuntimeAssert(initialIsActualCable || initialBe instanceof CableConnectableBlockOrEntity, "!initialIsActualCable must imply :CableConnectableBlockOrEntity");
            RuntimeAssert.RuntimeAssert(!initialIsActualCable || initialBlock instanceof BaseCableBlock, "initialIsActualCable must imply :BaseCableBlock");

            var blockPosProcessingQueue = new ArrayDeque<BlockPos>(); // blockpsoes that we connect to which are or act as a cable
            blockPosProcessingQueue.add(initialBp);
            var alreadyProcessedBps = new HashSet<BlockPos>();
            while (!blockPosProcessingQueue.isEmpty()) {
                var currBp = blockPosProcessingQueue.remove();
                var currInfo = getInfoAboutBp(level, currBp, clusterType);
                RuntimeAssert.RuntimeAssert(currInfo.isOrActsAsCable(), "should be or act as a cable, why would we be traversing otherwise");

                if (currInfo.blockEntity() instanceof BaseCableConnectableBlockEntity cableConnectableBe) {
                    clusterBaseCableConnectableEnts.add(cableConnectableBe);
                    if (cableConnectableBe instanceof ClusterHostEntity che && che.isHostForNetwork(clusterType))
                        clusterHostBlockEntities.add(che);
                }

                if (currInfo.block() instanceof BaseCableBlock) foundActualCableBlocksOfThisType.add(currBp);


                forAllDirs(dir -> {
                    var newBp = currBp.relative(dir);
                    var newInfo = getInfoAboutBp(level, newBp, clusterType);
                    if (!newInfo.supportsCurrentCluster()) {
                        if (currInfo.blockEntity() instanceof CableConnectableBlockOrEntity entToClearFaceOf)
                            facesThatAreNotConnectedToCurrentClusterType.add(new Tuple<>(entToClearFaceOf, dir));
                        return;
                    }

                    // src always acts as a cable --> just check if dest wants to connect
                    var doesDestinationWantToConnectToSrc = newInfo.blockEntity() instanceof CableConnectableBlockOrEntity newCcbe &&
                                                            newCcbe.canBePartOfCluster(clusterType) &&
                                                            newCcbe.canConnectTo(clusterType, dir.getOpposite());
                    if (doesDestinationWantToConnectToSrc) { // if dest wants to connect (and source can ofc definitely connect, add connection to whichever supports it)
                        if (currInfo.blockEntity() instanceof CableConnectableBlockOrEntity ccbe2 && ccbe2.canBePartOfCluster(clusterType)) {
                            facesToAssignCurrentNetTo.add(new Tuple<>(ccbe2, dir));
                        }

                        var newCcbe_copy_proven = (CableConnectableBlockOrEntity) newInfo.blockEntity();
                        if (newCcbe_copy_proven.canBePartOfCluster(clusterType)) {
                            facesToAssignCurrentNetTo.add(new Tuple<>(newCcbe_copy_proven, dir.getOpposite()));
                        }
                    } else if (newInfo.block() instanceof BaseCableBlock bcb && currInfo.blockEntity() instanceof CableConnectableBlockOrEntity blockWeAreComingFrom) {
                        // if dest doesnt want to connect but is a cable, still track the face on our block if we are a block that wants to track this
                        assert bcb.clusterType.equals(clusterType);
                        facesToAssignCurrentNetTo.add(new Tuple<>(blockWeAreComingFrom, dir));
                    }

                    if (newInfo.isOrActsAsCable()) { // this is a cable --> trace from this startpoint
                        if (alreadyProcessedBps.add(newBp))
                            blockPosProcessingQueue.add(newBp); // recurse

                    } else { // this is not a cable, but an entity that needs to be connected --> just track the face
                        // this means, check if dest wants to connect, and then connect like in last case, but dont recurse
                        //noinspection ConstantValue
                        assert newInfo.supportsCurrentCluster();

                        if (doesDestinationWantToConnectToSrc) { // if dest wants to connect (and source can ofc definitely connect, add connection to whichever supports it (already handled before))
                            if (newInfo.blockEntity() instanceof BaseCableConnectableBlockEntity cableConnectableBe2) {
                                clusterBaseCableConnectableEnts.add(cableConnectableBe2);
                                if (cableConnectableBe2 instanceof ClusterHostEntity che2 && che2.isHostForNetwork(clusterType))
                                    clusterHostBlockEntities.add(che2);
                            }
                        }
                    }
                });
            }

            var cluster = new CableCluster(clusterBaseCableConnectableEnts.toArray(BaseCableConnectableBlockEntity[]::new),
                    clusterHostBlockEntities.toArray(ClusterHostEntity[]::new), clusterType);

            var updatesToEmit = new HashSet<CableConnectableBlockOrEntity>();
            for (var tpl : facesThatAreNotConnectedToCurrentClusterType) {
                var ent = tpl.x();
                var netList = ent.getNetworkList();
                var existingCluster = netList.get(tpl.y());
                if (existingCluster != null && existingCluster.getClusterType() == clusterType) { // dont remove other clusters that we didnt touch
                    netList.remove(tpl.y());
                    updatesToEmit.add(ent);
                }
            }

            for (var tpl : facesToAssignCurrentNetTo) {
                var ent = tpl.x();
                ent.getNetworkList().put(tpl.y(), cluster);
                updatesToEmit.add(ent);
            }
            for (var ent : updatesToEmit) ent.onNetworkUpdated();
            updateCableBlockStates(new ArrayList<>(foundActualCableBlocksOfThisType), cluster.getHostCount() <= 1, level);
        } else { // is a block entity --> 6 nets
            // if this is the initial block, clear all nets as we will rebuild them anyway
//            if (initialInfo.blockEntity() instanceof CableConnectableBlockOrEntity entToClear) // TODO why doesnt this work?
//                entToClear.getNetworkList().clear();

            forAllDirs(dir -> {
                var neighborPos = initialBp.relative(dir);
                var neighborInfo = getInfoAboutBp(level, neighborPos, clusterType);
                // if the neighbor isnt an interesting face, simply remove our network and be done
                if (!neighborInfo.supportsCurrentCluster()) return;

                // if it does, its either a cable or a block entity
                var neighborIsActualCable = neighborInfo.blockEntity() == null;
                RuntimeAssert.RuntimeAssert(neighborIsActualCable || neighborInfo.blockEntity() instanceof CableConnectableBlockOrEntity,
                        "!initialIsActualCable must imply :CableConnectableBlockOrEntity");

                RuntimeAssert.RuntimeAssert(!neighborIsActualCable || neighborInfo.block() instanceof BaseCableBlock,
                        "initialIsActualCable must imply :BaseCableBlock");

                if (neighborIsActualCable) { // if it is a cable, just rebuilt from there
                    buildSingleNet2(level, neighborPos, clusterType);
                } else { // if it is a block entity, spawn a network that connects just those two and assign it
                    var connectedEntities = new BaseCableConnectableBlockEntity[]{
                            (BaseCableConnectableBlockEntity) initialInfo.blockEntity(),
                            (BaseCableConnectableBlockEntity) neighborInfo.blockEntity()
                    };

                    var cluster2 = new CableCluster(connectedEntities, Arrays.stream(connectedEntities)
                            .filter(x -> x instanceof ClusterHostEntity che && che.isHostForNetwork(clusterType))
                            .map(x -> ((ClusterHostEntity) x))
                            .toArray(ClusterHostEntity[]::new),
                            clusterType);

                    connectedEntities[0].getNetworkList().put(dir, cluster2);
                    connectedEntities[1].getNetworkList().put(dir.getOpposite(), cluster2);
                    connectedEntities[0].onNetworkUpdated();
                    connectedEntities[1].onNetworkUpdated();
                    RuntimeAssert.RuntimeAssert(connectedEntities[0] != connectedEntities[1], "what?");
                }
            });
        }
    }

    private static void buildSingleNet(Level level, BlockPos initialBp, ClusterType clusterType, Direction dir) {
        if (level.getBlockState(initialBp).isAir()) return; // nothing to do in this case

        // build a single network starting from either this block or from this block face if dir is nonnull
        var facesToTraverse = new ArrayDeque<Tuple<BlockPos, Direction>>();
        if (dir != null) {
            facesToTraverse.add(new Tuple<>(initialBp, dir));
        } else {
            var initialSrcInfo = getInfoAboutBp(level, initialBp, clusterType);
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

                if (srcBpInfo.isOrActsAsCable() || srcBpos.equals(initialBp) && dir3.equals(dir))
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

    public int getDebugId() {
        return this.debugId;
    }
}
