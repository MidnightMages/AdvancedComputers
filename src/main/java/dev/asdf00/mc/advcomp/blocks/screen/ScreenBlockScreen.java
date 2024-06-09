package dev.asdf00.mc.advcomp.blocks.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.lua.LuaStdOut;
import dev.asdf00.mc.advcomp.lua.components.GraphicsBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ScreenBlockScreen extends AbstractContainerScreen<ScreenMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(AdvancedComputers.MODID, "textures/gui/screen_gui.png");
    private static final Style MONOFONT = Style.EMPTY.withFont(new ResourceLocation(AdvancedComputers.MODID, "pixeloidmono")); // otf font

    private static final int LINE_CNT = 15;

    private final GraphicsBuffer gb;
    private ComputerBlockEntity computerEntity;

    public ScreenBlockScreen(ScreenMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        computerEntity = null;
        this.imageWidth = 256;
        this.imageHeight = 149;
        this.gb = new GraphicsBuffer(50, 15);
//        MONOFONT = AdvancedComputers.GetFont();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 1000; // hide top text
        this.inventoryLabelY = 1000; // hide inventory text
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        //renderScreenContents(pGuiGraphics);
    }

    private void renderScreenContents(@NotNull GuiGraphics pGuiGraphics) {

        int x = (width - imageWidth) / 2 + 6;
        int y = (height - imageHeight) / 2 + 11;
        for (int i = 0; i < gb.getHeight(); i++) {
            var s = "testString!!aaaaaaaaa";
            var s2 = /*Minecraft.getInstance().font.getSplitter().headByWidth(*/Component.literal(s).withStyle(MONOFONT);//, gb.getWidth(), Style.EMPTY) ;
            pGuiGraphics.drawString(font, s2 /*gb.getRow(i)*/, x, y + font.lineHeight * i, -1);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        // TODO: render image, not stdout
        renderStdOut(pGuiGraphics);

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private void renderStdOut(GuiGraphics pGuiGraphics) {
        var out = getComputerEntity().getLvm().getStdOut();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        if (out instanceof LuaStdOut stdOut) {
            // display standard out
            var txt = stdOut.getLastLines(LINE_CNT);
            x += 6;
            y += 6;
            for (int i = 0; i < txt.length; i++) {
                pGuiGraphics.drawString(font, txt[i], x, y, -1);
                y += font.lineHeight;
            }
        } else if (out instanceof String errCode) {
            // display stop code centered
            x += imageWidth / 2;
            y += (imageHeight - (font.lineHeight)) / 2;
            pGuiGraphics.drawCenteredString(font, errCode, x, y, -1);
        }
    }

    private ComputerBlockEntity getComputerEntity() {
        if (computerEntity == null) {
            computerEntity = getMenu().blockEntity.getComputerBlockEntity();
        }
        return computerEntity;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        switch (pKeyCode) {
            case 256 -> onClose();
            case 257 -> getComputerEntity().getLvm().pushMachineEvent("keyTyped", "\n");
            case 259 -> getComputerEntity().getLvm().pushMachineEvent("keyTyped", "\b");
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
        getComputerEntity().getLvm().pushMachineEvent("keyTyped", String.valueOf(pCodePoint));
        return true;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 1) {
            // right-click to paste
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null && clip.length() > 0) {
                getComputerEntity().getLvm().pushMachineEvent("textPasted", clip);
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
}
