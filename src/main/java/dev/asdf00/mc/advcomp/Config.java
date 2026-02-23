package dev.asdf00.mc.advcomp;

import dev.asdf00.mc.advcomp.utils.AcConfigBuilder;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = AdvancedComputers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final AcConfigBuilder BUILDER = new AcConfigBuilder();

    private static final ForgeConfigSpec.BooleanValue LUA_FS_ESCAPE_UPPERCASE_CHARS_IN_HOST_FS = BUILDER
            .comment("""
                    Whether to store in-game file on the host-filesystem using a path that is always lowercase.

                    If true, the in-game file and folder names have all their uppercase letters and '=' replaced by the lowercase variant, prefixed by an equals sign.
                    The letters 'A' to 'Z' are turned into '=A' and '=Z', and '=' into '=='. When accessing these files in Minecraft, the original names will be restored.
                    Any preexisting files containing A-Z or = are encoded when the containing in-game filesystem is initialized.

                    If false, no encoding is performed. Files and folders are written to and read from the host-filesystem directly.

                    Beware: Setting this to true will automatically encode all files.
                    Setting this to false afterwards will *not* undo this encoding. In that case, will need to rename those files manually.
                    """)
            .worldRestart()
            .define("lua.filesystem.escapeUppercaseCharactersOnHost", false);


    private static final ForgeConfigSpec.BooleanValue LUA_VM_CACHE2_ENABLED = BUILDER
            .comment("""
                    Whether to enable the second compilation cache. This cache is persistent across server restarts.
                    """)
            .worldRestart()
            .define("lua.vm.cache2_enabled", true);

    private static final ForgeConfigSpec.IntValue LUA_VM_CACHE2_MAX_FILES = BUILDER
            .comment("""
                    How many files to store in the second compilation cache in memory and also on disk in '<saveFolder>/advancedComputers/compilationCache/'.
                    """)
            .worldRestart()
            .defineInRange("lua.vm.cache2_max_files", 1000, 5, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean escapeUppercaseCharactersOnHost;
    public static boolean luaVmCache2Enabled;
    public static int luaVmCache2MaxFiles;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        escapeUppercaseCharactersOnHost = LUA_FS_ESCAPE_UPPERCASE_CHARS_IN_HOST_FS.get();
        luaVmCache2Enabled = LUA_VM_CACHE2_ENABLED.get();
        luaVmCache2MaxFiles = LUA_VM_CACHE2_MAX_FILES.get();
    }
}
