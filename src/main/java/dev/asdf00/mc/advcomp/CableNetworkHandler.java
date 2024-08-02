package dev.asdf00.mc.advcomp;

import dev.asdf00.mc.advcomp.blocks.cables.CableBlockEntity;
import dev.asdf00.mc.advcomp.blocks.cables.CableNetwork;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;
import dev.asdf00.mc.advcomp.types.IAcCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class CableNetworkHandler {
    private static final HashMap<DimensionType, CableNetworkHandler> handlers = new HashMap<>();

    private final LevelReader level;

    // OPTIM: could wipe out elements here in case a network rebuild happens to check them anyway out of pure luck
    private final Queue<BlockPos> rebuildQueue = new LinkedList<>();

    public CableNetworkHandler(LevelReader level) {
        this.level = level;
    }

    private static CableNetworkHandler getOrMakeNew(LevelAccessor level) {
        var dt = level.dimensionType();
        var val = handlers.get(dt);
        if (val == null) {
            val = new CableNetworkHandler(level);
            handlers.put(dt, val);
        }
        return val;
    }

    // needs to be called whenever a cable/computer/acConnectible is placed/loaded
    public static void markBlockPosForUpdate(@NotNull LevelAccessor level, @NotNull BlockPos e) {
        var handler = getOrMakeNew(level);
        handler.rebuildQueue.add(e);
    }

    // removed/unloaded
    public static void markBlockPosForUpdateIfExists(@NotNull LevelAccessor level, @NotNull BlockPos e) {
        var handler = handlers.get(level.dimensionType());
        if (handler != null)
            handler.rebuildQueue.add(e);
    }

    public void tick() {
        while (!rebuildQueue.isEmpty()) {
            var bp = rebuildQueue.remove();
            CableNetwork.onBlockPosChanged(this.level, bp);
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
            CableNetworkHandler.onLevelTick(event.level);
    }

    @SubscribeEvent
    public static void OnLevelUnloaded(LevelEvent.Unload event) {
        var level = event.getLevel();
        if (!level.isClientSide()) // only run on serverside
            CableNetworkHandler.cleanupHandlerIfExists(level);
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
