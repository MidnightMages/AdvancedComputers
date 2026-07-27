package dev.asdf00.mc.advcomp.blocks.adapter.redstone_io;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraft.core.Direction;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Queue;

public class AdapterBlockUD extends BaseAcBlockEntityComponentUD<AdapterBlockEntity> {

    public AdapterBlockUD(AdapterBlockEntity blockEntity) {
        super("adapter", blockEntity);
    }

    private AdapterBlockUD(LuaVirtualMachine acVm, boolean isAccessible, AdapterBlockEntity blockEntity) {
        super("adapter", acVm, isAccessible, blockEntity);
    }

    @LuaCallable
    public String getBlockName() {
        var direction = Direction.from3DDataValue(this.blockEntity.getBlockState().getValue(AdapterBlock.FACING).get3DDataValue());
        var posToQuery = blockEntity.getBlockPos().relative(direction);
        var bs = blockEntity.getLevel().getBlockState(posToQuery);
        return "%s@[%s]".formatted(ForgeRegistries.BLOCKS.getKey(bs.getBlock()).toString(), posToQuery.toShortString()); // TODO remove debug coords
    }

    @LuaCallable
    public LuaUserData getBlockUD() {

    }

    @LuaBackedBy
    public LuaUserData[] backing = null;


    @LuaDeserializer
    public static AdapterBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(AdapterBlockEntity.class, AdapterBlockUD::new, objs, reader, postActions, additionalData);
    }
}
