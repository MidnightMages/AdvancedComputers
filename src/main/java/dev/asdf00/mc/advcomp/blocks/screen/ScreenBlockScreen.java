package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class ScreenBlockScreen extends AbstractContainerScreen<ScreenMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(AdvancedComputers.MODID, "textures/gui/screen_gui.png");

    private static final int LINE_CNT = 27;
    private static final int SCREENSIZEY = 253;
    private static final int SCREENSIZEX = SCREENSIZEY * 16 / 9;

    private static final int CORNERSZ = 5;

    public ScreenBlockScreen(ScreenMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageHeight = SCREENSIZEY;
        this.imageWidth = SCREENSIZEX;
//        MONOFONT = AdvancedComputers.GetFont();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 1000; // hide top text
        this.inventoryLabelY = 1000; // hide inventory text
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        float padding = 0.2f;
        float totalScreenSize = 1 - 2 * padding;
        int screenWantedWidth = (int) (pGuiGraphics.guiWidth() * totalScreenSize);
        int screenWantedHeight = (int) (pGuiGraphics.guiHeight() * totalScreenSize);
        int paddingSizeLeft = (pGuiGraphics.guiWidth() - screenWantedWidth) / 2;
        int paddingSizeTop = (pGuiGraphics.guiHeight() - screenWantedHeight) / 2;

        pGuiGraphics.blitWithBorder(TEXTURE, paddingSizeLeft, paddingSizeTop, 0, 0, screenWantedWidth, screenWantedHeight, 256, 256, CORNERSZ);
        renderStdOut(pGuiGraphics, paddingSizeLeft + CORNERSZ + 1, paddingSizeTop + CORNERSZ + 3);

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private void renderStdOut(GuiGraphics pGuiGraphics, int startX, int startY) {
        var pose = pGuiGraphics.pose();
        pose.pushPose();
        pose.translate(startX, startY, 0);
        pose.mulPoseMatrix(new Matrix4f().scale(0.75f));
        var lines = this.getScreenEntity().guiContent.replace("\t", "    ").lines().toArray(String[]::new); // TODO handle tabs properly
        int y = 0;
        for (int i = 0; i < lines.length; i++) {
            var l = lines[i];
            pGuiGraphics.drawString(AdvancedComputers.getMonoFont(), l, 0, y, -1, false);
            y += 10;
        }
        pose.popPose();
    }

    private ScreenBlockEntity getScreenEntity() {
        return getMenu().blockEntity;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        switch (pKeyCode) {
            case 256 -> onClose();
            case 257 -> getScreenEntity().triggerMachineEvent("keyTyped", "\n");
            case 259 -> getScreenEntity().triggerMachineEvent("keyTyped", "\b");
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (super.charTyped(pCodePoint, pModifiers)) {
            return true;
        }
        getScreenEntity().triggerMachineEvent("keyTyped", String.valueOf(pCodePoint));
        return true;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 1) {
            // right-click to paste
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null && clip.length() > 0) {
                getScreenEntity().triggerMachineEvent("textPasted", clip);
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
}
