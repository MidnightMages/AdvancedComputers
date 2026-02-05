package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.SetBiMap;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

public class GpuUD extends BaseAcComponent {
    private final LuaVirtualMachine lvm;
    private final SetBiMap<ScreenBlockEntity, TextBufferUD> biMap;

    @LuaExposed(LuaExposed.Policy.READ)
    public volatile int remainingVideoRam = 128 * 25 * 4; // TODO figure out a proper size
    private final Object remainingVideoRamLockObj = new Object();

    public GpuUD(LuaVirtualMachine lvm) {
        super("gpu");
        this.lvm = lvm;
        biMap = new SetBiMap<>();
    }

    @LuaCallable
    public TextBufferUD newBuffer(int width, int height) {
        if (width <= 0 || height <= 0)
            throw new LuaJavaError("Width and height must be > 0 but were %s and %s respectively.".formatted(width, height));

        var vramNeeded = width * height;
        boolean haveEnoughSpace;
        synchronized (remainingVideoRamLockObj) {
            haveEnoughSpace = remainingVideoRam >= vramNeeded;
            if (haveEnoughSpace) {
                remainingVideoRam -= vramNeeded;
            }
        }
        if (haveEnoughSpace)
            return new TextBufferUD(width, height, this);

        throw new LuaJavaError("Not enough video ram remaining to allocate buffer of size (%s,%s)".formatted(width, height));
    }

    void freeBuffer(TextBufferUD bufferToFree) {
        synchronized (remainingVideoRamLockObj) {
            if (bufferToFree.wasFreedAlready)
                throw new LuaJavaError("Buffer was freed already");

            remainingVideoRam += bufferToFree.width * bufferToFree.height;
            bufferToFree.markAsFreed();
        }
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
            lvm.markScreenForUpdate(sbe);
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

    @LuaDeserializer
    public static GpuUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }

    // TODO NOT THREADSAFE AND UNPROTECTED
    public TextBufferUD getBufferForBlockEntity(ScreenBlockEntity sbe) {
        return biMap.get(sbe);
    }
}
