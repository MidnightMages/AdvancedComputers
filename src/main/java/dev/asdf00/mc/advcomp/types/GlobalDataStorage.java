package dev.asdf00.mc.advcomp.types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class GlobalDataStorage extends SavedData {

    private final AtomicInteger nextUniqueStorageId = new AtomicInteger(0);
    private final AtomicInteger nextUefiId = new AtomicInteger(0);
    public int getNextFreeUniqueStorageId() {
        var rv = nextUniqueStorageId.getAndIncrement();
        setDirty();
        return rv;
    }
    public int getNextUefiId() {
        var rv = nextUefiId.getAndIncrement();
        setDirty();
        return rv;
    }

    public GlobalDataStorage() {
        setDirty();
    }
    public GlobalDataStorage(CompoundTag tag) {
        nextUniqueStorageId.set(tag.getInt("nextManagedDiskId")); // TODO before release? maybe rename this to nextUniqueStorageId
        nextUefiId.set(tag.getInt("nextUefiId"));
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt("nextManagedDiskId", nextUniqueStorageId.get());
        compoundTag.putInt("nextUefiId", nextUefiId.get());
        return compoundTag;
    }

    public static GlobalDataStorage loadOrCreate(DimensionDataStorage dds) {
        return dds.computeIfAbsent(GlobalDataStorage::new, GlobalDataStorage::new, "advancedComputers");
    }
}
