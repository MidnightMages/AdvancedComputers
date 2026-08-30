package dev.asdf00.mc.advcomp.integration.lua;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.block.misc.InterfaceBlock;
import appeng.blockentity.misc.InterfaceBlockEntity;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.api.AcALIContext;
import dev.asdf00.mc.advcomp.api.AcAdapterLuaImplementation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

@AcAdapterLuaImplementation(block = InterfaceBlock.class)
public class Ae2InterfaceALI {
    //@AcAdapterLuaImplementation.TargetBlock
    //public static final Block block =  ForgeRegistries.BLOCKS.getValue(AEBlockIds.INTERFACE);

    @AcAdapterLuaImplementation.Method
    public static void search(AcALIContext ctx, String partialName) {

    }

    @AcAdapterLuaImplementation.Method
    public static long getCount(AcALIContext ctx, String fullName) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof InterfaceBlockEntity ae2if) {
                var kc = new KeyCounter();
                var rl = ResourceLocation.tryParse(fullName);
                var rl2 = ForgeRegistries.ITEMS.getValue(rl);
                var key = AEItemKey.of(rl2);
                ae2if.getGridNode().getGrid().getStorageService().getInventory().getAvailableStacks(kc);
                return kc.get(key);
            } else {
                throw new LuaJavaError("not pointing at ae2 interface");
            }
        });
    }

    @AcAdapterLuaImplementation.Method
    public static void extract(AcALIContext ctx, String fullName, int direction, int quantity) {

    }
}
