package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScreenBlockUD extends BaseAcComponent {

    private final ScreenBlockEntity screenBlockEntity;

    public ScreenBlockUD(ScreenBlockEntity screenBlockEntity) {
        super("screen");
        this.screenBlockEntity = screenBlockEntity;
    }

    // TODO implement lua api
    @LuaCallable
    public void printInline(LuaObject[] args) {
        printInternal(args, false);
    }

    @LuaCallable
    public void print(LuaObject[] args) {
        printInternal(args, true);
    }

    private void printInternal(LuaObject[] args, boolean doNewline) {
        var str = Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")) + (doNewline ? "\n" : "");
        NetCodeUtils.sendToClient(PacketDistributor.ALL.noArg(), new ScreenBlockEntity.ScreenContentToClientEvent(this.screenBlockEntity, "appendGuiText", str));
    }

    public void clearGuiScreen() {
        NetCodeUtils.sendToClient(PacketDistributor.ALL.noArg(), new ScreenBlockEntity.ScreenContentToClientEvent(this.screenBlockEntity, "clearGuiText", ""));
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static ScreenBlockUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
