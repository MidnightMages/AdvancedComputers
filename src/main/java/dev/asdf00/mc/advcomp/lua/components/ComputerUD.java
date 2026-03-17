package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public final class ComputerUD extends BaseAcComponent {
    private final ConcurrentLinkedQueue<LuaObject[]> eventQueue = new ConcurrentLinkedQueue<>();
    private long bootTimeMillis = System.currentTimeMillis();

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject uefi;

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject nvram = LuaObject.nil(); // requires mainboard tier 2 or higher

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject tpm = LuaObject.nil(); // requires mainboard tier 3

    /**
     * When using this specific constructor, {@link #setupMainboard} must be called before the object is being used in the VM.
     */
    public ComputerUD() {
        super("computer");
    }

    public void setupMainboard(MainboardItem.MainboardInfo mainboardInfo) {
        uefi = LuaObject.of(new UefiUD(mainboardInfo.uefiId()));
        if (mainboardInfo.tier().ordinal() >= MainboardItem.MainboardTier.T2.ordinal())
            nvram = LuaObject.of(new NvramUD());

//        if(mainboardTier.ordinal() >= MainboardItem.MainboardTier.T3.ordinal()) // TODO add tpm
//            tpm = LuaObject.of(new TpmUD());
    }

    private ComputerUD(LuaVirtualMachine acVm, LuaObject uefi, LuaObject nvram, LuaObject tpm) {
        // a computer component is always available
        super("computer", acVm, true);
        this.uefi = uefi;
        this.nvram = nvram;
        this.tpm = tpm;
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

    @LuaCallable
    public double getEpoch() {
        return System.currentTimeMillis() / 1000d;
    }

    @LuaCallable
    public long getEpochMs() {
        return System.currentTimeMillis();
    }

    @LuaCallable
    public String getDate() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX").format(new Date());
    }

    @LuaCallable
    public double getUptime() {
        return (System.currentTimeMillis() - bootTimeMillis) / 1000d;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        int uefiIdx = uefi.serialize(serialData, mappedObjs, additionalData);
        int nvramIdx = nvram.serialize(serialData, mappedObjs, additionalData);
        int tmpIdx = tpm.serialize(serialData, mappedObjs, additionalData);
        return new ByteArrayBuilder(Integer.BYTES)
                .append(uefiIdx)
                .append(nvramIdx)
                .append(tmpIdx)
                .append(bootTimeMillis)
                .toArray();
    }

    @LuaDeserializer
    public static ComputerUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        int uefiIdx = reader.readInt();
        int nvramIdx = reader.readInt();
        int tpmIdx = reader.readInt();
        var acVM = (LuaVirtualMachine) additionalData;
        var nu = new ComputerUD(acVM, objs[uefiIdx], objs[nvramIdx], objs[tpmIdx]);
        nu.bootTimeMillis = reader.readLong();
        acVM.onUdDeserialize(nu);
        postActions.add(() -> {
            if (!(nu.uefi.refVal instanceof UefiUD)) {
                throw new IllegalStateException(nu + " has no UefiUD after deserialization");
            }
            if (!nu.nvram.isNil() && !(nu.nvram.refVal instanceof NvramUD)) {
                throw new IllegalStateException(nu + " has no NvramUD after deserialization");
            }
//            if (!nu.tpm.isNil() && !(nu.tpm.refVal instanceof TpmUD)) { // todo add tpm
//                throw new IllegalStateException(nu + " has no NvramUD after deserialization");
//            }
        });
        return nu;
    }
}
