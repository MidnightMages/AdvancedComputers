package dev.asdf00.mc.advcomp.datagen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.RegistryBlockItemPair;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.common.data.BlockTagsProvider;
import net.neoforged.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class BlockTagGenerator extends BlockTagsProvider {

    public BlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider pProvider) {

        var allBlocks = Arrays.stream(AdvancedComputers.class.getDeclaredFields()).filter(f -> f.getType().equals(RegistryBlockItemPair.class)).map(x -> {
            try {
                return x.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).map(x -> ((Block) ((RegistryBlockItemPair<?>) x).block().get())).toArray(Block[]::new);
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(allBlocks);
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(allBlocks);
    }
}
