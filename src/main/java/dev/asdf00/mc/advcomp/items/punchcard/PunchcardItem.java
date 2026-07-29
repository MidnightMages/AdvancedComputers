package dev.asdf00.mc.advcomp.items.punchcard;

import dev.asdf00.mc.advcomp.blocks.punchcard_machine.PunchcardMachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PunchcardItem extends Item {
    public PunchcardItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.advancedcomputers.shared.notimplemented"));

        var data = getData(pStack);
        var text = data == null ? "§4NONE!§r" : data;
        pTooltipComponents.add(Component.literal("Stamped instructions: %s".formatted(text)));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    public static String getData(@NotNull ItemStack pStack) {
        var tag = pStack.getTag();
        return tag == null ? null : tag.getString(PunchcardMachineBlockEntity.NBT_PUNCHCARD_ITEM_DATA_KEY);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {
        if (!pLevel.isClientSide()) {
                NetworkHooks.openScreen(
                        (ServerPlayer) pPlayer,
                        new SimpleMenuProvider((pContainerId, inv, player) -> new PunchcardItemMenu(pContainerId, inv, pUsedHand), Component.literal("Punchcard")),
                        buf -> buf.writeEnum(pUsedHand)
                );
            }
        return InteractionResultHolder.sidedSuccess(pPlayer.getItemInHand(pUsedHand), pLevel.isClientSide());
    }
}
