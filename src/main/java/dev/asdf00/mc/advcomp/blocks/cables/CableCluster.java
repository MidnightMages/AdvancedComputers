package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.types.IAcBaseCableConnectableEntity;
import dev.asdf00.mc.advcomp.types.IAcCableHostEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;

public class CableCluster {
    public final HashSet<IAcBaseCableConnectableEntity> connectedEntities;
    private final HashSet<IAcBaseCableConnectableEntity> connectedHostEntities;

    public int getHostCount() {
        return connectedHostEntities.size();
    }

    public int getEntityCount() {
        return connectedEntities.size();
    }

    public CableCluster(HashSet<IAcBaseCableConnectableEntity> connectedEntities, HashSet<IAcBaseCableConnectableEntity> connectedHostEntities) {
        this.connectedEntities = connectedEntities;
        this.connectedHostEntities = connectedHostEntities;
    }

    public static void onBlockPosChanged(Level level, BlockPos initialBp) {
        // peripheral cluster
        CableCluster.onBlockPosChangedInternal(level, initialBp, (IAcBaseCableConnectableEntity be) -> be instanceof ComputerBlockEntity,
                (IAcBaseCableConnectableEntity be) -> true);

//        // network cluster
//        CableCluster.onBlockPosChangedInternal(level, initialBp, (IAcBaseCableConnectableEntity be) -> false /* TODO instanceof RouterBlockEntity if connected to the host port side */,
//                (IAcBaseCableConnectableEntity be) -> false);
    }

    public static void onBlockPosChangedInternal(Level level, BlockPos initialBp,
                                                 Function<IAcBaseCableConnectableEntity, Boolean> isHostBlock,
                                                 Function<IAcBaseCableConnectableEntity, Boolean> actsAsCable) {
        // what this does is:
        // assume the given blockpos initialBp has been changing, meaining one of the following:
        // computer, cable or IAcCableConnectable was added, removed or somehow replaced
        // approach:
        //    if added: simply rebuild network from current initialBp
        //    otherwise: rebuild 7 networks, one from current and then 6 form the surrounding ones (or always do this one)
        // whenever a network rebuild discovers a computer, the network is read from that computer and it is wiped off of any devices on that network
        var neighborStartPosesToCheck = new LinkedList<BlockPos>();
        neighborStartPosesToCheck.push(initialBp);
        for (var dir : Direction.values())
            neighborStartPosesToCheck.push(initialBp.relative(dir));

        HashSet<BlockPos> alreadyChecked = new HashSet<>();
        while (!neighborStartPosesToCheck.isEmpty()) {
            var networkRebuildStartpoint = neighborStartPosesToCheck.removeFirst(); // startpoint for an initial rebuild
            if (level.getBlockEntity(networkRebuildStartpoint) == null) // if there is no tileentity then we can skip this startpoint
                continue;

            Stack<BlockPos> posesToCheck = new Stack<>();
            posesToCheck.add(networkRebuildStartpoint); // add rebuild startpoint
            HashSet<IAcBaseCableConnectableEntity> connectedDevices = new HashSet<>();
            ArrayList<BlockPos> connectedCables = new ArrayList<>();
            HashSet<IAcBaseCableConnectableEntity> connectedComputers = new HashSet<>(1);
            Consumer<BlockPos> addNeighborsFunc = bpToAdd -> {
                for (var dir : Direction.values())
                    posesToCheck.push(bpToAdd.relative(dir));
            };

            while (!posesToCheck.empty()) {
                var pos = posesToCheck.pop();
                if (alreadyChecked.contains(pos)) // we have already checked this pos --> we are done
                    continue;

                boolean addToAlreadyChecked = true;
                var posBe = level.getBlockEntity(pos);
                if (posBe == null) // blockPos has no tileentity --> cant interact with cable ever --> we are done
                    continue;

                if (posBe instanceof CableBlockEntity) { // cable --> keep scanning neighbors
                    addNeighborsFunc.accept(pos);
                    connectedCables.add(pos);
                } else if (posBe instanceof IAcBaseCableConnectableEntity bcce) { // TE is relevant to this network/cluster --> process it
                    if (isHostBlock.apply(bcce))
                        connectedComputers.add(bcce);
                    else
                        connectedDevices.add(bcce);

                    if (actsAsCable.apply(bcce))
                        addNeighborsFunc.accept(pos);
                }
                alreadyChecked.add(pos);

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

            var newNet = new CableCluster(connectedDevices, connectedComputers);
            for (var dev : newNet.connectedEntities) {
                dev.getNetworkList().add(newNet);
            }

            boolean isNetworkValid = true;// default: no computers -> no update -> show cables as white aka normal
            for (var host : newNet.connectedHostEntities) { // go through all networks associated with the computers we found just now
                var oldNet = host.getNetworkList(); // should only contain one network, but just in case there is more, we iterate
                for (var on : oldNet) { // remove old references to this network from the nets that are being replaced
                    for (var on_devs : on.connectedEntities)
                        on_devs.getNetworkList().remove(on);
                    for (var on_comps : on.connectedHostEntities)
                        on_comps.getNetworkList().remove(on);
                }
                host.getNetworkList().add(newNet);
                if (host instanceof IAcCableHostEntity che) {
                    // tell the cluster-host(s) that the net might have changed;
                    // if any computer deems the network as invalid, show it as red
                    isNetworkValid &= che.onNetworkUpdated();
                } else
                    throw new RuntimeException("Class %s does not implement the interface %s".formatted(host.getClass().getName(), IAcCableHostEntity.class));
            }
            UpdateCableBlockStates(connectedCables, isNetworkValid, level);

            // last step remove all next potential startingpoints from that set if we have already checked them
            neighborStartPosesToCheck.removeIf(alreadyChecked::contains);
        }
    }

    private static void UpdateCableBlockStates(ArrayList<BlockPos> connectedCables, boolean netIsOk, Level level) {
        for (var c : connectedCables) {
            var bs = level.getBlockState(c).setValue(CableBlock.NETWORK_ERROR, !netIsOk);
            level.setBlock(c, bs, 2); // flags: 2 = sendToClient (NO block update)
        }
    }
}
