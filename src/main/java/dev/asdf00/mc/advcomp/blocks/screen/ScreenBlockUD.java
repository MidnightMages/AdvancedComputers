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

    public final ScreenBlockEntity screenBlockEntity;

    public ScreenBlockUD(ScreenBlockEntity screenBlockEntity) {
        super("screen");
        this.screenBlockEntity = screenBlockEntity;
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
