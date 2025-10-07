package dev.asdf00.mc.advcomp.blocks.computer;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.NetCodeUtils.NetworkMessage;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.api.IAcClusterHostEntity;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.exceptions.ACError;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.IAcDevCableConnectableEntity;
import dev.asdf00.mc.advcomp.types.cluster.AcClusterType;
import dev.asdf00.mc.advcomp.types.cluster.BaseAcCableConnectableBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class ComputerBlockEntity extends BaseAcCableConnectableBlockEntity implements MenuProvider, IAcClusterHostEntity {
    public final NotifyingItemHandler itemHandler = new NotifyingItemHandler(this, ComputerBlockMenu.TE_INVENTORY_SLOT_COUNT, this::itemHandler_onSlotChanged);
    private ComputerTier tier;
    private ComputerBlock block;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final LazyOptional<IAcDevCableConnectableEntity> lazyCableConnectable;

    protected final ContainerData data;
    private int computerState = 0;
    private LuaVirtualMachine lvm;
    private final Object lockLVM = new Object();

    // set to STOPPED on first tick to reset block state to indicate stopped LVM
    private final AtomicReference<ComputerBlock.ComputerRunState> newRunState = new AtomicReference<>(ComputerBlock.ComputerRunState.STOPPED);
    private void SetRunState(ComputerBlock.ComputerRunState rs){
        newRunState.set(rs);
    }

    void itemHandler_onSlotChanged(int slot) {
        if(!isServer()) return;
        if(lvm != null)
            lvm.rebuildUserdataFromInventory();
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        // todo add logic
        int a = 0;
        if (!pLevel.isClientSide()) {
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
            return lazyItemHandler.cast();

        if (cap == AcCapabilities.CABLE_CONNECTABLE)
            return lazyCableConnectable.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyCableConnectable.invalidate();
    }

    public void drops() {
//        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
//        for (int i = 0; i < itemHandler.getSlots(); i++) {
//            inv.setItem(i, itemHandler.getStackInSlot(i));
//        }
//
//        Containers.dropContents(this.level, this.worldPosition, inv);
        var is = new ItemStack(block.asItem());
        var t = new CompoundTag();
        saveAdditional(t);

        if (!t.isEmpty())
            is.addTagElement("blockData", t);
        Containers.dropItemStack(this.level, this.worldPosition.getX(),this.worldPosition.getY(), this.worldPosition.getZ(), is);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new ComputerBlockMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
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
        assert level != null;
        if (!level.isClientSide()) { // on level load, associated blocks appear to be air instead
            var b = level.getBlockState(this.getBlockPos()).getBlock();
            if (b instanceof ComputerBlock c) {
                this.block = c;
                this.tier = this.block.TIER;
            } else {
                AdvancedComputers.LOGGER.error("Associated block was not a computer blocK, but instead was somehow %s???".formatted(b.getName()));
            }
            System.out.println("ON LOAD COMPUTER Tier: %s".formatted(tier.name()));
        }

        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        // crash LVM
        if (isServer()) {
            if (lvm != null) {
                lvm.tryKill("Chunk unloaded", false);
            }
        }
    }

    @Override
    public boolean isNetworkValid(Direction dir) {
        if (!this.connectedNetworks.containsKey(dir))
            return true;

        var netType = this.connectedNetworks.get(dir).getClusterType();
        if (netType == null)
            return true;

        long hostCnt = 0;
        ArrayList<CableCluster> seen = new ArrayList<>();
        for (var cluster : this.connectedNetworks.values()) {
            if (seen.contains(cluster))
                continue;

            seen.add(cluster);
            if (cluster.getClusterType().equals(netType)) {
                hostCnt += Arrays.stream(cluster.connectedEntities).filter(e -> e instanceof ComputerBlockEntity).count();
            }
        }
        return hostCnt <= 1;
    }

    @Override
    public boolean isHostForNetwork(Direction dir, AcClusterType type) {
        return type.equals(AdvancedComputers.CLUSTER_TYPE_DEVICE);
    }

    @Override
    public void onNetworkUpdated(Direction dir) {
        var net = connectedNetworks.get(dir);
        var cc = net.getHostCount();
        if (cc > 1) {
            if (lvm != null)
                lvm.tryKill("Too many computers connected to this network", false); // TODO make sure lvm checks how many computers are part of this net when lvm is started, as lvm is null on world load

            AdvancedComputers.LOGGER.info("invalid network for computer at bp %s. Computer count: %s"
                    .formatted(this.getBlockPos(), cc));
        } else {
            AdvancedComputers.LOGGER.info("valid network for computer at bp %s. Peripheral count: %s"
                    .formatted(this.getBlockPos(), net.getEntityCount()));
        }
    }

    @Override
    public Direction getWorldOrientation() {
        return getLevel().getBlockState(getBlockPos()).getValue(ComputerBlock.FACING);
    }

    // =================================================================================================================
    //       Lua Interactions     Lua Interactions     Lua Interactions     Lua Interactions     Lua Interactions
    // =================================================================================================================

    public LuaVirtualMachine getLvm() {
        if (isServer()) {
            synchronized (lockLVM) {
                if (lvm == null) {
                    lvm = new LuaVirtualMachine(this, Integer.MAX_VALUE);
                    lvm.rebuildUserdataFromInventory();
                }
                return lvm;
            }
        } else {
            return null;
        }
    }

    private void setBlockStates(ComputerBlock.ComputerRunState newState){

    }

    public void toggleLVMPowerState() {
        if (isServer()) {
            getLvm().toggleOnOff(() -> {
                        var bs = level.getBlockState(getBlockPos()).setValue(ComputerBlock.RUN_STATE, ComputerBlock.ComputerRunState.RUNNING);
                        level.setBlock(getBlockPos(), bs, 2);
                    },
                    (gracefulShutdown) -> SetRunState(gracefulShutdown ? ComputerBlock.ComputerRunState.STOPPED :  ComputerBlock.ComputerRunState.CRASHED));
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
