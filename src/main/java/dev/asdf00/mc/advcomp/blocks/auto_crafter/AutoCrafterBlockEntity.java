package dev.asdf00.mc.advcomp.blocks.auto_crafter;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.api.ItemCanBeInitialized;
import dev.asdf00.mc.advcomp.blocks.BasePeripheralComponentBlockEntity;
import dev.asdf00.mc.advcomp.items.MainboardItem;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.utils.NotifyingItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoCrafterBlockEntity extends BasePeripheralComponentBlockEntity implements MenuProvider {
    public final NotifyingItemHandler itemHandler = new NotifyingItemHandler(this, AutoCrafterBlockMenu.TE_INVENTORY_SLOT_COUNT,
            (slotIdx, itemStack) -> slotIdx == 0 && (itemStack.getItem() instanceof MainboardItem)  ? 1 : 0,
            this::itemHandler_onSlotChanged
    );
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    protected final ContainerData data;

    public AutoCrafterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.AUTO_CRAFTER_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                }
            }

            @Override
            public int getCount() {
                return 0;
            }
        };
    }

    void itemHandler_onSlotChanged(int slot) {
        if(getLevel().isClientSide()) return;

        var is = itemHandler.getStackInSlot(slot);
        if (is.getItem() instanceof ItemCanBeInitialized cbi) {
            cbi.initialize(is);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return TranslationMap.GuiTitle("auto_crafter_block");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return lazyItemHandler.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }

        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new AutoCrafterBlockMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        itemHandler.saveContents(pTag);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        itemHandler.loadContents(pTag);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public LuaUserDataComponent createUserdata() {
        return new AutoCrafterBlockUD(this);
    }
}
