package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.cables.base.BaseCableBlock;
import dev.asdf00.mc.advcomp.types.cluster.AcClusterType;
import dev.asdf00.mc.advcomp.types.cluster.IAcBaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.types.cluster.IAcBaseCableConnectableEntity;
import dev.asdf00.mc.advcomp.types.cluster.IAcClusterHostEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class CableCluster {
    public final ArrayList<IAcBaseCableConnectableBlockEntity> connectedEntities; // contains all connected devices
    private final ArrayList<IAcClusterHostEntity> connectedHostEntities; // contains all connected devices that implement the interface IAcClusterHostEntity
    public final AcClusterType clusterType;

    public int getHostCount() {
        return connectedHostEntities.size();
    }

    public int getEntityCount() {
        return connectedEntities.size();
    }

    public AcClusterType getClusterType() {
        return this.clusterType;
    }

    public CableCluster(ArrayList<IAcBaseCableConnectableBlockEntity> connectedEntities, ArrayList<IAcClusterHostEntity> connectedHostEntities, AcClusterType clusterType) {
        this.connectedEntities = connectedEntities;
        this.connectedHostEntities = connectedHostEntities;
        this.clusterType = clusterType;
    }

    public static void onBlockPosChanged(Level level, BlockPos initialBp) {
        var clusterTypes = AdvancedComputers.AC_CLUSTER_TYPE_MANAGER.GetNetworkTypes();
        for (var cType : clusterTypes.values()) {
            CableCluster.onBlockPosChangedInternal(level, initialBp, cType);
        }

        // peripheral cluster
//        CableCluster.onBlockPosChangedInternal(level, initialBp, (IAcBaseCableConnectableEntity be) -> be instanceof ComputerBlockEntity,
//                (IAcBaseCableConnectableEntity be) -> true);

//        // network cluster
//        CableCluster.onBlockPosChangedInternal(level, initialBp, (IAcBaseCableConnectableEntity be) -> false /* TODO instanceof RouterBlockEntity if connected to the host port side */,
//                (IAcBaseCableConnectableEntity be) -> false);
    }

    public static void onBlockPosChangedInternal(Level level, BlockPos initialBp, AcClusterType clusterType) {
        // what this does is:
        // assume the given blockpos initialBp has been changing, meaining one of the following:
        // computer, cable or IAcBaseCableConnectableEntity was added, removed or somehow replaced
        // approach:
        //    (not implemented yet) if added: simply rebuild network from current initialBp
        //    otherwise: rebuild 7 networks, one from current and then 6 form the surrounding ones (or always do this one)
        // whenever a network rebuild discovers a IAcBaseCableConnectableBlockEntity, the network is read from that block and it is wiped off of any devices on that network
        var neighborStartPosesToCheck = new LinkedList<BlockPos>();
        neighborStartPosesToCheck.push(initialBp);
        for (var dir : Direction.values())
            neighborStartPosesToCheck.push(initialBp.relative(dir));

        HashSet<BlockPos> alreadyCheckedCables = new HashSet<>();
        while (!neighborStartPosesToCheck.isEmpty()) {

            var networkRebuildStartpoint = neighborStartPosesToCheck.removeFirst(); // startpoint for an initial rebuild
            var spBe = level.getBlockEntity(networkRebuildStartpoint);
            if (spBe == null) // if there is no tileentity then we can skip this startpoint
                continue;

            // if this startpoint does not support interact with cables then we are done
            if (!(spBe instanceof IAcBaseCableConnectableEntity prece))
                continue;

            // if this startpoint does not support this cluster type then we are done
            if(!prece.canBePartOfCluster(clusterType))
                continue;

            if (prece instanceof  IAcBaseCableConnectableBlockEntity precbe)
                precbe.getNetworkList().clear(); // TODO let the block know if a face was cleared and not actually re-discovered and restored


            // this is from the perspective of the block itself, so going into the block by going north in the algorithm would mean this contains the south side of the block
            HashMap<BlockPos, ArrayList<Direction>> alreadyEnteredBlockFaces = new HashMap<>();

            HashMap<BlockPos, IAcBaseCableConnectableBlockEntity> connectedDevices = new HashMap<>();
            HashMap<BlockPos, IAcClusterHostEntity> connectedHosts = new HashMap<>();
            AtomicBoolean startpointWasProcessed = new AtomicBoolean(false);

            Stack<BlockPos> posesToCheck = new Stack<>();
            posesToCheck.add(networkRebuildStartpoint); // add rebuild startpoint
            //HashSet<IAcBaseCableConnectableEntity> connectedComputers = new HashSet<>(1);
            ArrayList<BlockPos> connectedCables = new ArrayList<>();
            BiConsumer<BlockPos, IAcBaseCableConnectableEntity> addNeighborsOfFunc = (bpToAdd, be) -> {
                boolean isFirst = !startpointWasProcessed.get();
                if (isFirst)
                    startpointWasProcessed.set(true);

                for (var dir : Direction.values()) {
                    var rel = bpToAdd.relative(dir);
                    var relBe = level.getBlockEntity(rel);
                    if (relBe instanceof IAcBaseCableConnectableEntity ce) { // if this is false then we can ignore it
                        // otherwiseif this was the startpoint, then unmark it for clearing as we now know which direction to assign it to
                        if (isFirst) {
                            var firstArr = alreadyEnteredBlockFaces.get(bpToAdd);
                            if (firstArr != null) {
                                if (firstArr.contains(dir)) {
                                    throw new RuntimeException("Invalid state"); // we have already entered the block from this side, but how?
                                } else {
                                    firstArr.add(dir);
                                }
                            } else {
                                alreadyEnteredBlockFaces.put(bpToAdd, new ArrayList<>(List.of(dir)));
                            }
                        }

                        // additionally we have to check first if we have already entered this block from this side (if it is a block entity and not a cable)
                        if (ce instanceof IAcBaseCableConnectableBlockEntity) { // either skip or mark
                            var arr = alreadyEnteredBlockFaces.get(rel);
                            var blockFace = dir.getOpposite();
                            if (arr != null) {
                                if (arr.contains(blockFace)) {
                                    continue; // we have already entered the block from this side --> skip to avoid loops
                                } else {
                                    arr.add(blockFace);
                                }
                            } else {
                                alreadyEnteredBlockFaces.put(rel, new ArrayList<>(List.of(blockFace)));
                            }
                        }
                        if (ce.canBePartOfCluster(clusterType) && be.canConnectTo(ce, dir)) // check if the other thing can be part of this network type and then check if it can connect
                            posesToCheck.push(rel);
                    }
//
//                    if (relBe instanceof IAcBaseCableConnectableEntity be2) { // check blocks
//                        if (be.canConnectTo(be2, dir))
//                            posesToCheck.push(rel);
//                    } else if (relBe instanceof BaseAcCableEntityBlock cablebe) // new thing is a cable
//                    {
//                        if (be.canConnectTo(cablebe, dir))
//                            posesToCheck.push(rel);
//                    }
                }
            };

//            BiConsumer<BlockPos, IAcBaseCableConnectableEntity> addNeighborsOfBeFunc = (bpToAdd, be) -> {
//                for (var dir : Direction.values()) {
//                    var rel = bpToAdd.relative(dir);
//                    var relBe = level.getBlockEntity(rel);
//                    if (relBe instanceof IAcBaseCableConnectableEntity be2) { // check blocks
//                        if (be.canConnectTo(be2, dir))
//                            posesToCheck.push(rel);
//                    } else if (relBe instanceof BaseAcCableEntityBlock cablebe) // new thing is a cable
//                    {
//                        if (be.canConnectTo(cablebe, dir))
//                            posesToCheck.push(rel);
//                    }
//                }
//            };
//            BiConsumer<BlockPos, BaseCableBlockEntity> addNeighborsOfCableFunc = (bpToAdd, bcce) -> {
//                for (var dir : Direction.values()) {
//                    var rel = bpToAdd.relative(dir);
//                    var relBe = level.getBlockEntity(rel);
//                    if (relBe instanceof IAcBaseCableConnectableEntity be) { // check blocks
//                        if (be.canConnectTo(bcce, dir.getOpposite()))
//                            posesToCheck.push(rel);
//                    } else if (relBe instanceof BaseAcCableEntityBlock cablebe) // add cables of same type
//                    {
//                        if (cablebe.getClass().equals(bcce.getClass()))
//                            posesToCheck.push(rel);
//                    }
//                    // if block entity is null then there is no point in even adding it to the candidates
//                }
//            };

            while (!posesToCheck.empty()) {
                var pos = posesToCheck.pop();
                if (alreadyCheckedCables.contains(pos)) // we have already checked this pos --> we are done
                    continue;

                var posBe = level.getBlockEntity(pos);

                // blockPos has no tileentity or no cable-like tileentity --> cant interact with cable ever --> we are done
                if (posBe instanceof IAcBaseCableConnectableEntity ce) {
                    if (ce.actsAsCable()) { // if it acts as a cable --> add neighbors
                        addNeighborsOfFunc.accept(pos, ce);
                    }
                    // in any case, add the device, if it is one, to the current list
                    if (ce instanceof IAcBaseCableConnectableBlockEntity cbe) {
                        connectedDevices.put(pos, cbe);
                        if (cbe instanceof IAcClusterHostEntity che) {
                            connectedHosts.put(pos, che);
                        }
                    } else { // otherwise it must be a normal cable --> keep track of it so we can update blockstates later
                        connectedCables.add(pos);
                        alreadyCheckedCables.add(pos);
                    }
                }

//                if (posBe instanceof ComputerBlockEntity compBe) { // computer --> unregister old network later, store reference for curr network; keep scanning neighbors
//                    connectedComputers.add(compBe);
//                    addNeighborsFunc.accept(pos);
//                } else if (posBe instanceof CableBlockEntity) { // cable --> keep scanning neighbors
//                    addNeighborsFunc.accept(pos);
//                } else if (posBe instanceof IAcDevCableConnectableEntity connectableBe) { // peripheral device --> keep track of it so we can set refs later
//                    connectedDevices.add(connectableBe);
//                    addToAlreadyChecked = false;
//                }
//
//                if (addToAlreadyChecked)
//                    alreadyChecked.add(pos);
            }

            if (!startpointWasProcessed.get())
                throw new RuntimeException("what?");

            if (connectedDevices.size() == 1 && connectedCables.isEmpty()) // this must be a single block --> no network --> simply unset it on the block, if it even is a blockentity
            {
                var nl = connectedDevices.values().iterator().next().getNetworkList();
//              var dirs = new ArrayList<>(nl.keySet());
                nl.clear();
//              for (var direction : dirs) {
//                  cbe.onNetworkUpdated(direction); // todo also fire this even here and below when a network connection is removed
//              }
            } else {

                var newNet = new CableCluster(new ArrayList<>(connectedDevices.values()), new ArrayList<>(connectedHosts.values()), clusterType);
                for (var keyPos : connectedDevices.keySet()) { // replace existing networks on all connected blocks on the given sides; GC should do the rest
                    var valBe = connectedDevices.get(keyPos);
                    if (valBe == null)
                        throw new IllegalStateException("Cluster rebuild failed");

                    var faces = alreadyEnteredBlockFaces.get(keyPos);
                    var netlist = valBe.getNetworkList();
                    for (Direction face : faces) {
                        netlist.put(face, newNet);
                        valBe.onNetworkUpdated(face);
                    }
                }

                boolean isNetworkValid = true; // default: no hosts -> no update -> show cables as white aka normal
                outerloop:
                for (var keyPos : connectedHosts.keySet()) { // go through all networks associated with the hosts we found just now and ask if the network is valid for all connected sides
                    var valBe = connectedHosts.get(keyPos);
                    for (Direction face : alreadyEnteredBlockFaces.get(keyPos)) { // TODO technically isNetworkValid should be consistent, meaning any side should return the same result if the attached network is the same, so this maybe could be optimized?
                        if (!valBe.isNetworkValid(face)) {
                            isNetworkValid = false;
                            break outerloop;
                        }
                    }
                }
                UpdateCableBlockStates(connectedCables, isNetworkValid, level);
            }
            // last step remove all next potential startingpoints from that set if we have already checked them
            neighborStartPosesToCheck.removeIf(connectedCables::contains);
        }
    }

    private static void UpdateCableBlockStates(ArrayList<BlockPos> connectedCables, boolean netIsOk, Level level) {
        for (var c : connectedCables) {
            var bs = level.getBlockState(c).setValue(BaseCableBlock.NETWORK_ERROR, !netIsOk);
            level.setBlock(c, bs, 2); // flags: 2 = sendToClient (NO block update)
        }
    }
}
