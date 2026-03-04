package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;

public abstract class BaseMassStorageUD extends BaseAcComponent {
    protected String _storageFamilyName = ""; // e.g. hdd or floppy (and maybe ssd?)
    private String storageApiName = "";

    public BaseMassStorageUD(String storageFamilyName, String storageApiName) {
        super("massStorage");
        this._storageFamilyName = storageFamilyName; // hdd or floppy (and maybe ssd?)
        this.storageApiName = storageApiName; // typically managed or unmanaged
    }

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty storageApiType = LuaProperty.ofString(() -> storageApiName, null);

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty storageFamilyName = LuaProperty.ofString(() -> _storageFamilyName, null);

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty diskId = LuaProperty.ofInt(this::getDiskId, null);

    /**
     * This is supposed to return a unique disk id so that computers in minecraft can uniquely identify a disk.
     * The ids may change but there must never be any re-using going on.
     * No two different disks shall at any point use the same id.
     */
    abstract int getDiskId();
}
