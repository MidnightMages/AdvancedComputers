package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.mc.advcomp.lua.components.BaseAcComponent;

public abstract class BaseMassStorageUD extends BaseAcComponent {
    private String _storageFamilyName = ""; // e.g. hdd or floppy (and maybe ssd?)
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

}
