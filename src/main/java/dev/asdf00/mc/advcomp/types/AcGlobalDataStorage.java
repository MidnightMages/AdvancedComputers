package dev.asdf00.mc.advcomp.types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class AcGlobalDataStorage extends SavedData {

    private final AtomicInteger nextManagedDiskId = new AtomicInteger(0);
    private final AtomicInteger nextUefiId = new AtomicInteger(0);
    public int getNextFreeManagedDiskId() {
        var rv = nextManagedDiskId.getAndIncrement();
        setDirty();
        return rv;
    }
    public int getNextUefiId() {
        var rv = nextUefiId.getAndIncrement();
        setDirty();
        return rv;
    }

    public AcGlobalDataStorage() {
        setDirty();
    }
    public AcGlobalDataStorage(CompoundTag tag) {
        nextManagedDiskId.set(tag.getInt("nextManagedDiskId"));
        nextUefiId.set(tag.getInt("nextUefiId"));
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt("nextManagedDiskId", nextManagedDiskId.get());
        compoundTag.putInt("nextUefiId", nextUefiId.get());
        return compoundTag;
    }

    public static AcGlobalDataStorage loadOrCreate(DimensionDataStorage dds) {
        return dds.computeIfAbsent(AcGlobalDataStorage::new, AcGlobalDataStorage::new, "advancedComputers");
    }
}
