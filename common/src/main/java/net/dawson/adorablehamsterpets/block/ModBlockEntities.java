package net.dawson.adorablehamsterpets.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.RegistryKeys;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<HamsterBedBlockEntity>> HAMSTER_BED_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("hamster_bed_be", () ->
                    BlockEntityType.Builder.create(HamsterBedBlockEntity::new, ModBlocks.HAMSTER_BED.get()).build(null));

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}