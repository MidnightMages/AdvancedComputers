package dev.asdf00.mc.advcomp;

import net.minecraft.network.chat.Component;

public class TranslationMap {

    private static Component GetRaw(String prefix, String suffix) {
        return Component.translatable(prefix + "." + AdvancedComputers.MODID + "." + suffix);
    }

    public static Component GuiName(String name) {
        return GetRaw("gui", name);
    }

    public static Component BlockName(String name) {
        return GetRaw("block", name);
    }

    public static Component GuiTitle(String blockName) {
        return GuiName(blockName + ".title");
    }

    public static Component GuiButton(String blockName, String buttonName) {
        return GuiName(blockName + ".button." + buttonName);
    }
}
