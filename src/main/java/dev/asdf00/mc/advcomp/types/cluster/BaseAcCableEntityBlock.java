package dev.asdf00.mc.advcomp.types.cluster;

import dev.asdf00.mc.advcomp.CableClusterHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * IMPORTANT: This is NOT the right class to extend if you are writing a peripheral. Use BaseAcCableConnectableEntityBlock instead!
 * BUT: This IS the correct class if you want to make a new cable type :)
 */
public abstract class BaseAcCableEntityBlock extends BlockEntity implements IAcBaseCableConnectableEntity {
    public BaseAcCableEntityBlock(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    private boolean alreadyDestroyed = false;
    private boolean alreadyCreated = false;

    // TODO make sure this fires on chunk unload and on block destroy
    private void onUnloadedOrDestroyed() {
        if (this.level == null || this.level.isClientSide())
            return;

        alreadyCreated = false;
        if (!alreadyDestroyed) {
            alreadyDestroyed = true;
            CableClusterHandler.markBlockPosForUpdateIfExists(this.level, this.getBlockPos());
        }
    }

    // TODO make sure this fires on chunk load and on block place
    private void onLoadOrResurrected() {
        if (this.level == null || this.level.isClientSide())
            return;

        alreadyDestroyed = false;
        if (!alreadyCreated) {
            alreadyCreated = true;
            CableClusterHandler.markBlockPosForUpdate(this.level, this.getBlockPos());
        }
    }

    /**
     * IF YOU OVERRIDE THIS YOU MUST >>ABSOLUTELY<< CALL THIS 'super().' METHOD OR PROVIDE SIMILAR FUNCTIONALITY. OTHERWISE YOUR BLOCK WILL **NOT** WORK PROPERLY.
     */
    @Override
    public void onChunkUnloaded() {
        onUnloadedOrDestroyed();
        super.onChunkUnloaded();
    }

    /**
     * IF YOU OVERRIDE THIS YOU MUST >>ABSOLUTELY<< CALL THIS 'super().' METHOD OR PROVIDE SIMILAR FUNCTIONALITY. OTHERWISE YOUR BLOCK WILL **NOT** WORK PROPERLY.
     */
    @Override
    public void setRemoved() {
        onUnloadedOrDestroyed();
        super.setRemoved();
    }

    /**
     * IF YOU OVERRIDE THIS YOU MUST >>ABSOLUTELY<< CALL THIS 'super().' METHOD OR PROVIDE SIMILAR FUNCTIONALITY. OTHERWISE YOUR BLOCK WILL **NOT** WORK PROPERLY.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        onLoadOrResurrected();
    }

    /**
     * IF YOU OVERRIDE THIS YOU MUST >>ABSOLUTELY<< CALL THIS 'super().' METHOD OR PROVIDE SIMILAR FUNCTIONALITY. OTHERWISE YOUR BLOCK WILL **NOT** WORK PROPERLY.
     */
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        onLoadOrResurrected();
    }
}
