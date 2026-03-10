package dev.asdf00.mc.advcomp.blocks.computer;

public enum ComputerTier {
    Wood(0,1, 64),
    Iron(1,3, 16),
    Diamond(3,5, 4),
    Netherite(3, 6, 1),
    Creative(3, 6, 0);

    public final int diskSlotCount;
    public final int componentSlotCount;
    public final double threadExecutionSleepFactor;

    ComputerTier(int diskSlotCount, int componentSlotCount, double threadExecutionSleepFactor) {
        this.diskSlotCount = diskSlotCount;
        this.componentSlotCount = componentSlotCount;
        this.threadExecutionSleepFactor = threadExecutionSleepFactor;
    }
}
