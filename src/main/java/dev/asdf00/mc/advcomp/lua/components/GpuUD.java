package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockUD;
import dev.asdf00.mc.advcomp.utils.SetBiMap;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class GpuUD extends BaseAcComponent {
    public final SetBiMap<ScreenBlockEntity, TextBufferUD> screenBufferMap;

    @LuaExposed(LuaExposed.Policy.READ)
    public volatile int remainingVideoRam = 128 * 25 * 4; // TODO figure out a proper size
    private final Object remainingVideoRamLockObj = new Object();

    public GpuUD() {
        super("gpu");
        screenBufferMap = new SetBiMap<>();
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
            if (bufferToFree.isFreed)
                throw new LuaJavaError("Buffer was freed already");
            remainingVideoRam += bufferToFree.width * bufferToFree.height;
        }
    }

    @LuaCallable
    public void assignBuffer(TextBufferUD buf, ScreenBlockUD screenUD) {
        ScreenBlockEntity sbe = screenUD.screenBlockEntity;
        if (sbe == null)
            throw new IllegalStateException("internal error trying to find screen");

        screenBufferMap.put(sbe, buf);
        acVm.dirtyBuffer(buf);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        throw new UnsupportedOperationException("not implemented");
    }

    @LuaDeserializer
    public static GpuUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        // TODO actually provide serializaion
        return null;
    }
}
