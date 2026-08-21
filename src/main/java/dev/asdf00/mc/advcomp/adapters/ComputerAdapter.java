package dev.asdf00.mc.advcomp.adapters;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.api.AcAdapter;
import dev.asdf00.mc.advcomp.api.AcAdapterContext;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import net.minecraft.world.level.block.JukeboxBlock;

@AcAdapter(block = JukeboxBlock.class)
public final class ComputerAdapter {
    @AcAdapter.PropertyGet
    public static String runState(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlock() instanceof ComputerBlock) {
                return ctx.getBlockState().getValue(ComputerBlock.RUN_STATE).getSerializedName();
            } else {
                throw new LuaJavaError("not pointing at a computer");
            }
        });
    }

    @AcAdapter.PropertyGet
    public static String crashReason(AcAdapterContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof ComputerBlockEntity cbe) {
                var lvm = cbe.getLvm();
                return lvm == null ? null : lvm.stopCode;
            } else {
                throw new LuaJavaError("not pointing at a computer");
            }
        });
    }
}
