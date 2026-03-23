package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.AcPaths;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class UefiUD extends BaseAcComponent {
    private int uefiId = -1;
    @SuppressWarnings("FieldCanBeLocal")
    private final int MAX_UEFI_LENGTH = 8192;

    @LuaExposed(LuaExposed.Policy.READWRITE)
    public final LuaProperty data = LuaProperty.ofString(this::getUefiScript, this::setUefiScript);

    public UefiUD(int uefiId) {
        super("uefi");
        this.uefiId = uefiId;
    }

    private UefiUD(LuaVirtualMachine acVm) {
        super("uefi", acVm, true);
    }

    public String getUefiScript() {
        RuntimeAssert.RuntimeAssert(uefiId >= 0, "uefi id was negative somehow");
        try {
            return Files.readString(AcPaths.getUefiFilePath(uefiId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUefiScript(String newScript) {
        RuntimeAssert.RuntimeAssert(uefiId >= 0, "uefi id was negative on write somehow");
        if (newScript.length() > MAX_UEFI_LENGTH)
            throw new LuaJavaError("UEFI only supports a max length of %s characters.".formatted(MAX_UEFI_LENGTH));
        try {
            Files.writeString(AcPaths.getUefiFilePath(uefiId), newScript);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        var bdr = new ByteArrayBuilder(4);
        bdr.append(uefiId);
        return bdr.toArray();
    }

    @LuaDeserializer
    public static UefiUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var nu = new UefiUD((LuaVirtualMachine) additionalData);
        nu.uefiId = reader.readInt();
        return nu;
    }
}
