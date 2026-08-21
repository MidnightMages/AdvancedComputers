package dev.asdf00.mc.advcomp.adapters;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.api.AcAdapter;
import dev.asdf00.mc.advcomp.api.AcAdapterContext;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.NoteBlock;

@AcAdapter(block = JukeboxBlock.class)
public final class NoteblockAdapter {
    @AcAdapter.PropertyGet
    public static String instrument(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlock() instanceof NoteBlock) {
                return ctx.getBlockState().getValue(NoteBlock.INSTRUMENT).getSerializedName();
            } else {
                throw new LuaJavaError("not pointing at a noteblock");
            }
        });
    }

    @AcAdapter.PropertyGet
    public static int notePitch(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlock() instanceof NoteBlock) {
                return ctx.getBlockState().getValue(NoteBlock.NOTE);
            } else {
                throw new LuaJavaError("not pointing at a noteblock");
            }
        });
    }

    @AcAdapter.PropertySet
    public static void notePitch(AcAdapterContext ctx, int pitch) {
        ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlock() instanceof NoteBlock) {
                if (!NoteBlock.NOTE.getPossibleValues().contains(pitch)) {
                    throw new LuaJavaError("Pitch '%s' is not valid for this noteblock.".formatted(pitch));
                }
                ctx.lvl().setBlock(ctx.pos(), ctx.getBlockState().setValue(NoteBlock.NOTE, pitch), 3);
            } else {
                throw new LuaJavaError("not pointing at a noteblock");
            }
        });
    }
}
