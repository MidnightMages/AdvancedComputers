package dev.asdf00.mc.advcomp.lua.components.fs;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.items.ManagedMassStorageUD;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class LuaFsFileUD implements LuaUserData {
    private VirtualFile f;
    private ManagedMassStorageUD parentFilesystemUD;
    private LuaObject luaIdentity;

    public LuaFsFileUD(VirtualFile f, ManagedMassStorageUD parentFilesystemUD) {
        this.f = f;
        this.parentFilesystemUD = parentFilesystemUD;
    }

    @LuaCallable
    public String read() {
        return f.readAllText();
    }

    @LuaCallable
    public void write(String s) {
        f.writeAllText(s);
    }

    @LuaCallable
    public void append(String s) {
        f.appendAllText(s);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return new ByteArrayBuilder()
                .append(LuaObject.of(parentFilesystemUD).serialize(serialData, mappedObjs, additionalData))
                .append(parentFilesystemUD.serializeVirtualFile(f))
                .toArray();
    }

    @LuaDeserializer
    public static LuaFsFileUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var parentFs = objs[reader.readInt()];
        var fData = reader.readString();


        var rv = new LuaFsFileUD(null, null);
        postActions.add(() -> {
            rv.parentFilesystemUD = ((ManagedMassStorageUD) parentFs.refVal);
            rv.f = rv.parentFilesystemUD.deserializeVirtualFile(fData);
        });
        return rv;
    }

    @Override
    public LuaObject getSelfAsLuaObject() {
        return luaIdentity;
    }

    @Override
    public void setSelfAsLuaObject(LuaObject self) {
        this.luaIdentity = self;
    }
}