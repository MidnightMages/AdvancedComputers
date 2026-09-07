package dev.asdf00.mc.advcomp.integration.lua;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.block.misc.InterfaceBlock;
import appeng.blockentity.misc.InterfaceBlockEntity;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.api.AcALIContext;
import dev.asdf00.mc.advcomp.api.AcAdapterLuaImplementation;
import dev.asdf00.mc.advcomp.lua.LuaHelpers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

@AcAdapterLuaImplementation(block = InterfaceBlock.class)
public class Ae2InterfaceALI {
    private static <T> T ae2Interact(AcALIContext ctx, Function<IStorageService, T> action) {
        return ctx.adapter().runOnTickThread(() -> {
            if (ctx.getBlockEntity() instanceof InterfaceBlockEntity ae2if) {
                return action.apply(ae2if.getGridNode().getGrid().getStorageService());
            } else {
                throw new LuaJavaError("not pointing at ae2 interface");
            }
        });
    }

    @SuppressWarnings("unused")
    @AcAdapterLuaImplementation.Method
    public static LuaObject search(AcALIContext ctx, String partialName) {
        HashMap<String, Long> itemCounts = ae2Interact(ctx, storage -> {
            var kc = new KeyCounter();
            storage.getInventory().getAvailableStacks(kc);
            var counts = new HashMap<String, Long>();
            for (var kv : kc) {
                var name = kv.getKey().getId().toString();
                if (name.contains(partialName)) {
                    counts.put(name, kv.getLongValue());
                    if (counts.size() > 1000) {
                        break;
                    }
                }
            }
            return counts;
        });
        var rv = LuaObject.table();
        for (var kv : itemCounts.keySet()) {
            rv.set(kv, LuaObject.of(itemCounts.get(kv)));
        }
        return rv;
    }

    @SuppressWarnings("unused")
    @AcAdapterLuaImplementation.Method
    public static long getCount(AcALIContext ctx, String fullName) {
        return ae2Interact(ctx, storage -> {
            var kc = new KeyCounter();
            var rl = ResourceLocation.tryParse(fullName);
            if (rl == null)
                throw new LuaJavaError("item '%s' was not found".formatted(fullName));
            var rl2 = ForgeRegistries.ITEMS.getValue(rl);
            if (rl2 == null || rl2.equals(Items.AIR))
                throw new LuaJavaError("item '%s' was not found".formatted(fullName));

            var key = AEItemKey.of(rl2);
            storage.getInventory().getAvailableStacks(kc);
            return kc.get(key);
        });
    }

    @SuppressWarnings("unused")
    @AcAdapterLuaImplementation.Method
    public static int extract(AcALIContext ctx, String fullName, int direction, int maxQuantity) {
        var cappedQuantity = Math.min(maxQuantity, 64);
        return ae2Interact(ctx, storage -> {
            var ih = getItemHandlerOnSideOrNull(ctx, direction, 2);
            if (ih == null) {
                throw new LuaJavaError("Destination block does not have an inventory");
            }

            var kc = new KeyCounter();
            var rl = ResourceLocation.tryParse(fullName);
            if (rl == null)
                throw new LuaJavaError("item '%s' was not found".formatted(fullName));
            var rl2 = ForgeRegistries.ITEMS.getValue(rl);
            if (rl2 == null || rl2.equals(Items.AIR))
                throw new LuaJavaError("item '%s' was not found".formatted(fullName));

            var key = AEItemKey.of(rl2);
            storage.getInventory().getAvailableStacks(kc);

            var maxAe2Available = Math.min(kc.get(key), maxQuantity);
            var destStack = new ItemStack(rl2, 1);
            var maxToTransfer = Math.min(maxAe2Available, destStack.getMaxStackSize());
            maxToTransfer = storage.getInventory().extract(key, maxToTransfer, Actionable.SIMULATE, IActionSource.empty());
            destStack.setCount((int) maxToTransfer);

            for (int i = 0; i < ih.getSlots(); i++) {
                if (destStack.isEmpty())
                    break;
                destStack = ih.insertItem(i, destStack, false);
            }

            var transferredItemCount = (int) maxToTransfer - destStack.getCount();
            storage.getInventory().extract(key, transferredItemCount, Actionable.MODULATE, IActionSource.empty());

            return transferredItemCount;
        });
    }

    // must be called from tickthread otherwise entity might be null
    private static IItemHandler getItemHandlerOnSideOrNull(AcALIContext ctx, int sideArgument, int sideArgumentIndex) {
        var blockEntity = ctx.getBlockEntity();
        var neighborPos = LuaHelpers.getNeighborBlockPosFromSideArgument(blockEntity, sideArgument, sideArgumentIndex);
        var neighborBe = Objects.requireNonNull(blockEntity.getLevel()).getBlockEntity(neighborPos);
        if (neighborBe != null) {
            var itemHandlerCap = neighborBe.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (itemHandlerCap.isPresent()) {
                //noinspection OptionalGetWithoutIsPresent
                return itemHandlerCap.resolve().get();
            }
        }
        return null;
    }
}
