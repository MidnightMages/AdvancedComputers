package dev.asdf00.mc.advcomp.lua.vm;

import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.NetCodeUtils;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;
import dev.asdf00.mc.advcomp.lua.components.GpuUD;
import dev.asdf00.mc.advcomp.lua.components.TextBufferUD;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class LuaSafepointHandler implements LuaUserData {
    private static final long SECOND = 1_000_000_000;
    private static final int BUF_SEND_PER_SEC = 30; // TODO make this a config option

    private final LuaVirtualMachine acVm;

    private long lastSafepointTimestamp = 0;
    private long lastCompilationStartedTimestamp = 0;
    private long lastBufferSend = 0;
    private long lastDelayedEventQueueChecked = 0;
    private final double sleepFactor;

    LuaSafepointHandler(LuaVirtualMachine acVm, double sleepFactor) {
        this.acVm = acVm;
        this.sleepFactor = sleepFactor;
    }

    void refundNanos(long nanos) {
        if (nanos > 0)
            lastSafepointTimestamp += nanos;
    }

    void handleVmEvent(LuaVM vmObj, LuaVM.HookType eventType) {
        if (Thread.interrupted()) {
            if (vmObj.isStopping()) {
                // skip any safepoint handling and let the vm exit gracefully
                return;
            } else {
                // this is not a suspend but a kill
                throw new LvmKillException();
            }
        }
        switch (eventType) {
            case COMPILATION_STARTED -> {
                lastCompilationStartedTimestamp = System.nanoTime();
                sendTextBufferUpdates();
            }
            case COMPILATION_FINISHED -> {
                // fake the last safepoint timestamp to effectively refund the compilation time
                long timeSpentCompiling = System.nanoTime() - lastCompilationStartedTimestamp;
                refundNanos(timeSpentCompiling);
            }
            case SAFEPOINT_REACHED -> {
                // capture time spent in lua calcuation
                long now = System.nanoTime();
                // maybe send text buffers
                if (now - lastBufferSend > SECOND / BUF_SEND_PER_SEC) {
                    if (sendTextBufferUpdates()) {
                        lastBufferSend = now;
                    }
                }
                if (now - lastDelayedEventQueueChecked > SECOND / 50) {
                    acVm.processDelayedEventsAtSafepoint();
                    lastDelayedEventQueueChecked = now;
                }
                // do the timeout calculation
                long timeSpentNs = (now - lastSafepointTimestamp);
                long sleepTimeNs = (long) Math.ceil(timeSpentNs * sleepFactor);
                long sleepTimeMs = sleepTimeNs / 1_000_000;
                if (sleepTimeMs > 10) {
                    try {
                        Thread.sleep(sleepTimeMs, (int) (sleepTimeNs % 1_000_000));
                    } catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt();
                        if (!vmObj.isStopping()) {
                            // this is not a suspend but a kill
                            throw new LvmKillException();
                        }
                    }
                    lastSafepointTimestamp = System.nanoTime();
                }
            }
            case VM_STARTED, VM_RESUMED -> {
                lastSafepointTimestamp = System.nanoTime();
            }
        }
    }

    /**
     * Always call this method before executing a long-running or blocking LUA function to allow for proper syncing with
     * the client.
     */
    void beforeLongLuaOperation() {
        sendTextBufferUpdates();
    }

    /**
     * Sends all pending {@link TextBufferUD} updates for the given LVM. Call this method before any
     * long-running Lua library function to avoid screen-lag due to the LVM not hitting a safepoint
     * in the upcoming time.
     *
     * @return if updates were sent.
     */
    boolean sendTextBufferUpdates() {
        if (!acVm.dirtyScreenBlockEntities.isEmpty()) {
            var gpu = acVm.componentReg.getSingleOfType(GpuUD.class);

            ScreenBlockEntity dirtyEntity;
            while ((dirtyEntity = acVm.dirtyScreenBlockEntities.poll()) != null) {
                var buffer = gpu.screenBufferMap.get(dirtyEntity);
                if (buffer != null)
                    acVm.dirtyBuffers.add(buffer);
            }
        }

        if (acVm.dirtyBuffers.isEmpty()) {
            return false;
        }
        for (TextBufferUD buf : acVm.dirtyBuffers) {
            // this is still the LUA thread, so thread-safety is not a concern here
            // here we send the stuff
            Set<ScreenBlockEntity> screens = buf.getAssociatedScreens();
            if (screens.isEmpty()) {
                // no screen to sent to
                continue;
            }
            if (buf.isAlive) {
                NetCodeUtils.sendToClient(
                        PacketDistributor.ALL.noArg(),
                        new ScreenBlockEntity.ScreenContentToClientEvent(screens.toArray(ScreenBlockEntity[]::new), "clearGuiText", ""));
            }
            String text = buf.getTextAsString();
            NetCodeUtils.sendToClient(
                    PacketDistributor.ALL.noArg(),
                    new ScreenBlockEntity.ScreenContentToClientEvent(screens.toArray(ScreenBlockEntity[]::new), "setGuiText", text));
        }
        acVm.dirtyBuffers.clear();
        return true;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return new byte[0];
    }

    @LuaDeserializer
    public static LuaSafepointHandler luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return ((LuaVirtualMachine) additionalData).timeTracker;
    }
}
