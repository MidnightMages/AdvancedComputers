package dev.asdf00.mc.advcomp.lua.components.fs;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.List;
import java.util.Map;

public class LuaFsFileUD implements LuaUserData {
    final VirtualFile f;

    public LuaFsFileUD(VirtualFile f) {
        this.f = f;
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
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static LuaFsFileUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}