package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public final class ComputerUD extends BaseAcComponent {
    private final ConcurrentLinkedQueue<LuaObject[]> eventQueue = new ConcurrentLinkedQueue<>();

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject nvram;

    public ComputerUD() {
        super("computer");
        nvram = LuaObject.of(new NvramUD());
    }

    private ComputerUD(LuaVirtualMachine acVm, LuaObject nvram) {
        // a computer component is always available
        super("computer", acVm, true);
        this.nvram = nvram;
    }

    /**
     * This method may be called from outside the LUA thread and enqueues a custom machine event to be read by the host
     * LUA program.
     */
    public void triggerMachineEvent(String eventName, LuaObject... args) {
        eventQueue.add(Stream.concat(Stream.of(LuaObject.of(eventName)), Arrays.stream(args)).toArray(LuaObject[]::new));
    }

    @LuaCallable
    public void beep(double freq, double duration) {
        var dur = Math.min(Math.max(duration, 0), 5);
        if (freq < 20 || freq > 2000) {
            throw new LuaJavaError("Invalid frequency %s. Must be in range [20, 2000]".formatted(freq));
        }

        // TODO fix beep
//        if (enableBeep)
//            playBeep(freq, dur);
    }

    @LuaCallable
    public LuaObject[] getMachineEvent() {
        LuaObject[] e = eventQueue.poll();
        return e == null ? new LuaObject[]{LuaObject.NIL} : e;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        int nvrId = nvram.serialize(serialData, mappedObjs, additionalData);
        return new ByteArrayBuilder(Integer.BYTES).append(nvrId).toArray();
    }

    @LuaDeserializer
    public static ComputerUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        int nvrIdx = reader.readInt();
        var nu = new ComputerUD((LuaVirtualMachine) additionalData, objs[nvrIdx]);
        postActions.add(() -> {
            if (!(nu.nvram.refVal instanceof NvramUD)) {
                throw new IllegalStateException(nu + " has no NvramUD after deserialization");
            }
        });
        return nu;
    }
}
