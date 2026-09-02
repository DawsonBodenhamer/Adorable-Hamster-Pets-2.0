package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
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
        if (!hamster.level().isClientSide() && !hamster.isBaby()) {
            hamster.setAge(Configs.AHP_MAIN.breedingCooldownSeconds.get() * 20);
        }
    }

    @Nullable
    public static AgeableMob createChild(
            HamsterEntity parent, ServerLevel world, AgeableMob mate) {
        HamsterEntity baby = ModEntities.HAMSTER.get().create(world);
        if (baby == null) return null;

        HamsterGenome babyGenome =
                HamsterGeneticsUtil.calculateBabyGenome(parent, mate, parent.getRandom());
        baby.setGenome(babyGenome);

        if (!Configs.AHP_MAIN.babiesSpawnWild) {
            UUID ownerUuid = parent.getOwnerUUID();
            if (ownerUuid != null) {
                baby.setOwnerUUID(ownerUuid);
                baby.setTame(true, true);
            }
        }

        baby.setBaby(true);
        UUID chosenParent = parent.getRandom().nextBoolean() ? parent.getUUID() : mate.getUUID();
        baby.setParentUuid(chosenParent);
        return baby;
    }

    // --- Death and Inventory Cleanup ---

    public static boolean handleDeath(HamsterEntity hamster) {
        if (!hamster.level().isClientSide() && Configs.AHP_MAIN.enableRespawnInBed.get()) {
            if (HamsterBedUtil.tryRespawnInBed(hamster)) {
                hamster.discard();
                return true;
            }
        }

        if (!hamster.level().isClientSide()) {
            if (!hamster.isTame() && Configs.AHP_MAIN.disableWildLootDrops) {
                hamster.getItems().clear();
            }

            for (ItemStack stack : hamster.getItems()) {
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(
                            hamster.level(),
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
            HamsterEntity hamster, ServerLevelAccessor world, MobSpawnType spawnReason) {
        initializeSpawn(hamster, world, spawnReason, false);
    }

    /**
     * Initializes a hamster with an already resolved cave-spawn context.
     */
    public static void initializeSpawn(
            HamsterEntity hamster,
            ServerLevelAccessor world,
            MobSpawnType spawnReason,
            boolean supplementalCaveSpawn) {
        AdorableHamsterPets.LOGGER.debug(
                "[AHP Spawn Debug] HamsterEntity.initialize called. SpawnReason: {}", spawnReason);

        boolean caveEnvironment = HamsterGeneticsUtil.isCaveEnvironment(
                world, hamster.blockPosition(), supplementalCaveSpawn);

        if (!world.isClientSide()) {
            int personalityId = hamster.getRandom().nextIntBetweenInclusive(1, 3);
            hamster.getEntityData().set(HamsterEntity.ANIMATION_PERSONALITY_ID, personalityId);
        }

        HamsterGenome wildGenome =
                HamsterGeneticsUtil.generateWildGenome(
                        world, hamster.blockPosition(), hamster.getRandom(), caveEnvironment);
        hamster.setGenome(wildGenome);

        if (!hamster.isTame()) {
            hamster.getAttribute(Attributes.MAX_HEALTH)
                    .setBaseValue(Configs.AHP_MAIN.wildMaxHealth.get());
            hamster.setHealth(hamster.getMaxHealth());
        }

        if (spawnReason == MobSpawnType.NATURAL || spawnReason == MobSpawnType.CHUNK_GENERATION) {
            hamster.totalAgeTicks = (1L + hamster.getRandom().nextInt(30)) * 24000L;
        } else {
            hamster.totalAgeTicks = 24000L;
        }

        HamsterInventoryUtil.generateWildLoot(hamster, hamster.getRandom(), caveEnvironment);

        if (world instanceof ServerLevel serverWorld) {
            RedstoneFeverUtil.tryApplyNaturalFever(hamster, serverWorld, spawnReason);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterLifecycleUtil() {}
}
