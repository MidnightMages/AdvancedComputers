package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.SetBiMap;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

public class GpuUD extends BaseAcComponent {
    private final LuaVirtualMachine lvm;
    private final SetBiMap<ScreenBlockEntity, TextBufferUD> biMap;

    public GpuUD(LuaVirtualMachine lvm) {
        super("gpu");
        this.lvm = lvm;
        biMap = new SetBiMap<>();
    }

    @LuaCallable
    public TextBufferUD newBuffer(int width, int height) {
        return new TextBufferUD(width, height, this);
    }

    @LuaCallable
    public void assignBuffer(TextBufferUD buf, int screenID) {
        try {
            ScreenBlockEntity sbe = lvm.screenBEs.get(screenID);
            biMap.put(sbe, buf);
            // TODO send to client
        } catch (IndexOutOfBoundsException e) {
            throw new LuaJavaError("Screen %d not found".formatted(screenID));
        }
    }

    /**
     * Should only be called by {@link TextBufferUD}.
     */
    public void dirty(TextBufferUD buf) {
        // TODO check if this works
        for (ScreenBlockEntity sbe : biMap.getBack(buf)) {
            BlockPos pos = sbe.getBlockPos();
            // TODO send msg to client to redraw screen
            // maybe pool stuff until next tick, keep a set of dirty screens and fire redraw messages
            // from ComputerBlockEntity#tick.
            // Network.sendToClient("redraw", pos, buf);
        }
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        throw new UnsupportedOperationException("not implemented");
    }
}
