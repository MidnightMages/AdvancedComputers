package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.lua.components.AcItemComponent;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DiskItem extends Item implements AcItemComponent {
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
