package dev.asdf00.mc.advcomp.blocks.screen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.ClientStatics;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ScreenBlockScreen extends AbstractContainerScreen<ScreenMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(AdvancedComputers.MODID, "textures/gui/screen_gui.png");
    private static ScreenBlockScreen currentlyOpenInstance = null;

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
        currentlyOpenInstance = this;
    }

    @Override
    public void onClose() {
        currentlyOpenInstance = null;
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
        renderStdOut(pGuiGraphics);
        pose.popPose();


        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private void renderStdOut(GuiGraphics pGuiGraphics) {
        var lines = this.getScreenEntity().guiContent.replace("\t", "    ").lines().toArray(String[]::new); // TODO handle tabs properly
        int y = 0;
        for (int i = 0; i < lines.length; i++) {
            var l = lines[i];
            pGuiGraphics.drawString(ClientStatics.getMonoFont(), l, -1, y + 1, -1, false);
            y += 9;
        }
    }

    public void triggerRawScreenEvent(String type, String content) {
        NetCodeUtils.sendToServer(new ScreenBlockEntity.ScreenInputToServerEvent(getScreenEntity(), type, content));
    }

    public void triggerKeyPressedEvent(String type, int keyCode, int scanCode, int modifiers, String stringRepresentation) {
        String content = "%d;%d;%d;%s".formatted(keyCode, scanCode, modifiers, stringRepresentation);
        triggerRawScreenEvent(type, content);
    }

    public void emitGeneralKeyEvent(String type, int pKeyCode, int pScanCode, int pModifiers) {
        // pmodifiers:
        // 1=shift, 2=ctrl and r_ctrl, 4=alt, 6=alt_gr
        boolean isPressEvent = type.equals("keyPressed");
        switch (pKeyCode) {
            case 256 -> { // ESC
                onClose();
            }
            // 335 is the numpad enter, 257 is the regular one
            case 258 -> {
                triggerKeyPressedEvent(type, pKeyCode, pScanCode, pModifiers, "\t");
                if (isPressEvent)
                    charTyped('\t', pModifiers);
            }
            case 257, 335 -> {
                triggerKeyPressedEvent(type, pKeyCode, pScanCode, pModifiers, "\n");
                if (isPressEvent)
                    charTyped('\n', pModifiers);
            } // enter, numpad_enter
            case 259 -> {
                triggerKeyPressedEvent(type, pKeyCode, pScanCode, pModifiers, "\b");
                if (isPressEvent)
                    charTyped('\b', pModifiers);
            }
            case 260, // insert
                 261, // del
                 262, // rightarrow
                 263, // leftarrow
                 264, // downarrow
                 265, // uparrow
                 266, // pgup
                 267, // pgdown
                 268, // home
                 269, // end

                 280, // capslock
                 281, // scroll lock
                 282, // numlock
                 283, // print
                 284, // pause

                 290, // F1 to
                 291, 292, 293, 294, 295, 296, 297, 298, 299, 300,
                 301, // F12

                 340, // shift
                 341, // ctrl
                 342, // alt
                 343, // win
                 344, // shift
                 345, // rctrl
                 346 // alt gr
                    -> triggerKeyPressedEvent(type, pKeyCode, pScanCode, pModifiers, "");


            // -- numpad: we cannot properly track numlock --> always use numlock=on
            case 320, // numpad_0
                 321, // numpad_1
                 322, // numpad_2
                 323, // numpad_3
                 324, // numpad_4
                 325, // numpad_5
                 326, // numpad_6
                 327, // numpad_7
                 328, // numpad_8
                 329 // numpad_9
                    -> triggerKeyPressedEvent(type, pKeyCode, pScanCode, pModifiers, String.valueOf(pKeyCode - 320));

            case 330 ->
                    triggerKeyPressedEvent(type, pKeyCode, pScanCode, pModifiers, GLFW.glfwGetKeyName(pKeyCode, pScanCode)); // numpad_period
            default -> {
                var chr = GLFW.glfwGetKeyName(pKeyCode, pScanCode);
                if (chr == null || chr.isEmpty()) {
                    triggerKeyPressedEvent(type, pKeyCode, pScanCode, pModifiers, "");
                } else {
                    String casedChar;
                    if ((pModifiers & 1) != 0) // shift pressed
                        casedChar = chr.toUpperCase();
                    else
                        casedChar = chr.toLowerCase();

                    triggerKeyPressedEvent(type, pKeyCode, pScanCode, pModifiers, casedChar);
                }
            }
        }
    }

    private ScreenBlockEntity getScreenEntity() {
        return getMenu().blockEntity;
    }

    private static void onKeyboardEventGuarded(Consumer<ScreenBlockScreen> action) {
        var ply = Minecraft.getInstance().player;
        var openInstance = currentlyOpenInstance;
        if (ply == null || openInstance == null)
            return;

        var menu = ply.containerMenu;
        if (menu instanceof ScreenMenu)
            action.accept(openInstance);
    }

    // pModifiers:
    // 1=shift, 2=ctrl and r_ctrl, 4=alt, 6=alt_gr
    @SubscribeEvent(receiveCanceled = true)
    public static void onKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() == 256) // not ESC
        {
            return;
        }
        onKeyboardEventGuarded(screen -> screen.emitGeneralKeyEvent("keyPressed", event.getKeyCode(), event.getScanCode(), event.getModifiers()));
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onKeyReleasedPre(ScreenEvent.KeyReleased.Pre event) {
        onKeyboardEventGuarded(screen -> {
            if (event.getKeyCode() == 256) // not ESC
            {
                LocalPlayer ply = Minecraft.getInstance().player;
                assert ply != null;
                ply.closeContainer();
                return;
            }
            screen.emitGeneralKeyEvent("keyReleased", event.getKeyCode(), event.getScanCode(), event.getModifiers());
        });
    }
//    @SubscribeEvent(receiveCanceled = true)
//    public static void onCharTypedPre(ScreenEvent.CharacterTyped.Pre event) {
//        onKeyboardEventGuarded(screen -> );
//    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        return true; // mark as consumed
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        return true; // mark as consumed
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        triggerRawScreenEvent("charTyped", String.valueOf(pCodePoint));
        return true; // mark as consumed
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 2) {
            // middle-click to paste
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (!clip.isEmpty()) {
                triggerRawScreenEvent("textPasted", clip);
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
}
