package dev.asdf00.mc.advcomp.blocks.computer;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.NetCodeUtils.NetworkMessage;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.api.ClusterHostEntity;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.exceptions.ACError;
import dev.asdf00.mc.advcomp.lua.components.AcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.vm.State;
import dev.asdf00.mc.advcomp.types.cluster.ClusterType;
import dev.asdf00.mc.advcomp.utils.NotifyingItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ComputerBlockEntity extends BaseCableConnectableBlockEntity implements MenuProvider, ClusterHostEntity {
    public final NotifyingItemHandler itemHandler;
    private ComputerTier tier;
    private ComputerBlock block;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int computerState = 0;
    private LuaVirtualMachine lvm;
    private final Object lockLVM = new Object();
    private boolean isFirstTick = true;

    // set to STOPPED on first tick to reset block state to indicate stopped LVM
    private final AtomicReference<ComputerBlock.ComputerRunState> newRunState = new AtomicReference<>(ComputerBlock.ComputerRunState.STOPPED);

    public void setRunState(ComputerBlock.ComputerRunState rs) {
        newRunState.set(rs);
    }

    void itemHandler_onSlotChanged(int slot) {
        if (!isServer()) return;
        try {
            if (lvm != null && lvm.getState() == State.RUNNING) { // only notify the running vm
                lvm.onInventorySlotChanged(slot);
            }
        } catch (RuntimeException e) {
            var exceptionAsString = e + "\n" + Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
            AdvancedComputers.LOGGER.error(("An exception occurred during inventory change handling in computer at position %s. " +
                                            "To avoid items being destroyed, the following exception has been swallowed. Please report this." +
                                            "Original exception:\n%s").formatted(getBlockPos(), exceptionAsString));
        }
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        // todo add logic
        int a = 0;
        if (!pLevel.isClientSide()) {
            if (isFirstTick) { // trigger attempting to load vm
                onFirstTick();
                isFirstTick = false;
            }

            var newRstate = newRunState.getAndSet(null);
            if (newRstate != null) {
                // LVM state changed last tick
                var bs = pState.setValue(ComputerBlock.RUN_STATE, newRstate); // TODO distinguish between crash and graceful shutdown
                pLevel.setBlock(pPos, bs, 2);
            }
        }
    }

    public ComputerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.COMPUTER_BE.get(), pPos, pBlockState, Arrays.asList(AdvancedComputers.CLUSTER_TYPE_DEVICE, AdvancedComputers.CLUSTER_TYPE_NETWORK));
        itemHandler = new NotifyingItemHandler(this, ComputerBlockMenu.TE_INVENTORY_SLOT_COUNT(((ComputerBlock) pBlockState.getBlock()).TIER), this::itemHandler_onSlotChanged);
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
    }

    private boolean isServer() {
        return !getLevel().isClientSide();
    }

    public ComputerTier getTier() {
        return tier != null ? tier : ((ComputerBlock) level.getBlockState(getBlockPos()).getBlock()).TIER;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return TranslationMap.GuiTitle("computer_block");
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
        var is = new ItemStack(block.asItem());
        var t = new CompoundTag();
        saveAdditional(t);

        if (!t.isEmpty())
            is.addTagElement("blockData", t);
        Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), is);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new ComputerBlockMenu(pContainerId, pPlayerInventory, this, this.data);
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

        // cannot init the lvm in here, somehow. Need to do it in onLoad() instead.
    }

    @Override
    public void onLoad() {
        super.onLoad();

        // TODO maybe trigger network loading from within here so we get the inventory data and very consistent network data, even on load?


        assert level != null;
        if (!level.isClientSide()) { // OLD INFO: on level load, associated blocks appear to be air instead -- maybe this is totally fine now?
            var b = level.getBlockState(this.getBlockPos()).getBlock();
            if (b instanceof ComputerBlock c) {
                this.block = c;
                this.tier = this.block.TIER;
            } else {
                AdvancedComputers.LOGGER.error("Associated block was not a computer blocK, but instead was somehow %s???".formatted(b.getName()));
            }
            AdvancedComputers.LOGGER.info("ON LOAD COMPUTER Tier: %s".formatted(tier.name()));
        }
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    private void onFirstTick() {
        if (this.lvm == null)
            this.lvm = LuaVirtualMachine.deserializeOrNull(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        // crash LVM
        if (isServer()) {
            if (lvm != null) {
                try {
                    lvm.serialize();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public boolean isHostForNetwork(ClusterType type) {
        return type.equals(AdvancedComputers.CLUSTER_TYPE_DEVICE);
    }

    private Set<BaseCableConnectableBlockEntity> existingBlockComponents = new HashSet<>();
    @Override
    public void onNetworkUpdated() {
        CableCluster deviceCluster = null;
        CableCluster networkCluster = null;
        HashSet<CableCluster> alreadyProcessed = new HashSet<>();
        for (var cluster : connectedNetworks.values()) {
            if(!alreadyProcessed.add(cluster)) // skip already processed clusters as multiple faces may show the *same* one
                continue;

            if (cluster.clusterType == AdvancedComputers.CLUSTER_TYPE_DEVICE) {
                if (deviceCluster != null)
                    throw new IllegalStateException("somehow there were multiple device clusters??");
                deviceCluster = cluster;

            }
            if (cluster.clusterType == AdvancedComputers.CLUSTER_TYPE_NETWORK) {
                if (networkCluster != null)
                    throw new IllegalStateException("somehow there were multiple network clusters??");
                networkCluster = cluster;
            }
        }

        if (deviceCluster != null && lvm != null) {
            var hostCount = deviceCluster.getHostCount();
            if (hostCount > 1) {
                lvm.tryKill("Too many computers connected to this network");

                AdvancedComputers.LOGGER.info("invalid network for computer at bp %s. Computer count: %s"
                        .formatted(this.getBlockPos(), hostCount));
            } else {
                AdvancedComputers.LOGGER.info("valid network for computer at bp %s. Peripheral count: %s"
                        .formatted(this.getBlockPos(), deviceCluster.getEntityCount()));
            }

            if (lvm != null) {
                var newComponents = deviceCluster.connectedEntities.clone();
                Set<BaseCableConnectableBlockEntity> newComponentsSet = Arrays.stream(newComponents).collect(Collectors.toSet());
                for (BaseCableConnectableBlockEntity x : existingBlockComponents.stream().filter(x -> !newComponentsSet.contains(x))
                        .toArray(BaseCableConnectableBlockEntity[]::new))
                    lvm.onBlockComponentRemoved(x);

                for (BaseCableConnectableBlockEntity x : newComponentsSet.stream().filter(x -> !existingBlockComponents.contains(x))
                        .toArray(BaseCableConnectableBlockEntity[]::new)) {
                    if (x instanceof AcBlockEntityComponent acBlockEntityComponent) {
                        lvm.onBlockComponentAdded((BlockEntity & AcBlockEntityComponent) acBlockEntityComponent);
                    }
                }

                existingBlockComponents = newComponentsSet;
            }
        }
    }

    // =================================================================================================================
    //       Lua Interactions     Lua Interactions     Lua Interactions     Lua Interactions     Lua Interactions
    // =================================================================================================================

    // can be triggered before the first tick if, e.g. a screen asks for it
    public LuaVirtualMachine getLvm() {
        if (isServer()) {
            synchronized (lockLVM) {
                if (lvm == null) {
                    lvm = LuaVirtualMachine.deserializeOrNull(this);
                    if (lvm == null)
                        lvm = new LuaVirtualMachine(this);
                }
                return lvm;
            }
        } else {
            return null;
        }
    }

    public void toggleLVMPowerState() {
        if (isServer()) {
            try {
                getLvm().toggleOnOff();
            } catch (Exception e) {
                setRunState(ComputerBlock.ComputerRunState.CRASHED);
                throw e;
            }
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
                    ACError.Assert(cbe.isServer(), "Handling UI button event for ComputerBlockEntity client-side");
                    if (btnId == 1) {
                        cbe.toggleLVMPowerState();
                    }
                } else {
                    AdvancedComputers.LOGGER.warn("Received invalid packet for toggling power state of ComputerBlockEntity");
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
