package dev.asdf00.mc.advcomp.blocks.punchcard_machine.mainboard_programmer;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.api.ItemCanBeInitialized;
import dev.asdf00.mc.advcomp.exceptions.ACError;
import dev.asdf00.mc.advcomp.utils.NotifyingItemHandler;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PunchcardMachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final String NBT_BLOCK_TEXT_KEY = "codeBoxContents";
    public static final String NBT_PUNCHCARD_ITEM_DATA_KEY = "data";
    public final NotifyingItemHandler itemHandler = new NotifyingItemHandler(this, PunchcardMachineBlockMenu.TE_INVENTORY_SLOT_COUNT,
            (slotIdx, itemStack) -> slotIdx == 0 && itemStack.is(Items.PAPER) ? -1 : 0,
            this::itemHandler_onSlotChanged
    );
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    protected final ContainerData data;
    String currentGuiText = "";
    final int INPUT_SLOT = 0;
    final int OUTPUT_SLOT = 1;

    public PunchcardMachineBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.PUNCHCARD_MACHINE_BE.get(), pPos, pBlockState);
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
        if (getLevel().isClientSide()) return;

        var is = itemHandler.getStackInSlot(slot);
        if (is.getItem() instanceof ItemCanBeInitialized cbi) {
            cbi.initialize(is);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return TranslationMap.GuiTitle("punchcard_machine_block");
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
        return new PunchcardMachineBlockMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        itemHandler.saveContents(pTag);
        pTag.putString(NBT_BLOCK_TEXT_KEY, currentGuiText);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        currentGuiText = pTag.getString(NBT_BLOCK_TEXT_KEY);
        itemHandler.loadContents(pTag);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    private void createPunchcardAndClearText() {
        if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) // output slot obstructed
            return;

        var inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        if (!inputStack.isEmpty()) {
            itemHandler.setStackInSlot(INPUT_SLOT, inputStack.copyWithCount(inputStack.getCount() - 1)); // reduce input item
            var outStack = new ItemStack(AdvancedComputers.PUNCHCARD_ITEM.get(), 1);
            var outTag = outStack.getOrCreateTag();
            outTag.putString(NBT_PUNCHCARD_ITEM_DATA_KEY, currentGuiText);
            itemHandler.setStackInSlot(OUTPUT_SLOT, outStack);
            currentGuiText = "";
            syncTextChangedToClients();
        }

    }

    public void syncToServer(boolean triggerButtonClick, String text) {
        RuntimeAssert.RuntimeAssert(level.isClientSide(), "must be called clientside");
        NetCodeUtils.sendToServer(new PunchcardMachineBlockEntity.SyncToServerEvent(this, triggerButtonClick, text));
    }

    private void syncTextChangedToClients() {
        NetCodeUtils.sendToClient(PacketDistributor.TRACKING_CHUNK.with(() -> {
                    assert this.level != null;
                    return this.level.getChunkAt(this.getBlockPos());
                }),
                new PunchcardMachineBlockEntity.SyncToClientEvent(this, currentGuiText));
    }

    void sendInitialSyncToClient() {
        syncTextChangedToClients();
    }

    public static class SyncToServerEvent implements NetCodeUtils.NetworkMessage {
        private final BlockPos bePos;
        private final boolean encodeButtonWasClicked;
        private final String textToSet;

        public SyncToServerEvent(PunchcardMachineBlockEntity be, boolean encodeButtonWasClicked, String textToSet) {
            bePos = be.worldPosition;
            this.encodeButtonWasClicked = encodeButtonWasClicked;
            this.textToSet = textToSet;
        }

        private SyncToServerEvent(BlockPos bePos, boolean encodeButtonWasClicked, String textToSet) {
            this.bePos = bePos;
            this.encodeButtonWasClicked = encodeButtonWasClicked;
            this.textToSet = textToSet;
        }

        public static SyncToServerEvent decode(FriendlyByteBuf buffer) {
            return new SyncToServerEvent(
                    buffer.readBlockPos(),
                    buffer.readBoolean(),
                    NetCodeUtils.readNullableStringFromBuf(buffer));
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(bePos);
            buffer.writeBoolean(encodeButtonWasClicked);
            NetCodeUtils.writeNullableStringToBuf(buffer, textToSet);
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                var obj = ctx.getSender().level().getBlockEntity(bePos);
                if (obj instanceof PunchcardMachineBlockEntity be) {
                    assert be.level != null;
                    ACError.Assert(!be.level.isClientSide, "Handling UI event for PunchcardMachineBlockEntity client-side");
                    if (textToSet != null) {
                        be.currentGuiText = textToSet;
                    }

                    if (encodeButtonWasClicked) {
                        be.createPunchcardAndClearText();
                    }

                    var destClients = ctx.getSender().server.getPlayerList().getPlayers()
                            .stream()
                            .filter(x -> x.containerMenu instanceof PunchcardMachineBlockMenu)
                            .filter(x -> encodeButtonWasClicked || !x.getUUID().equals(ctx.getSender().getUUID())) // exclude sender if it wasnt a button press
                            .toArray(ServerPlayer[]::new);

                    for (var client : destClients)
                        NetCodeUtils.sendToClient(PacketDistributor.PLAYER.with(() -> client), new SyncToClientEvent(be, be.currentGuiText));
                } else {
                    AdvancedComputers.LOGGER.warn("Received invalid packet for UI event for PunchcardMachineBlockEntity");
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class SyncToClientEvent implements NetCodeUtils.NetworkMessage {
        private final BlockPos bePos;
        private final String textToSet;

        public SyncToClientEvent(PunchcardMachineBlockEntity be, String textToSet) {
            bePos = be.worldPosition;
            this.textToSet = textToSet;
        }

        private SyncToClientEvent(BlockPos bePos, String textToSet) {
            this.bePos = bePos;
            this.textToSet = textToSet;
        }

        public static SyncToClientEvent decode(FriendlyByteBuf buffer) {
            return new SyncToClientEvent(buffer.readBlockPos(), NetCodeUtils.readStringFromBuf(buffer));
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(bePos);
            NetCodeUtils.writeStringToBuf(buffer, textToSet);
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                var obj = Minecraft.getInstance().level.getBlockEntity(bePos);
                if (obj instanceof PunchcardMachineBlockEntity be) {
                    assert be.level != null;
                    ACError.Assert(be.level.isClientSide, "Handling UI client event for PunchcardMachineBlockEntity server-side");
                    be.currentGuiText = textToSet;
                } else {
                    AdvancedComputers.LOGGER.warn("Received invalid packet for UI event for PunchcardMachineBlockEntity clientside");
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
