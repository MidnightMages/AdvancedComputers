package dev.asdf00.mc.advcomp.types;

import dev.asdf00.mc.advcomp.CableNetworkHandler;
import dev.asdf00.mc.advcomp.blocks.cables.CableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseAcCableConnectableEntityBlock extends BlockEntity implements IAcCableConnectableEntity {
    public Set<CableNetwork> connectedNetworks;

    public BaseAcCableConnectableEntityBlock(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        connectedNetworks = new HashSet<>();
    }

    @Override
    public final Set<CableNetwork> getNetworkList() {
        return connectedNetworks;
    }

    // TODO make sure this fires on chunk unload and on block destroy
    private void onUnloadedOrDestroyed() {
        if (this.level != null)
            CableNetworkHandler.markBlockPosForUpdateIfExists(this.level, this.getBlockPos());
    }

    // TODO make sure this fires on chunk load and on block place
    private void onLoadOrResurrected() {
        if (this.level != null)
            CableNetworkHandler.markBlockPosForUpdate(this.level, this.getBlockPos());
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


//    @Override
//    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
//        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
//    }
//
//    @Override
//    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
//        super.onPlace(pState, pLevel, pPos, pOldState, pMovedByPiston);
//    }

        //    @Override
//    public void destroy(@NotNull LevelAccessor pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState) {
//        super.destroy(pLevel, pPos, pState);
//
//    }

//    @Nullable
//    @Override
//    public final BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState){
//        return newBlockEntityAC(pPos, pState);
//    }
//
//    @Nullable
//    public abstract BlockEntity newBlockEntityAC(@NotNull BlockPos pPos, @NotNull BlockState pState);
}
