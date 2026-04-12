package dev.asdf00.mc.advcomp.datagen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.RegistryBlockItemPair;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlockTagGenerator extends BlockTagsProvider {

    public BlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider pProvider) {

        List<Block> allBlocks = new ArrayList<>(Arrays.stream(AdvancedComputers.class.getDeclaredFields()).filter(f -> f.getType().equals(RegistryBlockItemPair.class)).map(x -> {
            try {
                return x.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).map(x -> ((Block) ((RegistryBlockItemPair<?>) x).block().get())).toList());

        var woodComputer = AdvancedComputers.COMPUTER_BLOCK_WOOD.block().get();
        RuntimeAssert.RuntimeAssert(allBlocks.remove(woodComputer), "removal failed");
        var woodScreen = AdvancedComputers.SCREEN_BLOCK_WOOD.block().get();
        RuntimeAssert.RuntimeAssert(allBlocks.remove(woodScreen), "removal failed");
        this.tag(BlockTags.MINEABLE_WITH_AXE).add(woodComputer, woodScreen);

        Block[] allBlocksArray = allBlocks.toArray(Block[]::new);
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(allBlocksArray);
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(allBlocksArray);
    }
}
