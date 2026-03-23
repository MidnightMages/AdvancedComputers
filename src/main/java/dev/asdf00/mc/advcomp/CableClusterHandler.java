package dev.asdf00.mc.advcomp;

import dev.asdf00.mc.advcomp.blocks.cables.CableCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.event.TickEvent;
import net.neoforged.event.level.LevelEvent;
import net.neoforged.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class CableClusterHandler {
    private static final HashMap<DimensionType, CableClusterHandler> handlers = new HashMap<>();

    private final Level level;

    // OPTIM: could wipe out elements here in case a network rebuild happens to check them anyway out of pure luck
    private final Queue<BlockPos> rebuildQueue = new LinkedList<>();

    public CableClusterHandler(Level level) {
        this.level = level;
    }

    private static CableClusterHandler getOrMakeNew(Level level) {
        var dt = level.dimensionType();
        var val = handlers.get(dt);
        if (val == null) {
            val = new CableClusterHandler(level);
            handlers.put(dt, val);
        }
        return val;
    }

    // needs to be called whenever a cable/computer/acConnectible is placed/loaded
    public static void markBlockPosForUpdate(@NotNull Level level, @NotNull BlockPos e) {
        if (level.isLoaded(e)) {
            var handler = getOrMakeNew(level);
            handler.rebuildQueue.add(e);
        }
    }

    // removed/unloaded
    public static void markBlockPosForUpdateIfExists(@NotNull Level level, @NotNull BlockPos e) {
        var handler = handlers.get(level.dimensionType());
        if (handler != null && level.isLoaded(e))
            handler.rebuildQueue.add(e);
    }

    public void tick() {
        while (!rebuildQueue.isEmpty()) {
            var bp = rebuildQueue.remove();
            if (level.isLoaded(bp))
                CableCluster.onBlockPosChanged(this.level, bp);
        }
    }

    public static void onLevelTick(LevelAccessor level) {
        var handler = handlers.get(level.dimensionType());
        if (handler != null) { // if we dont have a handler, there is no point in ticking it
            handler.tick();
        }
    }

    public static void cleanupHandlerIfExists(LevelAccessor level) { // TODO cross dim networks will need special handling probably
        handlers.remove(level.dimensionType());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent event) {
        if (event.side.isServer())
            CableClusterHandler.onLevelTick(event.level);
    }

    @SubscribeEvent
    public static void OnLevelUnloaded(LevelEvent.Unload event) {
        var level = event.getLevel();
        if (!level.isClientSide()) // only run on serverside
            CableClusterHandler.cleanupHandlerIfExists(level);
    }
//    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
//    // this is quite iffy. hopefully no mod with lowest priority cancels this event
//    public static void onBlockBreak(BlockEvent.BreakEvent event) {
//        var level = event.getLevel();
//        if (!level.isClientSide()) {
//            var bp = event.getPos();
//            var be = level.getBlockEntity(bp);
//            if (be instanceof IAcCableConnectableEntity || be instanceof ComputerBlockEntity || be instanceof CableBlockEntity)
//                markBlockPosForUpdateIfExists(level, event.getPos());
//        }
//    }
//
//    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
//    // this is quite iffy. hopefully no mod with lowest priority cancels this event
//    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
//        var level = event.getLevel();
//        if (!level.isClientSide()) {
//            var bp = event.getPos();
//            var be = level.getBlockEntity(bp);
//            if (be instanceof IAcCableConnectableEntity || be instanceof ComputerBlockEntity || be instanceof CableBlockEntity)
//                markBlockPosForUpdateIfExists(level, event.getPos());
//        }
//    }
}
