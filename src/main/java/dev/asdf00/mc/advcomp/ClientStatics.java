package dev.asdf00.mc.advcomp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;

public class ClientStatics {
    private static Font monoFont = null;
    private static FontSet fontSet = null;

    public static Font getMonoFont() {
        if (monoFont == null || fontSet.providers.isEmpty()) {
            fontSet = Minecraft.getInstance().fontManager.fontSets.get(new ResourceLocation(AdvancedComputers.MODID, "acfont-firacode-regular"));
            monoFont = new Font((p_284586_) -> fontSet, false);
        }
        return monoFont;
    }
}
