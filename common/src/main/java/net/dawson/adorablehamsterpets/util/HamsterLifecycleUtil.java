package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.world.ServerWorldAccess;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Manages hamster spawn initialization, growth, breeding, and death lifecycle mechanics.
 */
public final class HamsterLifecycleUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                           Static Lifecycle Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Growth and Breeding ---

    public static void onGrowUp(HamsterEntity hamster) {
        if (!hamster.getWorld().isClient() && !hamster.isBaby()) {
            hamster.setBreedingAge(Configs.AHP_MAIN.breedingCooldownSeconds.get() * 20);
        }
    }

    @Nullable
    public static PassiveEntity createChild(
            HamsterEntity parent, ServerWorld world, PassiveEntity mate) {
        HamsterEntity baby = ModEntities.HAMSTER.get().create(world);
        if (baby == null) return null;

        HamsterGenome babyGenome =
                HamsterGeneticsUtil.calculateBabyGenome(parent, mate, parent.getRandom());
        baby.setGenome(babyGenome);

        if (!Configs.AHP_MAIN.babiesSpawnWild) {
            UUID ownerUuid = parent.getOwnerUuid();
            if (ownerUuid != null) {
                baby.setOwnerUuid(ownerUuid);
                baby.setTamed(true, true);
            }
        }

        baby.setBaby(true);
        UUID chosenParent = parent.getRandom().nextBoolean() ? parent.getUuid() : mate.getUuid();
        baby.setParentUuid(chosenParent);
        return baby;
    }

    // --- Death and Inventory Cleanup ---

    public static boolean handleDeath(HamsterEntity hamster) {
        if (!hamster.getWorld().isClient() && Configs.AHP_MAIN.enableRespawnInBed.get()) {
            if (HamsterBedUtil.tryRespawnInBed(hamster)) {
                hamster.discard();
                return true;
            }
        }

        if (!hamster.getWorld().isClient()) {
            if (!hamster.isTamed() && Configs.AHP_MAIN.disableWildLootDrops) {
                hamster.getItems().clear();
            }

            for (ItemStack stack : hamster.getItems()) {
                if (!stack.isEmpty()) {
                    ItemScatterer.spawn(
                            hamster.getWorld(),
                            hamster.getX(),
                            hamster.getY(),
                            hamster.getZ(),
                            stack);
                }
            }
            hamster.getItems().clear();
            HamsterInventoryUtil.updateCheekStates(hamster);
        }
        return false;
    }

    // --- Spawn Initialization ---

    public static void initializeSpawn(
            HamsterEntity hamster, ServerWorldAccess world, SpawnReason spawnReason) {
        initializeSpawn(hamster, world, spawnReason, false);
    }

    /**
     * Initializes a hamster with an already resolved cave-spawn context.
     */
    public static void initializeSpawn(
            HamsterEntity hamster,
            ServerWorldAccess world,
            SpawnReason spawnReason,
            boolean supplementalCaveSpawn) {
        AdorableHamsterPets.LOGGER.debug(
                "[AHP Spawn Debug] HamsterEntity.initialize called. SpawnReason: {}", spawnReason);

        boolean caveEnvironment = HamsterGeneticsUtil.isCaveEnvironment(
                world, hamster.getBlockPos(), supplementalCaveSpawn);

        if (!world.isClient()) {
            int personalityId = hamster.getRandom().nextBetween(1, 3);
            hamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, personalityId);
        }

        HamsterGenome wildGenome =
                HamsterGeneticsUtil.generateWildGenome(
                        world, hamster.getBlockPos(), hamster.getRandom(), caveEnvironment);
        hamster.setGenome(wildGenome);

        if (!hamster.isTamed()) {
            hamster.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                    .setBaseValue(Configs.AHP_MAIN.wildMaxHealth.get());
            hamster.setHealth(hamster.getMaxHealth());
        }

        if (spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.CHUNK_GENERATION) {
            hamster.totalAgeTicks = (1L + hamster.getRandom().nextInt(30)) * 24000L;
        } else {
            hamster.totalAgeTicks = 24000L;
        }

        HamsterInventoryUtil.generateWildLoot(hamster, hamster.getRandom(), caveEnvironment);

        if (world instanceof ServerWorld serverWorld) {
            RedstoneFeverUtil.tryApplyNaturalFever(hamster, serverWorld, spawnReason);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterLifecycleUtil() {}
}
