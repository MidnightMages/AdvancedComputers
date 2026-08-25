package dev.asdf00.mc.advcomp.blocks.adapter;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.api.AcAdapterContext;
import dev.asdf00.mc.advcomp.lua.adapterapi.AdapterCompanion;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.function.Supplier;

public class AdapterBlockUD extends BaseAcBlockEntityComponentUD<AdapterBlockEntity> {

    private volatile AdapterCompanion adapterCompanion = AdapterCompanion.EMPTY_COMPANION;

    public AdapterBlockUD(AdapterBlockEntity blockEntity) {
        super("adapter", blockEntity);
    }

    private AdapterBlockUD(LuaVirtualMachine acVm, boolean isAccessible, AdapterBlockEntity blockEntity) {
        super("adapter", acVm, isAccessible, blockEntity);
    }

    public <T> T runOnTickThread(Supplier<T> toExecute) {
        return blockEntity.runOnTickThread(toExecute);
    }

    public void runOnTickThread(Runnable toExecute) {
        runOnTickThread(() -> {
            toExecute.run();
            return null;
        });
    }

    public @NotNull BlockPos getTargetPosition() {
        var direction = Direction.from3DDataValue(this.blockEntity.getBlockState().getValue(AdapterBlock.FACING).get3DDataValue());
        return blockEntity.getBlockPos().relative(direction);
    }

    public void onTargetChanged(Class<? extends Block> blockClass) {
        adapterCompanion = AdapterCompanion.ofBlock(blockClass);
    }

    @Override
    public LuaObject luaGeneralGet(LuaObject key) throws LuaJavaError {
        var adComp = adapterCompanion;
        if (!key.isString()) {
            throw new LuaJavaError("Adapters can only handle keys of type 'string', got '%s'".formatted(key.getTypeAsString()));
        }
        String k = key.getString();
        if (adComp.isGetter(k)) {
            return adComp.get(this, blockEntity.getLevel(), getTargetPosition(), k);
        } else if (adComp.isCallable(k)) {
            return adComp.getFunction(k);
        } else {
            return null;
        }
    }

    @Override
    public boolean luaGeneralSet(LuaObject key, LuaObject value) throws LuaJavaError {
        var adComp = adapterCompanion;
        if (!key.isString()) {
            throw new LuaJavaError("Adapters can only handle keys of type 'string', got '%s'".formatted(key.getTypeAsString()));
        }
        String k = key.getString();
        if (adComp.isSetter(k)) {
            adComp.set(this, blockEntity.getLevel(), getTargetPosition(), k, value);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String[] getExtraReadableUdKeys() {
        return adapterCompanion.readableKeys;
    }

    @Override
    public String[] getExtraWritableUdKeys() {
        return adapterCompanion.writableKeys;
    }

    @LuaCallable
    public String getBlockName() {
        var posToQuery = getTargetPosition();
        var bs = blockEntity.getLevel().getBlockState(posToQuery);
        return "%s@[%s]".formatted(ForgeRegistries.BLOCKS.getKey(bs.getBlock()).toString(), posToQuery.toShortString()); // TODO remove debug coords
    }

    @LuaDeserializer
    public static AdapterBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var rv = genericDeserialize(AdapterBlockEntity.class, AdapterBlockUD::new, objs, reader, postActions, additionalData);
        rv.getBlockEntity().setNewUD(rv);
        return rv;
    }

    public AcAdapterContext validateCall(AdapterCompanion attempted) {
        if (adapterCompanion != attempted) {
            throw new LuaJavaError("The block in front of the Adapter has changed!");
        }
        return new AcAdapterContext(this, blockEntity.getLevel(), getTargetPosition());
    }
}
