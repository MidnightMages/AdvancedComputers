package dev.asdf00.mc.advcomp.blocks.item_Interface;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.BasePeripheralComponentBlockEntity;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ItemInterfaceBlockEntity extends BasePeripheralComponentBlockEntity {

    ConcurrentLinkedQueue<Runnable> tickThreadQueue = new ConcurrentLinkedQueue<>();
    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        while (true) {
            var newItem = tickThreadQueue.poll();
            if (newItem == null)
                return;
            newItem.run();
        }
    }

    public ItemInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.ITEM_INTERFACE_BE.get(), pPos, pBlockState);
    }

    @Override
    public LuaUserDataComponent CreateUserdata() {
        return new ItemInterfaceBlockEntityUD(this);
    }
}
