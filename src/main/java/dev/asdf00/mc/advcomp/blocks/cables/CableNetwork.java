package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.types.IAcCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.function.Consumer;

public class CableNetwork {
    public final HashSet<IAcCableConnectableEntity> connectedPeripherals;
    private final HashSet<ComputerBlockEntity> connectedComputers;

    public int getComputerCount() {
        return connectedComputers.size();
    }
    public int getPeripheralCount() {
        return connectedPeripherals.size();
    }

    public CableNetwork(HashSet<IAcCableConnectableEntity> connectedPeripherals, HashSet<ComputerBlockEntity> connectedComputers) {
        this.connectedPeripherals = connectedPeripherals;
        this.connectedComputers = connectedComputers;
    }

    public static void onBlockPosChanged(LevelReader level, BlockPos initialBp) {
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

            Stack<BlockPos> posesToCheck = new Stack<>();
            posesToCheck.add(networkRebuildStartpoint); // add rebuild startpoint
            HashSet<IAcCableConnectableEntity> connectedDevices = new HashSet<>();
            HashSet<ComputerBlockEntity> connectedComputers = new HashSet<>(1);
            Consumer<BlockPos> addNeighborsFunc = bpToAdd -> {
                for (var dir : Direction.values())
                    posesToCheck.push(bpToAdd.relative(dir));
            };

            while (!posesToCheck.empty()) {
                var pos = posesToCheck.pop();
                if (!alreadyChecked.add(pos)) // we have already checked this pos --> we are done
                    continue;

                var posBe = level.getBlockEntity(pos);
                if (posBe == null) // blockPos has no tileentity --> cant interact with cable ever --> we are done
                    continue;

                if (posBe instanceof ComputerBlockEntity compBe) { // computer --> unregister old network later, store reference for curr network; keep scanning neighbors
                    connectedComputers.add(compBe);
                    addNeighborsFunc.accept(pos);
                } else if (posBe instanceof CableBlockEntity) { // cable --> keep scanning neighbors
                    addNeighborsFunc.accept(pos);
                } else if (posBe instanceof IAcCableConnectableEntity connectableBe) { // peripheral device --> keep track of it so we can set refs later
                    connectedDevices.add(connectableBe);
                }
            }

            var newNet = new CableNetwork(connectedDevices, connectedComputers);
            for (var dev : newNet.connectedPeripherals) {
                dev.getNetworkList().add(newNet);
            }

            for (var comp : newNet.connectedComputers) { // go through all networks associated with the computers we found just now
                var oldNet = comp.getNetworkList(); // should only contain one network, but just in case there is more, we iterate
                for (var on : oldNet) { // remove old references to this network from the nets that are being replaced
                    for (var on_devs : on.connectedPeripherals)
                        on_devs.getNetworkList().remove(on);
                    for (var on_comps : on.connectedComputers)
                        on_comps.getNetworkList().remove(on);
                }
                comp.getNetworkList().add(newNet);
                comp.onNetworkUpdated(); // tell the computers that the net might has changed
            }

            // last step remove all next potential startingpoints from that set if we have already checked them
            neighborStartPosesToCheck.removeIf(alreadyChecked::contains);
        }
    }
}
