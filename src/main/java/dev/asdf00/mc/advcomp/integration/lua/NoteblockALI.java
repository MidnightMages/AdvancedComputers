package dev.asdf00.mc.advcomp.integration.lua;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.api.AcALIContext;
import dev.asdf00.mc.advcomp.api.AcAdapterLuaImplementation;
import net.minecraft.world.level.block.NoteBlock;

@AcAdapterLuaImplementation(block = NoteBlock.class)
public final class NoteblockALI {
    @AcAdapterLuaImplementation.PropertyGet
    public static String instrument(AcALIContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlock() instanceof NoteBlock) {
                return ctx.getBlockState().getValue(NoteBlock.INSTRUMENT).getSerializedName();
            } else {
                throw new LuaJavaError("not pointing at a noteblock");
            }
        });
    }

    @AcAdapterLuaImplementation.PropertyGet
    public static int notePitch(AcALIContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlock() instanceof NoteBlock) {
                return ctx.getBlockState().getValue(NoteBlock.NOTE);
            } else {
                throw new LuaJavaError("not pointing at a noteblock");
            }
        });
    }

    @AcAdapterLuaImplementation.PropertySet
    public static void notePitch(AcALIContext ctx, int pitch) {
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
