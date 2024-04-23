package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.lua.IAcComponent;
import net.minecraft.world.item.Item;

public class StorageItem extends Item implements IAcComponent {

    private final int totalCapcityBytes;
    private final boolean IsUnmanaged = false;

    public StorageItem(int totalCapcityBytes) {
        super(new Properties());
        this.totalCapcityBytes = totalCapcityBytes;
    }

    @Override
    public String getComponentName() {
        return "disk";
    }

    @Override
    public void onRegister() {

    }

    @Override
    public void onDeregister() {

    }
}
