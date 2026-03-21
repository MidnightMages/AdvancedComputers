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

    private static final int COL_CNT = 110;
    private static final int LINE_CNT = 44;
    private static final int SCREENSIZEX = COL_CNT * 5; // 5 = font char width
    private static final int SCREENSIZEY = LINE_CNT * 9; // 9 = font char height

    private static final int CORNERSZ = 5;

    public ScreenBlockScreen(ScreenMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = SCREENSIZEX + CORNERSZ * 2; // TODO set this to the actual current screen size, or to a good estimate, so that JEI properly shows
        this.imageHeight = SCREENSIZEY + CORNERSZ * 2;
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
        int screenWantedWidth = SCREENSIZEX;
        int screenWantedHeight = SCREENSIZEY;
        int SCREENPADDING = 2;
        float smallestDividerSoScreenFits = (Math.max(
                (float) screenWantedWidth / (pGuiGraphics.guiWidth() - CORNERSZ * 2f - SCREENPADDING * 2),
                (float) screenWantedHeight / (pGuiGraphics.guiHeight() - CORNERSZ * 2f - SCREENPADDING * 2)
        ));
        screenWantedWidth = (int) (screenWantedWidth / smallestDividerSoScreenFits);
        screenWantedHeight = (int) (screenWantedHeight / smallestDividerSoScreenFits);

        int paddingSizeLeft = (pGuiGraphics.guiWidth() - screenWantedWidth) / 2;
        int paddingSizeTop = (pGuiGraphics.guiHeight() - screenWantedHeight) / 2;


        var pose = pGuiGraphics.pose();
        pose.pushPose();
        pose.translate(paddingSizeLeft, paddingSizeTop, 0);
        var totalScreenBackgroundWidth = CORNERSZ * 2 + screenWantedWidth + SCREENPADDING * 2;
        var totalScreenBackgroundHeight = CORNERSZ * 2 + screenWantedHeight + SCREENPADDING * 2;
        pGuiGraphics.blitNineSliced(TEXTURE, -CORNERSZ - SCREENPADDING, -CORNERSZ - SCREENPADDING,
                totalScreenBackgroundWidth, totalScreenBackgroundHeight,
                CORNERSZ,
                256, 256,
                256, 256);
        pose.mulPoseMatrix(new Matrix4f().scale(1f / smallestDividerSoScreenFits));
        renderStdOut(pGuiGraphics, (int) smallestDividerSoScreenFits, 0);
        pose.popPose();


        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private void renderStdOut(GuiGraphics pGuiGraphics, int startX, int startY) {
        var lines = this.getScreenEntity().guiContent.replace("\t", "    ").lines().toArray(String[]::new); // TODO handle tabs properly
        int y = 0;
        for (int i = 0; i < lines.length; i++) {
            var l = lines[i];
            pGuiGraphics.drawString(AdvancedComputers.getMonoFont(), l, -1, (y / startX) * startX + 1, -1, false);
            y += 9;
        }
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
