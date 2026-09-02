package net.dawson.adorablehamsterpets.fabric.datagen;

import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    private static final TagKey<Block> STORAGE_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("c", "storage_blocks")
    );

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        tag(BlockTags.CROPS)
                .add(ModBlocks.CUCUMBER_CROP.get())
                .add(ModBlocks.GREEN_BEANS_CROP.get());

        tag(BlockTags.MAINTAINS_FARMLAND)
                .add(ModBlocks.CUCUMBER_CROP.get())
                .add(ModBlocks.GREEN_BEANS_CROP.get());

        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.ACORN_CRATE.get())
                .add(ModBlocks.CUCUMBER_CRATE.get())
                .add(ModBlocks.GREEN_BEANS_CRATE.get())
                .add(ModBlocks.HAMSTER_FOOD_MIX_CRATE.get());

        tag(STORAGE_BLOCKS)
                .add(ModBlocks.ACORN_CRATE.get())
                .add(ModBlocks.CUCUMBER_CRATE.get())
                .add(ModBlocks.GREEN_BEANS_CRATE.get())
                .add(ModBlocks.HAMSTER_FOOD_MIX_CRATE.get());
    }
}
