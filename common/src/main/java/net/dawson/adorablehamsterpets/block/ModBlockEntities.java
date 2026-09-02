package net.dawson.adorablehamsterpets.block;

import java.util.Set;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<HamsterBedBlockEntity>> HAMSTER_BED_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("hamster_bed_be", () ->
                    new BlockEntityType<>(HamsterBedBlockEntity::new, Set.of(ModBlocks.HAMSTER_BED.get())));

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}