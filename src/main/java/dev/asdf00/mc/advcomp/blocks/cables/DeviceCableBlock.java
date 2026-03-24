package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.cables.types.BaseCableBlock;

public class DeviceCableBlock extends BaseCableBlock {
    public DeviceCableBlock(Properties pProperties) {
        super(AdvancedComputers.CLUSTER_TYPE_DEVICE, pProperties);
    }
}
