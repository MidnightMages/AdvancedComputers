package dev.asdf00.mc.advcomp.blocks.item_Interface;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.LuaHelpers;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ItemInterfaceBlockEntityUD extends BaseAcBlockEntityComponent<ItemInterfaceBlockEntity> {

    public ItemInterfaceBlockEntityUD(ItemInterfaceBlockEntity itemInterfaceBlockEntity) {
        super("itemInterface", itemInterfaceBlockEntity);
    }

    private ItemInterfaceBlockEntityUD(LuaVirtualMachine luaVirtualMachine, boolean isAccessible, ItemInterfaceBlockEntity blockEntity) {
        super("itemInterface", luaVirtualMachine, isAccessible, blockEntity);
    }

    // must be called from tickthread otherwise entity might be null
    private IItemHandler getItemHandlerOnSideOrNull(int sideArgument, int sideArgumentIndex) {
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

    private void argcheckSlotArgument(IItemHandler cap, int slotArgument, int slotArgumentIndex) {
        var maxSlotIdx = cap.getSlots() - 1;
        if (slotArgument < 0 || slotArgument > maxSlotIdx)
            throw new LuaJavaError("slot argument (#%s) is out of range: %s. Expected integer in range [0,%s]"
                    .formatted(slotArgumentIndex + 1, slotArgument, maxSlotIdx));
    }

    private void argcheckRange(int argument, int argumentIndex, int minInclusive, int maxInclusive) {
        if (argument < minInclusive || argument > maxInclusive)
            throw new LuaJavaError("argument (#%s) is out of range: %s. Expected integer in range [%s,%s]"
                    .formatted(argumentIndex + 1, argument, minInclusive, maxInclusive));
    }

    private <T> T runOnTickThread(Supplier<T> toExecute) {
        //noinspection unchecked
        T[] result = (T[]) new Object[1];
        RuntimeException[] resultException = new RuntimeException[1];
        AtomicBoolean complete = new AtomicBoolean(false);
        blockEntity.tickThreadQueue.add(() -> {
            try {
                result[0] = toExecute.get();
            } catch (RuntimeException exception) {
                resultException[0] = exception;
            }
            synchronized (result) {
                complete.set(true);
                result.notifyAll();
            }
        });

        while (true) {
            synchronized (result) {
                try {
                    result.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (complete.get()) {
                    if (resultException[0] != null)
                        throw resultException[0];
                    return result[0];
                }
            }
        }
    }

    @LuaCallable
    public String getNeighborBlockName(int side) {
        var neighborPos = LuaHelpers.getNeighborBlockPosFromSideArgument(blockEntity, side, 0);
        var neighborBlock = Objects.requireNonNull(blockEntity.getLevel()).getBlockState(neighborPos).getBlock();
        return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(neighborBlock)).toString();
    }

    @LuaCallable
    public int getNeighborSlotCount(int side) { // returns 0 if block has no inventory
        var cap = getItemHandlerOnSideOrNull(side, 0);
        return cap == null ? 0 : cap.getSlots();
    }

    @LuaCallable // returns how many items were moved, on success
    public int moveItemStackFromTo(int sideSource, int slotSource, int sideDest, int slotDest) {
        return moveItemStackFromTo(sideSource, slotSource, sideDest, slotDest, 64);
    }

    @LuaCallable // returns how many items were moved, on success
    public int moveItemStackFromTo(int sideSource, int slotSource, int sideDest, int slotDest, int maxAmount) {
        // RUN ON TICK THERAD
        return runOnTickThread(() -> {
            var sourceCap = getItemHandlerOnSideOrNull(sideSource, 0);
            var destCap = getItemHandlerOnSideOrNull(sideDest, 2);

            if (sourceCap == null)
                throw new LuaJavaError("source inventory on side %s was not found".formatted(sideSource));
            if (destCap == null)
                throw new LuaJavaError("source inventory on side %s was not found".formatted(sideDest));

            argcheckRange(maxAmount, 4, 1, 64);
            argcheckSlotArgument(sourceCap, slotSource, 1);
            argcheckSlotArgument(destCap, slotDest, 3);

            var srcStack = sourceCap.extractItem(slotSource, maxAmount, true); // slot, maxAmount, simulate=yes
            var remaining = destCap.insertItem(slotDest, srcStack, false); // slot, stackToInsert, simulate=no
            int itemCountToDestroy = maxAmount - remaining.getCount();
            var removedItemStack = sourceCap.extractItem(slotSource, itemCountToDestroy, false);
            if (removedItemStack.getCount() != itemCountToDestroy) {
                throw new RuntimeException(("it appears we duplicated some items while moving items from item interface " +
                                            "side %s to %s. The item interface is located at %s. Please report this error.")
                        .formatted(sideSource, sideDest, blockEntity.getBlockPos().toString()));
            }
            return itemCountToDestroy;
        });
    }

    @LuaCallable // returns how many items were moved, on success
    public LuaObject getStackInSlot(int side, int slot) {
        // RUN ON TICK THERAD
        var itemStack = runOnTickThread(() -> {
            var sourceCap = getItemHandlerOnSideOrNull(side, 0);

            if (sourceCap == null)
                throw new LuaJavaError("source inventory on side %s was not found".formatted(side));

            argcheckSlotArgument(sourceCap, slot, 1);

            return sourceCap.getStackInSlot(slot);
        });
        String itemName = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(itemStack.getItem())).toString();
        return LuaObject.table(
                LuaObject.of("name"), LuaObject.of(itemName),
                LuaObject.of("stackSize"), LuaObject.of(itemStack.getCount()),
                LuaObject.of("maxStackSize"), LuaObject.of(itemStack.getMaxStackSize()),
                LuaObject.of("damage"), LuaObject.of(itemStack.getDamageValue()),
                LuaObject.of("maxDamage"), LuaObject.of(itemStack.getMaxDamage()),
                LuaObject.of("hasNbt"), LuaObject.of(itemStack.hasTag()),
                LuaObject.of("isEdible"), LuaObject.of(itemStack.isEdible())
        );
    }

    @LuaDeserializer
    public static ItemInterfaceBlockEntityUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(ItemInterfaceBlockEntity.class, ItemInterfaceBlockEntityUD::new, objs, reader, postActions, additionalData);
    }
}
