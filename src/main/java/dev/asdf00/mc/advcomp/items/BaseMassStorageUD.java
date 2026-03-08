package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;

public abstract class BaseMassStorageUD extends BaseAcComponent {

    @LuaExposed(LuaExposed.Policy.READ)
    public String storageFamilyName = ""; // e.g. hdd or floppy (and maybe ssd?)

    @LuaExposed(LuaExposed.Policy.READ)
    public String storageApiType = "";

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty diskId = LuaProperty.ofInt(this::getDiskId, null);

    public BaseMassStorageUD(String storageFamilyName, String storageApiType) {
        super("massStorage");
        this.storageFamilyName = storageFamilyName; // hdd or floppy (and maybe ssd?)
        this.storageApiType = storageApiType; // typically managed or unmanaged
    }

    protected BaseMassStorageUD(LuaVirtualMachine acVm, boolean isAccessible, String storageFamilyName, String storageApiType) {
        super("massStorage", acVm, isAccessible);
        this.storageFamilyName = storageFamilyName; // hdd or floppy (and maybe ssd?)
        this.storageApiType = storageApiType; // typically managed or unmanaged
    }

    /**
     * This is supposed to return a unique disk id so that computers in minecraft can uniquely identify a disk.
     * The ids may change but there must never be any re-using going on.
     * No two different disks shall at any point use the same id.
     */
    abstract int getDiskId();
}
