package dev.asdf00.mc.advcomp.blocks.computer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.TranslationMap;
import dev.asdf00.mc.advcomp.types.MultiImageButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ComputerBlockScreen extends AbstractContainerScreen<ComputerBlockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(AdvancedComputers.MODID, "textures/gui/computer_gui.png");
    private static final Component ON_OFF_BUTTON = TranslationMap.GuiButton("computer_block", "onoff");
    private MultiImageButton onOffButton;

    public ComputerBlockScreen(ComputerBlockMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    private static final int OnOffButtonSize = 14;

    @Override
    protected void init() {
        super.init();
        //this.titleLabelY = 1000; // hide top text
        onOffButton = addRenderableWidget(
                // (int pX, int pY, int pWidth, int pHeight, int pXTexStart, int pYTexStart, ResourceLocation pResourceLocation, Button.OnPress pOnPress)
                new MultiImageButton(this.leftPos + 22, this.topPos + 29, OnOffButtonSize, OnOffButtonSize, 0, 166, TEXTURE, this::handleOnOffButton)
        );
        onMachineStateChanged();
    }

    private boolean isMachineRunning() {
        return getMenu().blockEntity.getBlockState().getValue(ComputerBlock.RUN_STATE).isRunning();
    }

    private void onMachineStateChanged() {
        onOffButton.setXTexStart(isMachineRunning() ? OnOffButtonSize : 0);
    }

    private void handleOnOffButton(Button btn) {
        getMenu().blockEntity.toggleLVMPowerState();
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return this.onOffButton.mouseClicked(pMouseX, pMouseY, pButton) || super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
}
