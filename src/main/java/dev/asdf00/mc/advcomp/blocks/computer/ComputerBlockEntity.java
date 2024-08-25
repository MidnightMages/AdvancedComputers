package dev.asdf00.mc.advcomp.blocks.computer;


import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.IAcCableConnectable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static dev.asdf00.mc.advcomp.exceptions.AdvancedComputersError.AssertInit;
import static dev.asdf00.mc.advcomp.exceptions.AdvancedComputersError.AssertRuntime;

public class ComputerBlockEntity extends BlockEntity implements MenuProvider, IAcCableConnectable {
    public final ItemStackHandler itemHandler = new ItemStackHandler(ComputerBlockMenu.TE_INVENTORY_SLOT_COUNT);
    private LazyOptional<IItemHandler> lazyItemhandler = LazyOptional.empty();
    private final LazyOptional<IAcCableConnectable> lazyCableConnectable;

    protected final boolean isServer;
    protected final ContainerData data;
    private int computerState = 0;
    private LuaVirtualMachine lvm;
    private final Object lockLVM = new Object();

    private static final String PROTOCOL_VERSION = "0.1.a";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AdvancedComputers.MODID, "comp_be_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        // todo add logic
    }

    public ComputerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.COMPUTER_BE.get(), pPos, pBlockState);
        isServer = !getLevel().isClientSide();
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
        // create LVM
        lvm = new LuaVirtualMachine(this, Integer.MAX_VALUE);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        // crash LVM
        lvm.tryKill("Chunk unloaded");
    }

    // =================================================================================================================
    //       Lua Interactions     Lua Interactions     Lua Interactions     Lua Interactions     Lua Interactions
    // =================================================================================================================

    @Deprecated
    public LuaVirtualMachine getLvm() {
        return lvm;
    }

    public void startLVM() {
        if (isServer) {
            // actually start LVM
            synchronized (lockLVM) {
                lvm = new LuaVirtualMachine(this, Integer.MAX_VALUE);
                lvm.startIfOff();
            }
        } else {
            // send packet to server

        }
    }

    // =================================================================================================================
    //       Lua Events     Lua Events     Lua Events     Lua Events     Lua Events     Lua Events     Lua Events
    // =================================================================================================================

    private record EventCode(BiConsumer<FriendlyByteBuf, Object> encoder, Function<FriendlyByteBuf, Object> decoder) {
    }

    private static final HashMap<String, EventCode> CLIENT_EVENT_ENCODERS = new HashMap<>();

    public void raiseClientMachineEvent(String eventName, Object event) {
        var coder = CLIENT_EVENT_ENCODERS.get(eventName);
        AssertRuntime(coder != null, () -> "Event '%s' was never registered but was raised!".formatted(eventName));
        var msg = new AcC2SEventPacket(coder.encoder(), getBlockPos(), eventName, event);
        INSTANCE.sendToServer(msg);
    }

    public void raiseServerMachineEvent(String eventName, Object event) {
        AssertRuntime(isServer, () -> "Trying to handle server-side machine '%s' event on client!".formatted(eventName));
        // TODO: send event to LVM

    }

    // ---- setup methods ----

    public static void registerClientMachineEvent(String eventName, BiConsumer<FriendlyByteBuf, Object> encoder, Function<FriendlyByteBuf, Object> decoder) {
        var prev = CLIENT_EVENT_ENCODERS.put(eventName, new EventCode(encoder, decoder));
        AssertInit(prev == null, () -> "Event '%s' was registered twice!".formatted(eventName));
        // TODO: if server, register event in LVM

    }

    // =================================================================================================================
    //       Networking     Networking     Networking     Networking     Networking     Networking     Networking
    // =================================================================================================================

    private static void writeString(FriendlyByteBuf buf, String data) {
        var bytes = data.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readString(FriendlyByteBuf buf) {
        int len = buf.readInt();
        return new String(buf.readBytes(len).array(), StandardCharsets.UTF_8);
    }

    private static class AcC2SEventPacket {
        private final BiConsumer<FriendlyByteBuf, Object> encoder;
        private final BlockPos computerBlockEntity;
        private final String eventName;
        private final Object event;

        public AcC2SEventPacket(BiConsumer<FriendlyByteBuf, Object> encoder, BlockPos computerBlockEntity, String eventName, Object event) {
            AssertRuntime(encoder != null, () -> "Raising client-side machine event '%s' without encoder!".formatted(eventName));
            this.encoder = encoder;
            this.computerBlockEntity = computerBlockEntity;
            this.eventName = eventName;
            this.event = event;
        }

        private AcC2SEventPacket(BlockPos computerBlockEntity, String eventName, Object event) {
            this.encoder = null;
            this.computerBlockEntity = computerBlockEntity;
            this.eventName = eventName;
            this.event = event;
        }

        void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(computerBlockEntity);
            writeString(buffer, eventName);
            encoder.accept(buffer, event);
        }

        static AcC2SEventPacket decode(FriendlyByteBuf buffer) {
            var pos = buffer.readBlockPos();
            var name = readString(buffer);
            var event = CLIENT_EVENT_ENCODERS.get(name);
            return new AcC2SEventPacket(pos, name, event);
        }

        void handle(NetworkEvent.Context ctx) {
            // raise machine event server-side
            AssertRuntime(encoder == null, () -> "Machine event '%s' originating client-side has not been decoded on the server!".formatted(eventName));
            var obj = Minecraft.getInstance().level.getBlockEntity(computerBlockEntity);
            AssertRuntime(obj instanceof ComputerBlockEntity, () -> "No ComputerBlockEntity found at %s".formatted(computerBlockEntity));
            var cbe = (ComputerBlockEntity) obj;
            cbe.raiseServerMachineEvent(eventName, event);
        }
    }

    private static class AcS2CStatePacket {
        public AcS2CStatePacket(ComputerBlockEntity cbe) {
            // TODO: extract server cbe state
        }

        void encode(FriendlyByteBuf buffer) {
            // TODO: encode cbe state
        }

        static AcS2CStatePacket decode(FriendlyByteBuf buffer) {
            // TODO: decode state client side
            return null;
        }

        void handle(NetworkEvent.Context ctx) {
            // TODO: set client cbe state
        }
    }

    static {
        INSTANCE.registerMessage(0, AcC2SEventPacket.class, (msg, buf) -> msg.encode(buf), buf -> AcC2SEventPacket.decode(buf), (msg, supCtx) -> msg.handle(supCtx.get()));
        INSTANCE.registerMessage(1, AcS2CStatePacket.class, (msg, buf) -> msg.encode(buf), buf -> AcS2CStatePacket.decode(buf), (msg, supCtx) -> msg.handle(supCtx.get()));
    }
}
