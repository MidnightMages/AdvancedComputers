package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.types.cluster.CableConnectableBlockOrEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class DebuggerItem extends Item {
    public DebuggerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("Right-click any block to obtain information."));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        var level = pContext.getLevel();
        if (!level.isClientSide()) {
            var pos = pContext.getClickedPos();
            var bs = level.getBlockState(pos);
            var be = level.getBlockEntity(pos);
            Function<Block, String> getBlockName = block -> {
                var regEntry = ForgeRegistries.BLOCKS.getKey(block);
                return regEntry == null ? "???" : regEntry.toString().replace("advancedcomputers:", "");
            };
            var messageToSend = "§6============== Block information ==============§r\nChecking block %s at [%s].".formatted(getBlockName.apply(bs.getBlock()), pos.toShortString());

            if (be instanceof ComputerBlockEntity computerBlockEntity) {
                StringBuilder computerInfo = new StringBuilder("§6-- Computer Info:§r ");
                String runState = "UNKNOWN STATE!!!";
                switch (bs.getValue(ComputerBlock.RUN_STATE)){
                    case STOPPED -> runState = "§cSTOPPED§r";
                    case CRASHED -> {
                        var lvm = computerBlockEntity.getLvm();
                        runState = "§cCRASHED - %s§r".formatted(lvm == null ? "unknown" : lvm.stopCode);
                    }
                    case RUNNING, WORKING -> runState = "§aRUNNING§r";
                }
                computerInfo.append("State: %s".formatted(runState));
                messageToSend += "\n" + computerInfo;
            }
            if (be instanceof CableConnectableBlockOrEntity cableConnectable) {
                StringBuilder clusterInfo = new StringBuilder("§6-- Connected Clusters --§r");
                boolean anyClustersFound = false;
                for (var dir : Direction.values()) {
                    var connectedCluster = cableConnectable.getNetworkList().get(dir);
                    if (connectedCluster != null) {
                        var sideInfo = "[%s %d] %d participant(s): %s".formatted(
                                connectedCluster.getClusterType().getClusterName(), connectedCluster.getDebugId(),
                                connectedCluster.getEntityCount(),
                                String.join(", ", Arrays.stream(connectedCluster.connectedEntities)
                                        .map(x->x==cableConnectable ? "§dself§r" : getBlockName.apply(x.getBlockState().getBlock()))
                                        .toArray(String[]::new)
                                )
                        );
                        clusterInfo.append("\n§3%s:§r %s".formatted(dir.getName(), sideInfo));
                        anyClustersFound = true;
                    }
                }
                if (!anyClustersFound) {
                    clusterInfo = new StringBuilder("§6-- Connected Clusters:§r None");
                }
                messageToSend += "\n" + clusterInfo;
            }
            NetCodeUtils.sendToClient(PacketDistributor.PLAYER.with(() -> ((ServerPlayer) pContext.getPlayer())), new ToClientEvent(messageToSend));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static class ToClientEvent implements NetCodeUtils.NetworkMessage {
        private final String textToShow;

        public ToClientEvent(String textToShow) {
            this.textToShow = textToShow;
        }

        public static ToClientEvent decode(FriendlyByteBuf buffer) {
            return new ToClientEvent(NetCodeUtils.readStringFromBuf(buffer));
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            NetCodeUtils.writeStringToBuf(buffer, textToShow);
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                var ply = Minecraft.getInstance().player;
                if (ply != null) {
                    ply.displayClientMessage(Component.literal(textToShow), false);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
