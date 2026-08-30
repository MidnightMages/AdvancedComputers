package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.blocks.BaseCableConnectableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.BasePeripheralComponentBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.exceptions.ACError;
import dev.asdf00.mc.advcomp.lua.components.AcBlockEntityComponent;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;


public class ScreenBlockEntity extends BasePeripheralComponentBlockEntity implements MenuProvider, AcBlockEntityComponent {

    public ScreenBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AdvancedComputers.SCREEN_BE.get(), pPos, pBlockState);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return TranslationMap.GuiTitle("screen_block");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        assert level != null;
        if (level.isClientSide()) {
            NetCodeUtils.sendToServer(new ScreenInputToServerEvent(this, "clientLoadedScreen", ""));
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new ScreenMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public LuaUserDataComponent createUserdata() {
        return new ScreenBlockUD(this);
    }

    public static final Set<String> KNOWN_EVENT_NAMES = Set.of("keyPressed", "keyReleased", "textPasted", "charTyped");

    private void triggerMachineEvent_server(String name, LuaObject... content) {
        RuntimeAssert.RuntimeAssert(!getLevel().isClientSide(), "this is a serverside method");
        if (!KNOWN_EVENT_NAMES.contains(name)) {
            AdvancedComputers.LOGGER.error("Server received unknown Screen event");
            return;
        }

        ComputerBlockEntity cbe = getComputerBlockEntityOrNull();
        if (cbe != null) {
            cbe.getLvm().triggerMachineEvent(name, content);
        }
        // just drop event if no computer is connected
    }

    // =================================================================================================================
    //       Networking     Networking     Networking     Networking     Networking     Networking     Networking
    // =================================================================================================================

    public String guiContent = "";

    // TODO missing dim ID
    public static class ScreenContentToClientEvent implements NetCodeUtils.NetworkMessage {
        private final BlockPos[] screens;
        private final ResourceKey<Level>[] screenDimensions;
        private final String eventName;
        private final String content;

        @SuppressWarnings("unchecked")
        public ScreenContentToClientEvent(ScreenBlockEntity[] screenBEs, String eventName, String content) {
            this.screens = new BlockPos[screenBEs.length];

            this.screenDimensions = (ResourceKey<Level>[]) (new ResourceKey<?>[screenBEs.length]);
            for (int i = 0; i < screenBEs.length; i++) {
                this.screens[i] = screenBEs[i].getBlockPos();
                this.screenDimensions[i] = screenBEs[i].level.dimension();
            }
            this.eventName = eventName;
            this.content = content;
        }

        private ScreenContentToClientEvent(BlockPos[] screens, ResourceKey<Level>[] screenDimensions, String eventName, String content) {
            this.screens = screens;
            this.screenDimensions = screenDimensions;
            this.eventName = eventName;
            this.content = content;
        }

        @SuppressWarnings("unchecked")
        public static ScreenContentToClientEvent decode(FriendlyByteBuf buffer) {
            BlockPos[] positions = new BlockPos[buffer.readInt()];
            ResourceKey<Level>[] screensDimensions = (ResourceKey<Level>[]) (new ResourceKey<?>[positions.length]);
            for (int i = 0; i < positions.length; i++) {
                positions[i] = buffer.readBlockPos();
                screensDimensions[i] = buffer.readResourceKey(Registries.DIMENSION); //buffer.readResourceKey() NetCodeUtils.readStringFromBuf(buffer);
            }
            var name = NetCodeUtils.readStringFromBuf(buffer);
            var cont = NetCodeUtils.readStringFromBuf(buffer);
            return new ScreenContentToClientEvent(positions, screensDimensions, name, cont);
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeInt(screens.length);
            for (int i = 0; i < (screens.length); i++) {
                buffer.writeBlockPos(screens[i]);
                buffer.writeResourceKey(screenDimensions[i]);
            }
            NetCodeUtils.writeStringToBuf(buffer, eventName);
            NetCodeUtils.writeStringToBuf(buffer, content);
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                for (int i = 0; i < screens.length; i++) {
                    BlockPos screenPos = screens[i];
                    ResourceKey<Level> screenDimension = screenDimensions[i];
                    if (!Minecraft.getInstance().level.dimension().equals(screenDimension))
                        continue;

                    var obj = Minecraft.getInstance().level.getBlockEntity(screenPos);
                    if (obj instanceof ScreenBlockEntity sbe) {
                        ACError.Assert(sbe.getLevel().isClientSide(), "Handling this screen event must be done client-side");
                        if (eventName == null || content == null) {
                            AdvancedComputers.LOGGER.warn("Received invalid Screen event containing null values2");
                            return;
                        }
                        switch (eventName) {
                            case "appendGuiText":
                                sbe.guiContent += content;
                                while (true) {
                                    var startLen = sbe.guiContent.length();
                                    // TODO clean this up and optimize it
                                    sbe.guiContent = Pattern.compile("[^\b]\b", Pattern.DOTALL).matcher(sbe.guiContent).replaceAll("");
                                    if (!sbe.guiContent.isEmpty() && sbe.guiContent.charAt(0) == '\b')
                                        sbe.guiContent = sbe.guiContent.substring(1);

                                    var afterLen = sbe.guiContent.length();
                                    if (startLen == afterLen)
                                        break;
                                }
                                break;
                            case "clearGuiText":
                                sbe.guiContent = "";
                                break;
                            case "setGuiText":
                                sbe.guiContent = content;
                                break;
                            default:
                                AdvancedComputers.LOGGER.warn("Received invalid Screen event type2: '" + eventName + "'");
                                break;
                        }
                    } else {
                        AdvancedComputers.LOGGER.warn("Received invalid packet for Screen event2");
                    }
                }
            });
            ctx.setPacketHandled(true);
        }

        @Override
        public String toString() {
            return "ScreenContentToClientEvent{" +
                   "sbePos=" + screens +
                   ", eventName='" + eventName + '\'' +
                   ", content='" + content + '\'' +
                   '}';
        }
    }

    public static class ScreenInputToServerEvent implements NetCodeUtils.NetworkMessage {
        private final BlockPos sbePos;
        private final String eventName;
        private final String content;

        public ScreenInputToServerEvent(ScreenBlockEntity sbe, String eventName, String content) {
            sbePos = sbe.getBlockPos();
            this.eventName = eventName;
            this.content = content;
        }

        private ScreenInputToServerEvent(BlockPos sbePos, String eventName, String content) {
            this.sbePos = sbePos;
            this.eventName = eventName;
            this.content = content;
        }

        public static ScreenInputToServerEvent decode(FriendlyByteBuf buffer) {
            var pos = buffer.readBlockPos();
            var name = NetCodeUtils.readStringFromBuf(buffer);
            var cont = NetCodeUtils.readStringFromBuf(buffer);
            return new ScreenInputToServerEvent(pos, name, cont);
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
                    ACError.Assert(!sbe.getLevel().isClientSide(), "Handling this screen event must be done server-side");
                    if (eventName == null || content == null) {
                        AdvancedComputers.LOGGER.warn("Received invalid Screen event containing null values");
                        return;
                    }
                    if (eventName.equals("clientLoadedScreen")) {
                        sbe.getComputerBlockEntity().getLvm().requestScreenContents(sbe);
                        return;
                    }

                    if (eventName.equals("keyPressed") || eventName.equals("keyReleased")) { // that one contains modifiers too, in the format INT_MODIFIERS;STRING_LETTER
                        var splitted = content.split(";", 4);
                        sbe.triggerMachineEvent_server(eventName,
                                LuaObject.of(splitted[3]), // string representation
                                LuaObject.of(Integer.parseInt(splitted[0])), // keyCode
                                LuaObject.of(Integer.parseInt(splitted[1])), // scanCode
                                LuaObject.of(Integer.parseInt(splitted[2])) // modifiers (flags)
                        );
                    } else {
                        sbe.triggerMachineEvent_server(eventName, LuaObject.of(content));
                    }
                } else {
                    AdvancedComputers.LOGGER.warn("Received invalid packet for Screen event");
                }
            });
            ctx.setPacketHandled(true);
        }

        @Override
        public String toString() {
            return "ScreenInputToServerEvent{" +
                    "sbePos=" + sbePos +
                    ", eventName='" + eventName + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }

    private ComputerBlockEntity getComputerBlockEntity() {
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
}
