package dev.asdf00.mc.advcomp.blocks.cables;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.blocks.cables.types.BaseCableBlock;

public class NetworkCableBlock extends BaseCableBlock {
    public NetworkCableBlock(Properties pProperties) {
        super(AdvancedComputers.CLUSTER_TYPE_NETWORK, pProperties);
    }
}
