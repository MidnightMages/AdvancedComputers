package dev.asdf00.mc.advcomp.blocks.punchcard_reader;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.items.punchcard.PunchcardItem;
import dev.asdf00.mc.advcomp.lua.components.BaseAcBlockEntityComponentUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import net.minecraft.world.item.ItemStack;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.IntStream;

public class PunchcardReaderBlockUD extends BaseAcBlockEntityComponentUD<PunchcardReaderBlockEntity> {

    public PunchcardReaderBlockUD(PunchcardReaderBlockEntity punchcardReaderBlockEntity) {
        super("punchcardReader", punchcardReaderBlockEntity);
    }

    private PunchcardReaderBlockUD(LuaVirtualMachine acVm, boolean isAccessible, PunchcardReaderBlockEntity punchcardReaderBlockEntity) {
        super("punchcardReader", acVm, isAccessible, punchcardReaderBlockEntity);
    }

    /***
     * Shift the items in the given slot ids to the end while keeping the order the same. Returns whether the inventory has changed.
     */
    private boolean shiftItemsInSlotsTowardsEnd(int[] slotsToOrganize) {
        var emptySpots = new ArrayBlockingQueue<Integer>(slotsToOrganize.length);
        var inv = blockEntity.itemHandler;
        boolean modified = false;
        for (int srcSlotArrIdx = slotsToOrganize.length - 1; srcSlotArrIdx >= 0; srcSlotArrIdx--) { // start from the end, keep track of empty spots and fill them
            var slotIdx = slotsToOrganize[srcSlotArrIdx];
            var is = inv.getStackInSlot(slotIdx);
            if (is.isEmpty()) { // src is empty --> we found a free spot
                emptySpots.add(slotIdx);
            } else {
                if (!emptySpots.isEmpty()) { // if src is an item and we have a previous free slot, move it there
                    var destSlotId = emptySpots.remove();
                    inv.setStackInSlot(destSlotId, is);
                    inv.setStackInSlot(slotIdx, ItemStack.EMPTY);
                    emptySpots.add(slotIdx);
                    modified = true;
                }
            }
        }
        return modified;
    }

    private int getAvailableOutputSpace_tickThread() {
        return (int) IntStream.rangeClosed(8, 15)
                .filter(i -> blockEntity.itemHandler.getStackInSlot(i).isEmpty())
                .count();
    }

    private boolean shift_tickThread() {
        var inv = blockEntity.itemHandler;
        if (!inv.getStackInSlot(0).isEmpty()) { // item in reading-slot --> move that to the output
            if (getAvailableOutputSpace_tickThread() == 0)
                return false; // output is full --> dont do anything, not even reorganizing input

            var wasMoved = shiftItemsInSlotsTowardsEnd(new int[]{0, 9, 10, 11, 12, 13, 14, 15, 16});
            assert wasMoved;
        }

        shiftItemsInSlotsTowardsEnd(new int[]{8, 7, 6, 5, 4, 3, 2, 1, 0});
        return !inv.getStackInSlot(0).isEmpty();
    }

    @SuppressWarnings("unused")
    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty hasCardInReader = LuaProperty.ofBoolean(
            () -> blockEntity.runOnTickThread(
                    () -> !blockEntity.itemHandler.getStackInSlot(0).isEmpty()
            ), null);
    @SuppressWarnings("unused")
    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty unreadCardCnt = LuaProperty.ofInt(
            () -> blockEntity.runOnTickThread(() -> (int) (
                    IntStream.rangeClosed(1, 8)
                            .filter(i -> !blockEntity.itemHandler.getStackInSlot(i).isEmpty())
                            .count())),
            null);
    @SuppressWarnings("unused")
    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty freeOutSlotCnt = LuaProperty.ofInt(() -> blockEntity.runOnTickThread(this::getAvailableOutputSpace_tickThread), null);

    @LuaCallable
    public boolean shift() { // return true if a new card has been moved into the read slot
        return blockEntity.runOnTickThread(this::shift_tickThread);
    }

    @LuaCallable
    public String read(boolean shiftCards) { //
        if (!hasCardInReader.get().isTruthy() && !shiftCards)
            throw new LuaJavaError("Cannot read punchcard as none is currently in the slot");

        return blockEntity.runOnTickThread(() -> {
                    var inv = blockEntity.itemHandler;
                    if (inv.getStackInSlot(0).isEmpty() && shiftCards) // try to shift first if we cant read right now
                        shift_tickThread();

                    var punchcardToRead = inv.getStackInSlot(0);
                    if (punchcardToRead.isEmpty())
                        throw new LuaJavaError("Cannot read punchcard as no input punchcards are available");
                    // read the contents
                    var rv = PunchcardItem.getData(punchcardToRead);
                    // shift again to prepare for next time
                    if (shiftCards)
                        shift_tickThread();

                    return rv;
                }
        );
    }

    @LuaDeserializer
    public static PunchcardReaderBlockUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return genericDeserialize(PunchcardReaderBlockEntity.class, PunchcardReaderBlockUD::new, objs, reader, postActions, additionalData);
    }
}
