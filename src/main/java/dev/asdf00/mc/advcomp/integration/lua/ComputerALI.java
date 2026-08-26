package dev.asdf00.mc.advcomp.integration.lua;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.api.AcALIContext;
import dev.asdf00.mc.advcomp.api.AcAdapterLuaImplementation;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;

@AcAdapterLuaImplementation(block = ComputerBlock.class)
public final class ComputerALI {
    @AcAdapterLuaImplementation.PropertyGet
    public static String runState(AcALIContext ctx) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlock() instanceof ComputerBlock) {
                return ctx.getBlockState().getValue(ComputerBlock.RUN_STATE).getSerializedName();
            } else {
                throw new LuaJavaError("not pointing at a computer");
            }
        });
    }

    @AcAdapterLuaImplementation.PropertyGet
    public static String crashReason(AcALIContext ctx) {
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
