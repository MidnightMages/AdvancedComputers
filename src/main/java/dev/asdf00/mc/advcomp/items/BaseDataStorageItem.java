package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.lua.components.AcComponentSlotInfo;
import dev.asdf00.mc.advcomp.lua.components.AcItemComponent;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BaseDataStorageItem extends Item implements AcItemComponent {
    protected final int totalCapacityBytes;
    protected final String storageFamilyName;

    public BaseDataStorageItem(String storageFamilyName, int totalCapacityBytes) {
        super(new Properties());
        this.totalCapacityBytes = totalCapacityBytes;
        this.storageFamilyName = storageFamilyName;
    }

    @Override
    public LuaUserDataComponent CreateUserdata(AcComponentSlotInfo slotInfo) {
        return ManagedMassStorageUD.initFromItemStack(storageFamilyName, totalCapacityBytes);
    }
}
