package dev.asdf00.mc.advcomp.integration.lua;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.api.AcALIContext;
import dev.asdf00.mc.advcomp.api.AcAdapterLuaImplementation;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

@AcAdapterLuaImplementation(block = JukeboxBlock.class)
public final class JukeboxALI {
    @AcAdapterLuaImplementation.PropertyGet
    public static boolean isPlaying(AcALIContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof JukeboxBlockEntity jbe) {
                return jbe.isRecordPlaying();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
        });
    }

    @AcAdapterLuaImplementation.Method
    public void restartPlaying(AcALIContext ctx) {
        ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof JukeboxBlockEntity jbe) {
                jbe.startPlaying();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
            return null;
        });
    }

    @AcAdapterLuaImplementation.Method
    public void eject(AcALIContext ctx) {
        ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof JukeboxBlockEntity jbe) {
                jbe.popOutRecord();
            } else {
                throw new LuaJavaError("not pointing at a jukebox");
            }
            return null;
        });
    }

    @AcAdapterLuaImplementation.PropertyGet
    public String containedItem(AcALIContext ctx) {
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

    @AcAdapterLuaImplementation.PropertyGet
    public String recordName(AcALIContext ctx) {
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
