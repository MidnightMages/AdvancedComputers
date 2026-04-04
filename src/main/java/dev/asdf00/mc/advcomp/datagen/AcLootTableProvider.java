package dev.asdf00.mc.advcomp.datagen;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.RegistryBlockItemPair;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AcLootTableProvider extends BlockLootSubProvider {

    public AcLootTableProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    private Iterable<Block> getAllBlocks() {
        return Arrays.stream(AdvancedComputers.class.getDeclaredFields()).filter(f -> f.getType().equals(RegistryBlockItemPair.class)).map(x -> {
                    try {
                        return x.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).map(x -> ((Block) ((RegistryBlockItemPair<?>) x).block().get()))
                .filter(x -> !(x instanceof ComputerBlock))
                .collect(Collectors.toList());
    }

    @Override
    protected void generate() {
        var allBlocks = getAllBlocks();

        for (var block : allBlocks) {
            this.dropSelf(block);
        }
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return getAllBlocks();
    }
}
