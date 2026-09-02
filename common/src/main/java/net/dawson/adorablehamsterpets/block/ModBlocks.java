package net.dawson.adorablehamsterpets.block;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import java.util.function.Supplier;

public class ModBlocks {

    // --- 1. Create a DeferredRegister for Blocks ---
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, Registries.BLOCK);

    // --- 2. Change Block fields to RegistrySuppliers ---
    public static final RegistrySupplier<Block> GREEN_BEANS_CROP = registerBlock("green_beans_crop",
            () -> new GreenBeansCropBlock(blockProps("green_beans_crop", BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY).noOcclusion())));

    public static final RegistrySupplier<Block> CUCUMBER_CROP = registerBlock("cucumber_crop",
            () -> new CucumberCropBlock(blockProps("cucumber_crop", BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY).noOcclusion())));

    public static final RegistrySupplier<Block> WILD_GREEN_BEAN_BUSH = registerBlock("wild_green_bean_bush",
            () -> new WildGreenBeanBushBlock(blockProps("wild_green_bean_bush", BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                    .noOcclusion()
                    .noCollision()
                    .randomTicks()
                    .sound(SoundType.SWEET_BERRY_BUSH))));

    public static final RegistrySupplier<Block> WILD_CUCUMBER_BUSH = registerBlock("wild_cucumber_bush",
            () -> new WildCucumberBushBlock(blockProps("wild_cucumber_bush", BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                    .noOcclusion()
                    .noCollision()
                    .randomTicks()
                    .sound(SoundType.SWEET_BERRY_BUSH))));

    public static final RegistrySupplier<Block> SUNFLOWER_BLOCK = registerBlock("sunflower_block",
            () -> new SunflowerBlock(blockProps("sunflower_block", BlockBehaviour.Properties.ofFullCopy(Blocks.SUNFLOWER)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(SunflowerBlock.LIT) ? 15 : 0))));

    public static final RegistrySupplier<Block> HAMSTER_BED = registerBlock("hamster_bed",
            () -> new HamsterBedBlock(blockProps("hamster_bed", BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(0.5F).sound(SoundType.WOOD).noOcclusion())));

    // --- Crates ---
    public static final RegistrySupplier<Block> ACORN_CRATE = registerBlock("acorn_crate",
            () -> new Block(blockProps("acorn_crate", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.WOOD).strength(2.0F, 3.0F))));

    public static final RegistrySupplier<Block> CUCUMBER_CRATE = registerBlock("cucumber_crate",
            () -> new Block(blockProps("cucumber_crate", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.WOOD).strength(2.0F, 3.0F))));

    public static final RegistrySupplier<Block> GREEN_BEANS_CRATE = registerBlock("green_beans_crate",
            () -> new Block(blockProps("green_beans_crate", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.WOOD).strength(2.0F, 3.0F))));

    public static final RegistrySupplier<Block> HAMSTER_FOOD_MIX_CRATE = registerBlock("hamster_food_mix_crate",
            () -> new Block(blockProps("hamster_food_mix_crate", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.WOOD).strength(2.0F, 3.0F))));

    // --- 3. Private Helper Method for Block Registration ---
    /** 26.2: blocks must know their registry key before construction. */
    private static BlockBehaviour.Properties blockProps(String name, BlockBehaviour.Properties properties) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, name)));
    }

    private static RegistrySupplier<Block> registerBlock(String name, Supplier<Block> blockSupplier) {
        return BLOCKS.register(name, blockSupplier);
    }

    // --- 4. Main Registration Call ---
    public static void register() {
        BLOCKS.register();
    }
}