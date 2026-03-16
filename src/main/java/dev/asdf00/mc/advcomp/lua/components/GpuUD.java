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
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.types.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.LuaSerializationUtils;
import dev.asdf00.mc.advcomp.utils.SetBiMap;
import dev.asdf00.mc.advcomp.utils.Tuple;
import net.minecraft.world.level.LevelAccessor;

import java.util.*;

public class GpuUD extends BaseAcComponent {
    public final SetBiMap<ScreenBlockEntity, TextBufferUD> screenBufferMap;
    private final HashSet<TextBufferUD> allocatedBuffers;

    @LuaExposed(LuaExposed.Policy.READ)
    public volatile int remainingVideoRam = 128 * 25 * 4; // TODO figure out a proper size
    private final Object remainingVideoRamLockObj = new Object();

    public GpuUD() {
        super("gpu");
        screenBufferMap = new SetBiMap<>();
        allocatedBuffers = new HashSet<>();
    }

    private GpuUD(LuaVirtualMachine acVm) {
        super("gpu", acVm, true);
        screenBufferMap = new SetBiMap<>();
        allocatedBuffers = new HashSet<>();
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
        if (haveEnoughSpace) {
            var buf = new TextBufferUD(width, height, this);
            allocatedBuffers.add(buf); // track it for freeAllBuffers
            return buf;
        }

        throw new LuaJavaError("Not enough video ram remaining to allocate buffer of size (%s,%s)".formatted(width, height));
    }

    void onBufferFreed(TextBufferUD bufferToFree) {
        synchronized (remainingVideoRamLockObj) {
            if (bufferToFree.isFreed)
                throw new LuaJavaError("Buffer was freed already");
            remainingVideoRam += bufferToFree.width * bufferToFree.height;
            RuntimeAssert.RuntimeAssert(allocatedBuffers.remove(bufferToFree), "tried to free an already freed buffer??");
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

    @LuaCallable
    public int freeAllBuffers() {
        int freedBufferCount = 0;
        for (var buf : allocatedBuffers.toArray(TextBufferUD[]::new)) {
            buf.free();
            freedBufferCount++;
        }
        return freedBufferCount;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        var bdr = new ByteArrayBuilder();
        bdr.append(remainingVideoRam);
        bdr.append(allocatedBuffers.size());
        var serializedObjectsMap = new HashMap<TextBufferUD, Integer>();
        for (var allocatedBuffer : allocatedBuffers) {
            var serializedId = LuaObject.of(allocatedBuffer).serialize(serialData, mappedObjs, additionalData);
            serializedObjectsMap.put(allocatedBuffer, serializedId);
            bdr.append(serializedId);
        }
        for (var entry : screenBufferMap.entrySet()) {
            LuaSerializationUtils.appendBlockEntity(bdr, entry.getKey());
            var serializedBuffer = serializedObjectsMap.get(entry.getValue());
            RuntimeAssert.RuntimeAssert(serializedBuffer != null, "serialization failed");
            bdr.append(serializedBuffer);
        }
        return bdr.toArray();
    }

    @LuaDeserializer
    public static GpuUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        LevelAccessor level = ((LuaVirtualMachine) additionalData).computerBlockEntity.getLevel();
        int remainingVram = reader.readInt();
        int allocatedBuffers_size = reader.readInt();

        var allocatedBuffers_wrappers = new ArrayList<LuaObject>();
        for (int i = 0; i < allocatedBuffers_size; i++) {
            allocatedBuffers_wrappers.add(objs[reader.readInt()]);
        }

        var wrappers = new ArrayList<Tuple<ScreenBlockEntity, LuaObject>>();
        while (reader.remaining() > 0) {
            var be = LuaSerializationUtils.<ScreenBlockEntity>readBlockEntity(reader, level);
            if (be == null) {
                throw new IllegalStateException("we did not find some ScreenBlockEntity");
            }
            wrappers.add(new Tuple<>(be, objs[reader.readInt()]));
        }
        var nu = new GpuUD((LuaVirtualMachine) additionalData);
        nu.remainingVideoRam = remainingVram;

        // unwrap UD objects later
        postActions.add(() -> allocatedBuffers_wrappers.forEach(obj -> nu.allocatedBuffers.add(((TextBufferUD) obj.refVal))));
        postActions.add(() -> wrappers.forEach(t -> nu.screenBufferMap.put(t.x(), (TextBufferUD) t.y().refVal)));
        return nu;
    }
}
