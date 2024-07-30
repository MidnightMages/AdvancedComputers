package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.blocks.cables.CableNetwork;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.IAcCableConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenBlockEntity extends BlockEntity implements MenuProvider, IAcCableConnectable {
    public final ItemStackHandler itemHandler = new ItemStackHandler(2);
    private LazyOptional<IItemHandler> lazyItemhandler = LazyOptional.empty();
    private final LazyOptional<IAcCableConnectable> lazyCableConnectable;
    private CableNetwork cableNetwork;

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        // todo add logic
    }


    public ScreenBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.SCREEN_BE.get(), pPos, pBlockState);

        this.lazyCableConnectable = LazyOptional.of(() -> this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return TranslationMap.GuiTitle("screen_block");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemhandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemhandler.invalidate();
        lazyCableConnectable.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == AcCapabilities.CABLE_CONNECTABLE)
            return lazyCableConnectable.cast();

        return super.getCapability(cap, side);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new ScreenMenu(pContainerId, pPlayerInventory, this);
    }

    protected ComputerBlockEntity getComputerBlockEntity() {
        return (ComputerBlockEntity) level.getBlockEntity(getBlockPos().relative(Direction.DOWN));
    }

    @Override
    public void setNetwork(CableNetwork cableNetwork) {
        this.cableNetwork = cableNetwork;
    }
}
