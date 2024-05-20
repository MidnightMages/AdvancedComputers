package dev.asdf00.mc.advcomp.blocks.keycard_reader;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.items.BaseKeycardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class KeyCardReaderBlockEntity extends BlockEntity {
    public KeyCardReaderBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.KEYCARD_READER_BE.get(), pPos, pBlockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
    }

    void onKeycardSwiped(BaseKeycardItem swipedCard) {
        // maybe do the logic in here, or in the following function
        swipedCard.onKeycardSwiped(swipedCard);
    }

    public void tick(Level pLevel1, BlockPos pPos, BlockState pState1) {

    }
}
