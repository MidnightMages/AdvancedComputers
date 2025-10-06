package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.*;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.LuaEventQueue;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;

import java.util.List;
import java.util.Map;

public class ComputerUD implements LuaUserDataComponent {
    private final LuaVirtualMachine lvm;

    @Override
    public String getComponentType() {
        return "computer";
    }

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty id = LuaProperty.ofString(
            () -> "computer",
            null
    );

    public ComputerUD(LuaVirtualMachine lvm) {
        this.lvm = lvm;
    }

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject nvram = LuaObject.of(new NvramUD());

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
        var e = lvm.eventQueue.getQueuedEventOrNull();
        return e == null ? new LuaObject[]{LuaObject.NIL} : e;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static ComputerUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
