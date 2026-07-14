package dev.asdf00.mc.advcomp.blocks.mainboard_programmer;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.components.UefiUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;

import java.util.Queue;

public class MainboardProgrammerBlockUD extends BaseAcBlockEntityComponentUD<MainboardProgrammerBlockEntity> {

    public MainboardProgrammerBlockUD(MainboardProgrammerBlockEntity mainboardProgrammerBlockEntity) {
        super("mainboardProgrammer", mainboardProgrammerBlockEntity);
    }

    private MainboardProgrammerBlockUD(LuaVirtualMachine acVm, boolean isAccessible, MainboardProgrammerBlockEntity mainboardProgrammerBlockEntity) {
        super("mainboardProgrammer", acVm, isAccessible, mainboardProgrammerBlockEntity);
    }

    private MainboardItem.MainboardInfo getMainboardInfo() {
        var is = blockEntity.itemHandler.getStackInSlot(0);
        if (is.isEmpty())
            throw new LuaJavaError("no mainboard in slot");

        var mainboardItem = (MainboardItem) is.getItem();
        return mainboardItem.getInfo(is);
    }

    @SuppressWarnings("unused")
    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty containsMainboard = LuaProperty.ofBoolean(() -> !blockEntity.itemHandler.getStackInSlot(0).isEmpty(), null);

    @LuaCallable
    public int getMainboardTier() {
        var mInfo = getMainboardInfo();
        return mInfo.tier().ordinal() + 1;
    }

    @LuaCallable
    public void setUefiData(String data) {
        var mInfo = getMainboardInfo();
        var uefiHelperInstance = new UefiUD(mInfo.uefiId());
        uefiHelperInstance.setUefiScript(data);
    }

    @LuaCallable
    public String getUefiData() {
        var mInfo = getMainboardInfo();
        var uefiHelperInstance = new UefiUD(mInfo.uefiId());
        return uefiHelperInstance.getUefiScript();
    }

    @LuaDeserializer
    public static MainboardProgrammerBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(MainboardProgrammerBlockEntity.class, MainboardProgrammerBlockUD::new, objs, reader, postActions, additionalData);
    }
}
