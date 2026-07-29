package dev.asdf00.mc.advcomp.blocks.punchcard_machine;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class PunchcardMachineBlockScreen extends AbstractContainerScreen<PunchcardMachineBlockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(AdvancedComputers.MODID, "textures/gui/punchcard_machine_gui.png");
    private Button encodeButton;
    private MultiLineEditBox codeBox;
    private int focusId = 0;

    public PunchcardMachineBlockScreen(PunchcardMachineBlockMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        topPos -= 30;
        //this.titleLabelY = 1000; // hide top text
        this.inventoryLabelY = 1000;

        encodeButton = Button.builder(Component.literal("Encode"), this::handleEncodeButton)
                .bounds(this.leftPos + 119, this.topPos + 136, 50, 20)
                .build();

        codeBox = new MultiLineEditBox(getMinecraft().font.self(),
                this.leftPos + 6, this.topPos + 18, 164, 100,
                Component.literal("Code"), Component.literal(""));

        codeBox.setCharacterLimit(80);
        codeBox.setValueListener(text -> {
            getMenu().blockEntity.syncToServer(false, text);
        });

        addRenderableWidget(encodeButton);
        addRenderableWidget(codeBox);
        imageHeight = 240;

    }

    private void handleEncodeButton(Button button) {
        getMenu().blockEntity.syncToServer(true, null);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURE);
        pGuiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        var newText = menu.blockEntity.currentGuiText;
        if (newText != null) {
            this.codeBox.setValue(newText);
            menu.blockEntity.currentGuiText = null;
        }
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (encodeButton.mouseClicked(pMouseX, pMouseY, pButton)) {
            codeBox.setFocused(false);
            return true;
        }

        if (!codeBox.mouseClicked(pMouseX, pMouseY, pButton)) {
            codeBox.setFocused(false);
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }
        codeBox.setFocused(true);
        return true;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (codeBox.isFocused())
            return codeBox.charTyped(pCodePoint, pModifiers);

        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {

        if (pKeyCode == 258) { // TAB KEY
            focusId++;
            focusId %= 3;

            codeBox.setFocused(focusId == 1);
            encodeButton.setFocused(focusId == 2);
            return true;
        }


        if (pKeyCode != 256) { // ESC KEY
            if (codeBox.isFocused())
                return codeBox.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        if (codeBox.isFocused())
            return codeBox.keyReleased(pKeyCode, pScanCode, pModifiers);

        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }
}
