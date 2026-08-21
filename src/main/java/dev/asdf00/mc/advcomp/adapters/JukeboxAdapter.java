package dev.asdf00.mc.advcomp.adapters;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.api.AcAdapter;
import dev.asdf00.mc.advcomp.api.AcAdapterContext;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

@AcAdapter(block = JukeboxBlock.class)
public final class JukeboxAdapter {
    @AcAdapter.PropertyGet
    public static boolean isPlaying(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof JukeboxBlockEntity jbe) {
                return jbe.isRecordPlaying();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
        });
    }

    @AcAdapter.Method
    public void restartPlaying(AcAdapterContext ctx) {
        ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof JukeboxBlockEntity jbe) {
                jbe.startPlaying();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
            return null;
        });
    }

    @AcAdapter.Method
    public void eject(AcAdapterContext ctx) {
        ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof JukeboxBlockEntity jbe) {
                jbe.popOutRecord();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
            return null;
        });
    }

    @AcAdapter.PropertyGet
    public String containedItem(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof JukeboxBlockEntity jbe) {
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

    @AcAdapter.PropertyGet
    public String recordName(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof JukeboxBlockEntity jbe) {
                var firstItem = jbe.getFirstItem();
                if (firstItem.isEmpty())
                    return null;
                if (firstItem.getItem() instanceof RecordItem re) {
                    return re.getDisplayName().getString();
                }
                return firstItem.toString();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
        });
    }
}
