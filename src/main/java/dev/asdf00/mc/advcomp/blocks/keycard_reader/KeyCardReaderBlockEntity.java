package dev.asdf00.mc.advcomp.blocks.keycard_reader;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.items.BaseKeycardItem;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.IAcCableConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KeyCardReaderBlockEntity extends BlockEntity implements IAcCableConnectable {
    private final LazyOptional<IAcCableConnectable> lazyCableConnectable;

    public KeyCardReaderBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.KEYCARD_READER_BE.get(), pPos, pBlockState);

        this.lazyCableConnectable = LazyOptional.of(() -> this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == AcCapabilities.CABLE_CONNECTABLE)
            return lazyCableConnectable.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyCableConnectable.invalidate();
    }

    void onKeycardSwiped(BaseKeycardItem swipedCard) {
        // maybe do the logic in here, or in the following function
        swipedCard.onKeycardSwiped(swipedCard);
    }

    public void tick(Level pLevel1, BlockPos pPos, BlockState pState1) {

    }
}
