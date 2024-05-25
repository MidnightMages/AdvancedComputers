package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.lua.IAcComponent;
import dev.asdf00.mc.advcomp.lua.LuaComponentRegistry;
import dev.asdf00.mc.advcomp.lua.components.fs.UnmanagedStorageHandler;
import net.minecraft.world.item.Item;

public class StorageItem extends Item implements IAcComponent {

    private final int totalCapcityBytes;
    private final boolean IsUnmanaged = false;
    private UnmanagedStorageHandler storageComponent;

    public StorageItem(int totalCapcityBytes) {
        super(new Properties());
        this.totalCapcityBytes = totalCapcityBytes;
        storageComponent = new UnmanagedStorageHandler("someid", totalCapcityBytes);
    }

    @Override
    public String getComponentName() {
        return "disk";
    }

    @Override
    public void onRegister(LuaComponentRegistry.LuaFunctionGroup group) {
        group.RegisterMethods(storageComponent);
    }

    @Override
    public void onDeregister(LuaComponentRegistry.LuaFunctionGroup group) {

    }
}
