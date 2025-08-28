package dev.asdf00.mc.advcomp.blocks.computer;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.RegistryBlockItemPair;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public enum ComputerTier implements StringRepresentable {
    Wood(AdvancedComputers.COMPUTER_BLOCK_WOOD),
    Iron(AdvancedComputers.COMPUTER_BLOCK),
//    Gold,
    Diamond(AdvancedComputers.COMPUTER_BLOCK_DIAMOND),
    Netherite(AdvancedComputers.COMPUTER_BLOCK_NETHERITE);

    ComputerTier(RegistryBlockItemPair<Block> blockType) {

    }

    @Override
    public @NotNull String getSerializedName() {
        return this.toString().toLowerCase();
    }
}
