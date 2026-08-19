package dev.asdf00.mc.advcomp.adapters;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.api.AcAdapter;
import dev.asdf00.mc.advcomp.api.AcAdapterContext;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import java.util.Objects;

@AcAdapter(block = JukeboxBlock.class)
public final class JukeboxAdapter {
    @AcAdapter.PropertyGet
    public static boolean isPlaying(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            var be = ctx.lvl().getBlockEntity(ctx.pos());
            if (be instanceof JukeboxBlockEntity jbe) {
                return jbe.isRecordPlaying();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
        });
    }

    @AcAdapter.PropertyGet
    public String containedItem(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            var be = ctx.lvl().getBlockEntity(ctx.pos());
            if (be instanceof JukeboxBlockEntity jbe) {
                var is = jbe.getTheItem();
                if (is.isEmpty()) {
                    return "";
                }
                return Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(is.getItem())).toString();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
        });
    }

    @AcAdapter.Method
    public void eject(AcAdapterContext ctx) {
        ctx.adapter().runOnTickThread(() -> {
            var be = ctx.lvl().getBlockEntity(ctx.pos());
            if (be instanceof JukeboxBlockEntity jbe) {
                jbe.popOutRecord();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
            return null;
        });
    }
}
