package dev.asdf00.mc.advcomp.blocks.computer;


import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.NetCodeUtils.NetworkMessage;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.BaseAcDevCableConnectableEntityBlock;
import dev.asdf00.mc.advcomp.types.IAcCableHostEntity;
import dev.asdf00.mc.advcomp.types.IAcDevCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.asdf00.mc.advcomp.exceptions.AdvancedComputersError.AssertRuntime;

public class ComputerBlockEntity extends BaseAcDevCableConnectableEntityBlock implements MenuProvider, IAcCableHostEntity {
    public final ItemStackHandler itemHandler = new ItemStackHandler(ComputerBlockMenu.TE_INVENTORY_SLOT_COUNT);
    private LazyOptional<IItemHandler> lazyItemhandler = LazyOptional.empty();
    private final LazyOptional<IAcDevCableConnectableEntity> lazyCableConnectable;

    protected final ContainerData data;
    private int computerState = 0;
    private LuaVirtualMachine lvm;
    private final Object lockLVM = new Object();

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        // todo add logic
    }

    public ComputerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.COMPUTER_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> computerState;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> computerState = pValue;
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };

        this.lazyCableConnectable = LazyOptional.of(() -> this);
    }

    private boolean isServer() {
        return !getLevel().isClientSide();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return TranslationMap.GuiTitle("computer_block");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return lazyItemhandler.cast();

        if (cap == AcCapabilities.CABLE_CONNECTABLE)
            return lazyCableConnectable.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemhandler.invalidate();
        lazyCableConnectable.invalidate();
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new ComputerBlockMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(pTag);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        System.out.println("ON LOAD COMPUTER");
        lazyItemhandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        // crash LVM
        if (isServer()) {
            lvm.tryKill("Chunk unloaded");
        }
    }

    public void onNetworkUpdated() {
        if (connectedNetworks.size() > 1) {
            lvm.tryKill("Too many networks connected to thsi computer??");
            AdvancedComputers.LOGGER.warn("invalid network count for computer at bp %s. Network count: %s"
                    .formatted(this.getBlockPos(), connectedNetworks.size()));
        } else if (connectedNetworks.size() == 1) {
            var net = connectedNetworks.iterator().next();
            var cc = net.getHostCount();
            if (cc > 1) {
                if (lvm != null)
                    lvm.tryKill("Too many computers connected to this network"); // TODO make sure lvm checks how many computers are part of this net whne lvm is started, as lvm is null on world load

                AdvancedComputers.LOGGER.info("invalid network for computer at bp %s. Computer count: %s"
                        .formatted(this.getBlockPos(), cc));
            } else {
                AdvancedComputers.LOGGER.info("valid network for computer at bp %s. Peripheral count: %s"
                        .formatted(this.getBlockPos(), net.getEntityCount()));
            }
        }
    }

    // =================================================================================================================
    //       Lua Interactions     Lua Interactions     Lua Interactions     Lua Interactions     Lua Interactions
    // =================================================================================================================

    public LuaVirtualMachine getLvm() {
        if (isServer()) {
            synchronized (lockLVM) {
                if (lvm == null) {
                    lvm = new LuaVirtualMachine(this, Integer.MAX_VALUE);
                }
                return lvm;
            }
        } else {
            return null;
        }
    }

    public void toggleLVMPowerState() {
        if (isServer()) {
            getLvm().toggleOnOff();
        } else {
            NetCodeUtils.sendToServer(new ClientOriginatingUiEvent(this, 1));
        }
    }

    // =================================================================================================================
    //       Lua Events     Lua Events     Lua Events     Lua Events     Lua Events     Lua Events     Lua Events
    // =================================================================================================================

    // =================================================================================================================
    //       Networking     Networking     Networking     Networking     Networking     Networking     Networking
    // =================================================================================================================

    public static class ClientOriginatingUiEvent implements NetworkMessage {
        private final BlockPos cbePos;
        private final int btnId;  // 1 = On/Off-Btn

        public ClientOriginatingUiEvent(ComputerBlockEntity cbe, int btnId) {
            cbePos = cbe.worldPosition;
            this.btnId = btnId;
        }

        private ClientOriginatingUiEvent(BlockPos cbePos, int btnId) {
            this.cbePos = cbePos;
            this.btnId = btnId;
        }

        public static ClientOriginatingUiEvent decode(FriendlyByteBuf buffer) {
            return new ClientOriginatingUiEvent(buffer.readBlockPos(), buffer.readInt());
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(cbePos);
            buffer.writeInt(btnId);
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                var obj = ctx.getSender().level().getBlockEntity(cbePos);
                if (obj instanceof ComputerBlockEntity cbe) {
                    AssertRuntime(cbe.isServer(), "Handling UI button event for ComputerBlockEntity client-side");
                    if (btnId == 1) {
                        cbe.toggleLVMPowerState();
                    }
                } else {
                    AdvancedComputers.LOGGER.warn("Received invalid package for toggling power state of ComputerBlockEntity");
                }
            });
            ctx.setPacketHandled(true);
        }

        @Override
        public String toString() {
            return "ClientOriginatingUiEvent{" +
                    "cbePos=" + cbePos +
                    ", btnId=" + btnId +
                    '}';
        }
    }
}
