package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.exceptions.AdvancedComputersError;
import dev.asdf00.mc.advcomp.types.AcCapabilities;
import dev.asdf00.mc.advcomp.types.IAcDevCableConnectableEntity;
import dev.asdf00.mc.advcomp.types.cluster.BaseAcCableConnectableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;


public class ScreenBlockEntity extends BaseAcCableConnectableBlockEntity implements MenuProvider {
    public final ItemStackHandler itemHandler = new ItemStackHandler(2);
    private LazyOptional<IItemHandler> lazyItemhandler = LazyOptional.empty();
    private final LazyOptional<IAcDevCableConnectableEntity> lazyCableConnectable;

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        // todo add logic
    }


    public ScreenBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.SCREEN_BE.get(), pPos, pBlockState, Collections.singletonList(AdvancedComputers.CLUSTER_TYPE_DEVICE));

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
        if (!getLevel().isClientSide()) {
            CableCluster devCluster = connectedNetworks.values().iterator().next();
            if (devCluster != null) {
                var maybeCbe = devCluster.getHost();
                if (maybeCbe instanceof ComputerBlockEntity cbe) {
                    return cbe;
                }
            }
            return null;
        }
        return null;
    }

    public static final Set<String> KNOWN_EVENT_NAMES = Set.of("keyTyped", "textPasted");

    public void triggerMachineEvent(String name, String content) {
        if (getLevel().isClientSide()) {
            NetCodeUtils.sendToServer(new ScreenOriginatingEvent(this, name, content));
        } else {
            if (!KNOWN_EVENT_NAMES.contains(name)) {
                AdvancedComputers.LOGGER.error("Server received unknown Screen event");
                return;
            }
            var cbe = getComputerBlockEntity();
            if (cbe == null) {
                // just drop event if no computer is connected
                return;
            }
            cbe.getLvm().pushMachineEvent(name, content);
        }
    }

    // =================================================================================================================
    //       Networking     Networking     Networking     Networking     Networking     Networking     Networking
    // =================================================================================================================

    public static class ScreenOriginatingEvent implements NetCodeUtils.NetworkMessage {
        private final BlockPos sbePos;
        private final String eventName;
        private final String content;

        public ScreenOriginatingEvent(ScreenBlockEntity sbe, String eventName, String content) {
            sbePos = sbe.getBlockPos();
            this.eventName = eventName;
            this.content = content;
        }

        private ScreenOriginatingEvent(BlockPos sbePos, String eventName, String content) {
            this.sbePos = sbePos;
            this.eventName = eventName;
            this.content = content;
        }

        public static ScreenOriginatingEvent decode(FriendlyByteBuf buffer) {
            var pos = buffer.readBlockPos();
            var name = NetCodeUtils.readStringFromBuf(buffer);
            var cont = NetCodeUtils.readStringFromBuf(buffer);
            return new ScreenOriginatingEvent(pos, name, cont);
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(sbePos);
            NetCodeUtils.writeStringToBuf(buffer, eventName);
            NetCodeUtils.writeStringToBuf(buffer, content);
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                var obj = ctx.getSender().level().getBlockEntity(sbePos);
                if (obj instanceof ScreenBlockEntity sbe) {
                    AdvancedComputersError.Assert(!sbe.getLevel().isClientSide(), "Handling Screen event client-side");
                    if (eventName == null || content == null) {
                        AdvancedComputers.LOGGER.warn("Received invalid Screen event containing null values");
                        return;
                    }
                    sbe.triggerMachineEvent(eventName, content);
                } else {
                    AdvancedComputers.LOGGER.warn("Received invalid package for Screen event");
                }
            });
            ctx.setPacketHandled(true);
        }

        @Override
        public String toString() {
            return "ScreenOriginatingEvent{" +
                    "sbePos=" + sbePos +
                    ", eventName='" + eventName + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }
}
