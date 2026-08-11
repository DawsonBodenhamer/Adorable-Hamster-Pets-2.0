package net.dawson.adorablehamsterpets.fabric.datagen;

import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    private static final TagKey<Block> STORAGE_BLOCKS = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of("c", "storage_blocks")
    );

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                .add(ModBlocks.ACORN_CRATE.get())
                .add(ModBlocks.CUCUMBER_CRATE.get())
                .add(ModBlocks.GREEN_BEANS_CRATE.get())
                .add(ModBlocks.HAMSTER_FOOD_MIX_CRATE.get());

        getOrCreateTagBuilder(STORAGE_BLOCKS)
                .add(ModBlocks.ACORN_CRATE.get())
                .add(ModBlocks.CUCUMBER_CRATE.get())
                .add(ModBlocks.GREEN_BEANS_CRATE.get())
                .add(ModBlocks.HAMSTER_FOOD_MIX_CRATE.get());
    }
}
