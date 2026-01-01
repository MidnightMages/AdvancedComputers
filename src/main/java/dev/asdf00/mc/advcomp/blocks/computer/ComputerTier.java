package dev.asdf00.mc.advcomp.blocks.computer;

public enum ComputerTier {
    Wood(0,1),
    Iron(1,3),
    Diamond(3,5),
    Netherite(3,6);

    public final int diskSlotCount;
    public final int componentSlotCount;

    ComputerTier(int diskSlotCount, int componentSlotCount) {
        this.diskSlotCount = diskSlotCount;
        this.componentSlotCount = componentSlotCount;
    }
}
