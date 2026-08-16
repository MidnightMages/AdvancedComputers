package dev.asdf00.mc.advcomp;

import dev.asdf00.mc.advcomp.utils.AcConfigBuilder;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = AdvancedComputers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final AcConfigBuilder BUILDER = new AcConfigBuilder();

    private static final ForgeConfigSpec.IntValue AUDIO_VOLUME = BUILDER
            .comment("""
                    How loud synthesized audio, i.e. the beeping sound is.
                    """)
            .worldRestart()
            .defineInRange("audio.volume", 17,0,30); // lets limit it to 30 as 17 is already quite audible

    private static final ForgeConfigSpec.IntValue AUDIO_MAX_DISTANCE = BUILDER
            .comment("""
                    How far the synthesized audio is hearable.
                    """)
            .worldRestart()
            .defineInRange("audio.maxDistance", 25,0,64);


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
            .define("lua.vm.cache2Enabled", true);

    private static final ForgeConfigSpec.IntValue LUA_VM_CACHE2_MAX_FILES = BUILDER
            .comment("""
                    How many files to store in the second compilation cache in memory and also on disk in '<saveFolder>/advancedComputers/compilationCache/'.
                    """)
            .worldRestart()
            .defineInRange("lua.vm.cache2MaxFiles", 1000, 5, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue LUA_VM_PRECOMPILE_UEFI_AND_OS = BUILDER
            .comment("""
                    Precompile uefi.lua and parts of Advanced OS to speed up initial startup.
                    """)
            .worldRestart()
            .define("lua.vm.precompileUefiAndOs", true);

    private static final ForgeConfigSpec.BooleanValue DEBUG_LUA_PRINT_TO_SERVER_CONSOLE = BUILDER
            .comment("""
                    If this is set to true, the default UEFI will also print to the server console. This is useful for debugging, but it may look confusing in the server console.
                    If you are unsure, keep it turned off.
                    """)
            .worldRestart()
            .define("debug.lua.printToServerConsole", false);

    private static final ForgeConfigSpec.BooleanValue COMPONENT_INTERNET_HTTP_ENABLED = BUILDER
            .comment("Whether to allow http(s) requests to the real-world network.")
            .worldRestart()
            .define("component.internet.http.enabled", true);

    private static final ForgeConfigSpec.BooleanValue COMPONENT_INTERNET_BLOCK_LOCAL_IPS = BUILDER
            .comment("Whether local ip ranges should be blocked. Disabling this may expose services on the server's machine or in its LAN, which could be a security risk.")
            .worldRestart()
            .define("component.internet.blockLocalIps", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean escapeUppercaseCharactersOnHost;
    public static boolean luaVmCache2Enabled;
    public static int luaVmCache2MaxFiles;
    public static boolean luaVmPrecompileUefiAndOs;
    public static boolean debugLuaPrintToServerConsole;
    public static float audioVolume;
    public static int audioMaxDistance;
    public static boolean componentInternetHttpEnabled;
    public static boolean componentInternetBlockLocalIPs;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        escapeUppercaseCharactersOnHost = LUA_FS_ESCAPE_UPPERCASE_CHARS_IN_HOST_FS.get();
        luaVmCache2Enabled = LUA_VM_CACHE2_ENABLED.get();
        luaVmCache2MaxFiles = LUA_VM_CACHE2_MAX_FILES.get();
        luaVmPrecompileUefiAndOs = LUA_VM_PRECOMPILE_UEFI_AND_OS.get();
        debugLuaPrintToServerConsole = DEBUG_LUA_PRINT_TO_SERVER_CONSOLE.get();
        audioVolume = AUDIO_VOLUME.get() / 100f;
        audioMaxDistance = AUDIO_MAX_DISTANCE.get();
        RuntimeAssert.RuntimeAssert(audioVolume <= 1, "somehow the volume is greater than 1"); // just to avoid some ear-blasting accidents
        componentInternetHttpEnabled = COMPONENT_INTERNET_HTTP_ENABLED.get();
        componentInternetBlockLocalIPs = COMPONENT_INTERNET_BLOCK_LOCAL_IPS.get();
    }
}
