package dev.asdf00.mc.advcomp.blocks;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.components.AcBlockEntityComponent;
import dev.asdf00.mc.advcomp.types.capabilities.Capabilities;
import dev.asdf00.mc.advcomp.types.capabilities.DeviceCableConnectableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Base class for block entities that can be connected via a peripheral cable and are represented by a userdata component
 */
public abstract class BasePeripheralComponentBlockEntity extends BaseCableConnectableBlockEntity implements AcBlockEntityComponent {
    private final LazyOptional<DeviceCableConnectableEntity> lazyCableConnectable;
    private final ConcurrentLinkedQueue<Runnable> tickThreadQueue = new ConcurrentLinkedQueue<>();

    public <T extends BlockEntity> BasePeripheralComponentBlockEntity(BlockEntityType<T> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, List.of(AdvancedComputers.CLUSTER_TYPE_DEVICE));
        this.lazyCableConnectable = LazyOptional.of(() -> this);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        while (true) {
            var newItem = tickThreadQueue.poll();
            if (newItem == null)
                return;
            newItem.run();
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == Capabilities.CABLE_CONNECTABLE)
            return lazyCableConnectable.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyCableConnectable.invalidate();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public void runOnTickThread(Runnable toExecute) {
        runOnTickThread(() -> {
            toExecute.run();
            return null;
        });
    }

    public <T> T runOnTickThread(Supplier<T> toExecute) {
        //noinspection unchecked
        T[] result = (T[]) new Object[1];
        Throwable[] resultException = new Throwable[1];
        synchronized (result) {
            this.tickThreadQueue.add(() -> {
                try {
                    result[0] = toExecute.get();
                } catch (Throwable exception) {
                    resultException[0] = exception;
                }
                synchronized (result) {
                    result.notifyAll();
                }
            });
            try {
                result.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LuaJavaError("operation was interrupted and may or may not have been executed");
            }
            if (resultException[0] != null) {
                if (resultException[0] instanceof Error e)
                    throw new Error(e);
                else if (resultException[0] instanceof LuaJavaError e) {
                    throw new LuaJavaError(e.getMessage(), e);
                } else {
                    assert resultException[0] instanceof RuntimeException : resultException[0];
                    throw new RuntimeException(resultException[0]);
                }
            }
            return result[0];
        }
    }
}
