package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.components.IsAssociatedWithLuaUserdata;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.lua.components.fs.LuaFsFileUD;
import dev.asdf00.mc.advcomp.lua.components.fs.ManagedStorageHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DiskItem extends Item implements IsAssociatedWithLuaUserdata {
    private final boolean IsUnmanaged = false;
    private final int totalCapcityBytes;
    public DiskItem(int totalCapcityBytes) {
        super(new Properties());
        this.totalCapcityBytes = totalCapcityBytes;
//        storageComponent = new UnmanagedStorageHandler("someid", totalCapcityBytes);
    }

    @Override
    public LuaUserDataComponent CreateUserdata(ItemStack stack) {
        return new DiskItemUD(stack);
    }
}
