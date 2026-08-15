package dev.asdf00.mc.advcomp.blocks.adapter;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
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
        var posToQuery = getTargetPosition();
        var bs = blockEntity.getLevel().getBlockState(posToQuery);
        return "%s@[%s]".formatted(ForgeRegistries.BLOCKS.getKey(bs.getBlock()).toString(), posToQuery.toShortString()); // TODO remove debug coords
    }

    private @NotNull BlockPos getTargetPosition() {
        var direction = Direction.from3DDataValue(this.blockEntity.getBlockState().getValue(AdapterBlock.FACING).get3DDataValue());
        return blockEntity.getBlockPos().relative(direction);
    }

    @LuaCallable
    public void eject() {
        this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var be = this.blockEntity.getLevel().getBlockEntity(posToQuery);
            if (be instanceof JukeboxBlockEntity jbe) {
                jbe.popOutRecord();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
            return null;
        });
    }

    @LuaCallable
    public String getContainedItem() {
        return this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var be = this.blockEntity.getLevel().getBlockEntity(posToQuery);
            if (be instanceof JukeboxBlockEntity jbe) {
                var is = jbe.getFirstItem();
                if (is.isEmpty()) {
                    return "";
                }
                return Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(is.getItem())).toString();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
        });
    }

    @LuaCallable
    public boolean getIsPlaying() {
        return this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var be = this.blockEntity.getLevel().getBlockEntity(posToQuery);
            if (be instanceof JukeboxBlockEntity jbe) {
                return jbe.isRecordPlaying();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
        });
    }


    @LuaDeserializer
    public static AdapterBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(AdapterBlockEntity.class, AdapterBlockUD::new, objs, reader, postActions, additionalData);
    }
}
