package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.Config;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.MiscUtil;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class NetworkUD extends BaseAcComponent {
    public NetworkUD() {
        super("network");
    }

    private NetworkUD(LuaVirtualMachine acVm) {
        // an internet component, if present, is always available
        super("network", acVm, true);
    }

    private static int argcheckParseAddress(String luaAddress) { // address format 00.11.aa.ff; in regex terms [0-9a-f]{2}(\.[0-9a-f]{2}){3}; MSB first
        var splitted = luaAddress.split("\\.");
        if (splitted.length != 4) {
            throw new LuaJavaError("malformed ac ip address '%s'.".formatted(luaAddress));
        }
        long fullIp = 0;
        for (int i = 0; i < 4; i++) {
            var currString = splitted[i].toLowerCase();
            if (currString.length() != 2) {
                throw new LuaJavaError("ac ip address '%s' contains malformed segment '%s'.".formatted(luaAddress, currString));
            }
            fullIp <<= 8;
            fullIp += Integer.parseInt(currString, 16);
        }
        if (fullIp > Integer.MAX_VALUE) {
            throw new LuaJavaError("malformed ac ip address '%s'.".formatted(luaAddress));
        }

        return (int)fullIp;
    }

    private static void argcheckPort(int port) {
        if (port > (1 << 16) - 1) {
            throw new LuaJavaError("port must be in range [0, 65535].");
        }
    }

    @SuppressWarnings("unused")
    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty maxPacketSize = LuaProperty.ofInt(() -> Config.componentNetworkMaxPacketSize, null);

    @LuaCallable
    public void send(String address, int port, String message) {
        var acIpAddress = argcheckParseAddress(address);
        argcheckPort(port);
        if (message.length() > Config.componentNetworkMaxPacketSize)
            throw new LuaJavaError("message is too long, must be at most %s characters.".formatted(Config.componentNetworkMaxPacketSize));

        var ourNode = this.acVm.computerBlockEntity.getNetworkNode();
        var targetComputer = this.acVm.computerBlockEntity.getComputerBlockEntityForAcIp(acIpAddress);
        var path = ourNode.getShortestPathTo(targetComputer.getNetworkNode());
        if (path != null) { // if target is reachable
            var nodePath = path.nodePath();
            AdvancedComputers.LOGGER.warn("Would send network packet from bp %s to %s with length %s.".formatted(
                    nodePath[0].getPos(),
                    nodePath[nodePath.length - 1].getPos(),
                    path.length())
            );
            // TODO emit event with some delay and possible packet loss?
            // TODO need invokequeue so this works across dimensions probs

            LuaObject receiverSide = LuaObject.of("unknown");
            if (!nodePath[0].equals(nodePath[nodePath.length - 1])) {
                var lastIntermediateBlockEntity = nodePath[nodePath.length - 2].getBlockEntity();
                assert lastIntermediateBlockEntity != null;
                for (var dir : Direction.values()) {
                    var net = targetComputer.connectedNetworks.get(dir);
                    if (Set.of(net.connectedEntities).contains(lastIntermediateBlockEntity)) {
                        receiverSide = LuaObject.of(dir.toString().toLowerCase());
                        break;
                    }
                }
            }

            long delayByMilliseconds = (long) ((path.length() / Config.componentNetworkTransmissionSpeedBlocksPerSecond) * 1000);

            targetComputer.getLvm().queueDelayedMachineEvent(delayByMilliseconds,
                    "networkPacket",
                    LuaObject.of(message),
                    LuaObject.of(port),
                    LuaObject.of(MiscUtil.AcIpToString(this.acVm.computerBlockEntity.getAcIpAddress())),
                    receiverSide
            );
        }
    }


    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return new byte[0];
    }

    @SuppressWarnings("unused")
    @LuaDeserializer
    public static NetworkUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return new NetworkUD((LuaVirtualMachine) additionalData);
    }
}
