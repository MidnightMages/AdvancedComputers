package dev.asdf00.mc.advcomp.blocks.keycard_reader;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BasePeripheralComponentBlockEntity;
import dev.asdf00.mc.advcomp.items.BaseKeycardItem;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class KeyCardReaderBlockEntity extends BasePeripheralComponentBlockEntity {

    public KeyCardReaderBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.KEYCARD_READER_BE.get(), pPos, pBlockState);
    }

    void onKeycardSwiped(BaseKeycardItem swipedCard) {
        // maybe do the logic in here, or in the following function
        swipedCard.onKeycardSwiped(swipedCard);
    }

    @Override
    public LuaUserDataComponent createUserdata() {
        return new KeyCardReaderBlockEntityUD(this);
    }
}
