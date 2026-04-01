package dev.asdf00.mc.advcomp;

import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.internals.javac.PersistentJavaCompilationCache;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ModCommands {
    private static final int PERMISSION_LEVEL_OP = 3;

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var root = Commands.literal("ac_clearCache").requires(x -> x.hasPermission(PERMISSION_LEVEL_OP));
        for (var arg : "cache1,cache2,all".split(",")) {
            final String arg_clsr = arg;
            root.then(Commands.literal(arg).executes(ctx -> {
                if (arg_clsr.equals("cache1")) {
                    AdvancedComputers.LOGGER.info("Clearing cache1 as requested via command...");
                    LuaVM.clearCache();
                    ctx.getSource().sendSuccess(() -> Component.literal("Cleared cache1"), true);
                } else if (arg_clsr.equals("cache2")) {
                    AdvancedComputers.LOGGER.info("Clearing cache2as requested via command...");
                    PersistentJavaCompilationCache.clearCache();
                    ctx.getSource().sendSuccess(() -> Component.literal("Cleared cache2"), true);
                } else if (arg_clsr.equals("all")) {
                    AdvancedComputers.LOGGER.info("Clearing caches as requested via command...");
                    PersistentJavaCompilationCache.clearCache();
                    LuaVM.clearCache();
                    ctx.getSource().sendSuccess(() -> Component.literal("Cleared all caches"), true);
                } else {
                    ctx.getSource().sendFailure(Component.literal("unknown choice %s".formatted(arg_clsr)));
                    return 0;
                }
                return 1;
            }));
        }
        dispatcher.register(root);
    }
}
