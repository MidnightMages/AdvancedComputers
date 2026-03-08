package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockUD;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.LuaSerializationUtils;
import dev.asdf00.mc.advcomp.utils.SetBiMap;
import dev.asdf00.mc.advcomp.utils.Tuple;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
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

    private GpuUD(LuaVirtualMachine acVm) {
        super("gpu", acVm, true);
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
        ScreenBlockEntity sbe = screenUD.blockEntity;
        if (sbe == null)
            throw new IllegalStateException("internal error trying to find screen");

        screenBufferMap.put(sbe, buf);
        acVm.dirtyBuffer(buf);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        var bdr = new ByteArrayBuilder();
        bdr.append(remainingVideoRam);
        for (var entry : screenBufferMap.entrySet()) {
            LuaSerializationUtils.appendBlockEntity(bdr, entry.getKey());
            bdr.append(LuaObject.of(entry.getValue()).serialize(serialData, mappedObjs, additionalData));
        }
        return bdr.toArray();
    }

    @LuaDeserializer
    public static GpuUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        LevelAccessor level = ((LuaVirtualMachine) additionalData).cbe.getLevel();
        int remaining = reader.readInt();
        var wrappers = new ArrayList<Tuple<ScreenBlockEntity, LuaObject>>();
        while (reader.remaining() > 0) {
            var be = LuaSerializationUtils.<ScreenBlockEntity>readBlockEntity(reader, level);
            if (be == null) {
                throw new IllegalStateException("we did not find some ScreenBlockEntity");
            }
            wrappers.add(new Tuple<>(be, objs[reader.readInt()]));
        }
        var nu = new GpuUD((LuaVirtualMachine) additionalData);
        nu.remainingVideoRam = remaining;

        // unwrap UD objects later
        postActions.add(() -> wrappers.forEach(t -> nu.screenBufferMap.put(t.x(), (TextBufferUD) t.y().refVal)));
        return nu;
    }
}
