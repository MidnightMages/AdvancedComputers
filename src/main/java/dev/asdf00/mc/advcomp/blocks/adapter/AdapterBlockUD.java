package dev.asdf00.mc.advcomp.blocks.adapter;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Queue;
import java.util.function.Supplier;

public class AdapterBlockUD extends BaseAcBlockEntityComponentUD<AdapterBlockEntity> {

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

    @LuaCallable
    public String getBlockName() {
        var posToQuery = getTargetPosition();
        var bs = blockEntity.getLevel().getBlockState(posToQuery);
        return "%s@[%s]".formatted(ForgeRegistries.BLOCKS.getKey(bs.getBlock()).toString(), posToQuery.toShortString()); // TODO remove debug coords
    }

    public @NotNull BlockPos getTargetPosition() {
        var direction = Direction.from3DDataValue(this.blockEntity.getBlockState().getValue(AdapterBlock.FACING).get3DDataValue());
        return blockEntity.getBlockPos().relative(direction);
    }

    @Override
    public LuaObject luaGeneralGet(LuaObject key) throws LuaJavaError {
        return super.luaGeneralGet(key);
    }

    public LuaObject[] generalCall(String key, LuaObject... args) {
        return null;
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

    @LuaCallable
    public String getRecordName() {
        return this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var be = this.blockEntity.getLevel().getBlockEntity(posToQuery);
            if (be instanceof JukeboxBlockEntity jbe) {
                var firstItem = jbe.getFirstItem();
                if (firstItem.isEmpty())
                    return null;
                if (firstItem.getItem() instanceof RecordItem re) {
                    return re.getDisplayName().getString();
                }
                return jbe.getFirstItem().toString();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
        });
    }

    @LuaCallable
    public void restartPlaying() {
        this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var be = this.blockEntity.getLevel().getBlockEntity(posToQuery);
            if (be instanceof JukeboxBlockEntity jbe) {
                jbe.startPlaying();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
            return null;
        });
    }


    @LuaCallable
    public String nb_getInstrument() {
        return this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var bs = this.blockEntity.getLevel().getBlockState(posToQuery);
            if (bs.getBlock() instanceof NoteBlock) {
                return bs.getValue(NoteBlock.INSTRUMENT).getSerializedName();
            } else {
                throw new LuaJavaError("not pointing at a noteblock");
            }
        });
    }

    @LuaCallable
    public int nb_getNotePitch() {
        return this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var bs = this.blockEntity.getLevel().getBlockState(posToQuery);
            if (bs.getBlock() instanceof NoteBlock) {
                return bs.getValue(NoteBlock.NOTE);
            } else {
                throw new LuaJavaError("not pointing at a noteblock");
            }
        });
    }

    @LuaCallable
    public void nb_setNotePitch(int x) {
        this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var bs = this.blockEntity.getLevel().getBlockState(posToQuery);
            if (bs.getBlock() instanceof NoteBlock) {
                if (!NoteBlock.NOTE.getPossibleValues().contains(x)) {
                    throw new LuaJavaError("Pitch %s is not valid for this noteblock.".formatted(x));
                }
                blockEntity.getLevel().setBlock(posToQuery, bs.setValue(NoteBlock.NOTE, x), 3);
            } else {
                throw new LuaJavaError("not pointing at a noteblock");
            }
            return null;
        });
    }

    @LuaCallable
    public String c_getRunState() {
        return this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var bs = this.blockEntity.getLevel().getBlockState(posToQuery);
            if (bs.getBlock() instanceof ComputerBlock) {
                return bs.getValue(ComputerBlock.RUN_STATE).getSerializedName();
            } else {
                throw new LuaJavaError("not pointing at a computer");
            }
        });
    }

    @LuaCallable
    public LuaObject c_getCrashReason() {
        var rv = this.blockEntity.runOnTickThread(() -> {
            var posToQuery = getTargetPosition();
            var be = this.blockEntity.getLevel().getBlockEntity(posToQuery);
            if (be instanceof ComputerBlockEntity cbe) {
                var lvm = cbe.getLvm();
                return lvm == null ? null : lvm.stopCode;
            } else {
                throw new LuaJavaError("not pointing at a computer");
            }
        });
        return rv == null ? LuaObject.NIL : LuaObject.of(rv);
    }

    @LuaDeserializer
    public static AdapterBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(AdapterBlockEntity.class, AdapterBlockUD::new, objs, reader, postActions, additionalData);
    }
}
