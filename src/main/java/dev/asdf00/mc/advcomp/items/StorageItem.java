package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.lua.components.fs.UnmanagedStorageHandler;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

public class StorageItem extends Item implements LuaUserDataComponent {

    private final int totalCapcityBytes;
    private final boolean IsUnmanaged = false;
    private UnmanagedStorageHandler storageComponent;

    public StorageItem(int totalCapcityBytes) {
        super(new Properties());
        this.totalCapcityBytes = totalCapcityBytes;
        storageComponent = new UnmanagedStorageHandler("someid", totalCapcityBytes);
    }

    @Override
    public String getComponentType() {
        return "disk";
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static StorageItem todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
