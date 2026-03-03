package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class NvramUD extends BaseAcComponent {
    public NvramUD() {
        super("nvram");
    }

    private final HashMap<String, LuaObject> backing = new HashMap<>();

    @Override
    public LuaObject luaGeneralGet(LuaObject key) throws LuaJavaError {
        if (!key.isString())
            return null;

        return backing.getOrDefault(key.asString(), null);
    }

    @Override
    public boolean luaGeneralSet(LuaObject key, LuaObject value) throws LuaJavaError {
        if (!key.isString())
            throw new LuaJavaError("Only string keys are supported. Not keys of type %s!".formatted(key.getTypeAsString()));
        if (!value.isType(LuaObject.Types.NUMBER | LuaObject.Types.STRING | LuaObject.Types.NIL | LuaObject.Types.BOOLEAN))
            throw new LuaJavaError("Only primitive, immutable values are supported (number, string, nil, bool). Not values of type %s!".formatted(value.getTypeAsString()));

        if (value.isNil())
            backing.remove(key.asString());
        else
            backing.put(key.asString(), value);
        return true;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        var bdr = new ByteArrayBuilder();
        for (var entry : backing.entrySet()) {
            bdr.append(entry.getKey()).append(entry.getValue().serialize(serialData, mappedObjs, additionalData));
        }
        return bdr.toArray();
    }

    @LuaDeserializer
    public static NvramUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var nu = new NvramUD();
        while (reader.remaining() > 0) {
            nu.backing.put(reader.readString(), objs[reader.readInt()]);
        }
        return nu;
    }
}
