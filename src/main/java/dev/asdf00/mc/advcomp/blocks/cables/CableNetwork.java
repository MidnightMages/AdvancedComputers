package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.types.IAcCableConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;

import java.util.HashSet;
import java.util.Stack;

public class CableNetwork {
    public final HashSet<IAcCableConnectable> connectedDevices;
    private final HashSet<ComputerBlockEntity> connectedComputers;

    public int getComputerCount() {
        return connectedComputers.size();
    }

    public CableNetwork(HashSet<IAcCableConnectable> connectedDevices, HashSet<ComputerBlockEntity> connectedComputers) {
        this.connectedDevices = connectedDevices;
        this.connectedComputers = connectedComputers;
    }

    public static CableNetwork buildNetwork(LevelReader level, BlockPos startPos) {
        Stack<BlockPos> posesToCheck = new Stack<>();
        posesToCheck.push(startPos);

        HashSet<BlockPos> alreadyChecked = new HashSet<>();
        HashSet<IAcCableConnectable> connectedDevices = new HashSet<>();
        HashSet<ComputerBlockEntity> connectedComputers = new HashSet<>(1);

        while (!posesToCheck.empty()) {
            var pos = posesToCheck.pop();
            for (Direction direction : Direction.values()) {
                BlockPos p = pos.relative(direction);
                var be = level.getBlockEntity(p);
                if (be instanceof CableBlockEntity) {
                    if (alreadyChecked.add(p)) {
                        posesToCheck.push(p);
                    }
                } else if (be instanceof ComputerBlockEntity cbe) {
                    if (alreadyChecked.add(p)) {
                        posesToCheck.push(p);
                    }
                    connectedComputers.add(cbe);
                } else if (be instanceof IAcCableConnectable iacc) {
                    connectedDevices.add(iacc);
                }
            }
        }

        return new CableNetwork(connectedDevices, connectedComputers);
    }

    public void updateDevices() {
        for (var dev : this.connectedDevices)
            dev.setNetwork(this);
    }

    public static void rebuildNetwork(LevelReader level, BlockPos bp) {
        var newNet = CableNetwork.buildNetwork(level, bp);
        newNet.updateDevices();
    }
}
