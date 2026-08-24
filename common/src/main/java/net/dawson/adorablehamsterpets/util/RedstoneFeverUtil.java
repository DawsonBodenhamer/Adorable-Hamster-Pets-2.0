package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.RedstoneFeverState;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative transition and eligibility policy for Redstone Fever.
 */
public final class RedstoneFeverUtil {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ───────────────────────────────────────────────────────────────────────────────*/

    public static final long SUNLIGHT_TICKS_PER_DAY = 12_000L;
    private static final double TREMOR_SPIKE_FREQUENCY = 0.06D; // Bigger = more often
    private static final double TREMOR_SPIKE_PHASE_MULTIPLIER = 0.61803398875D;
    private static final double TREMOR_WINDOW_LOWER_BOUND = 0.454D; // Clip lower part of sine wave to shorten duration by ~30%
    private static final double VISIBLE_TREMOR_SPIKE_THRESHOLD = 0.92D; // Higher threshold hides smaller spikes
    private static final double SHIVER_SOUND_ALIGNMENT_OFFSET_TICKS = -7;
    public static final UUID FEVER_MOVEMENT_SPEED_MODIFIER_ID =
            UUID.fromString("4d52e31c-20ec-4e58-9f91-71587b77b4b3");
    private static final Set<String> WARNED_INVALID_DIMENSIONS = ConcurrentHashMap.newKeySet();

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ───────────────────────────────────────────────────────────────────────────────*/

    // --- 1. Fever Eligibility and Transitions ---
    public static void tryApplyNaturalFever(
            HamsterEntity hamster, ServerWorld world, SpawnReason spawnReason) {
        if (!Configs.AHP_MAIN.enableRedstoneFever) return;
        if (!RedstoneFeverPolicy.isEligibleFreshSpawnReason(spawnReason)) return;
        if (!Configs.AHP_WORLDGEN.enableNaturalRedstoneFeverSpawning) return;
        if (hamster.getY() > Configs.AHP_WORLDGEN.maximumRedstoneFeverSpawnY.get()) return;
        if (world.isSkyVisible(hamster.getBlockPos())) return;
        if (!isAllowedDimension(world)) return;
        if (Configs.AHP_WORLDGEN.requireRedstoneFeverCaveBiomeTags
                && !world.getBiome(hamster.getBlockPos()).isIn(CaveBiomeTagHolder.CAVE_BIOMES)) {
            return;
        }
        if (hamster.getRandom().nextInt(100) >= Configs.AHP_WORLDGEN.redstoneFeverChance.get()) return;
        applyFever(hamster, false);
    }

    public static boolean applyFever(HamsterEntity hamster, boolean resolveCommissionedRoll) {
        if (!Configs.AHP_MAIN.enableRedstoneFever) return false;
        if (hamster.isTamed() || hamster.getRedstoneFeverState().isFevered()) return false;

        hamster.getRedstoneFeverState().setFevered(true);
        hamster.getRedstoneFeverState().setScarVariant(hamster.getRandom().nextInt(3));
        if (resolveCommissionedRoll) {
            hamster.getRedstoneFeverState().setCommissionedRollResolved(true);
        }
        hamster.synchronizeRedstoneFeverVisualState();
        hamster.setTarget(null);
        hamster.getNavigation().stop();
        if (resolveCommissionedRoll) {
            SoundEvent hiss = ModSounds.getRandomSoundFrom(
                    ModSounds.HAMSTER_HISS_SOUNDS, hamster.getRandom());
            if (hiss != null) hamster.playSound(hiss, 0.7F, hamster.getSoundPitch());
        }
        return true;
    }

    /**
     * Enforces the global feature toggle for a loaded hamster before ordinary behavior runs.
     * Disabling the feature is an administrative cleanup, so it never awards rescue credit or
     * presents the player-facing cure effects used by sunlight and command cures.
     */
    public static void enforceFeatureToggle(HamsterEntity hamster) {
        if (Configs.AHP_MAIN.enableRedstoneFever) return;

        if (hamster.getRedstoneFeverState().isFevered()) {
            clearDisabledState(hamster);
        } else {
            clearMovementSpeedModifier(hamster);
            if (hamster.hasRedstoneFever()) hamster.synchronizeRedstoneFeverVisualState();
        }
    }

    /**
     * Normalizes state reconstructed from NBT or a typed transfer before it can be synchronized or
     * consumed by behavior.
     */
    public static void normalizeDisabledState(HamsterEntity hamster) {
        if (!Configs.AHP_MAIN.enableRedstoneFever) enforceFeatureToggle(hamster);
    }

    private static void clearDisabledState(HamsterEntity hamster) {
        RedstoneFeverState state = hamster.getRedstoneFeverState();
        state.setFirstLeadRescuerUuid(null);
        state.setFirstSunlightTargetUuid(null);
        state.setFevered(false);
        hamster.setRedstoneFeverBurstActive(false);
        hamster.setTarget(null);
        hamster.getNavigation().stop();
        hamster.synchronizeRedstoneFeverVisualState();
        clearMovementSpeedModifier(hamster);
    }

    public static void cure(HamsterEntity hamster) {
        // --- 1. Resolve Advancement Credit ---
        UUID creditedPlayer = hamster.getRedstoneFeverState().getFirstLeadRescuerUuid();
        if (creditedPlayer == null) {
            creditedPlayer = hamster.getRedstoneFeverState().getFirstSunlightTargetUuid();
        }
        if (creditedPlayer != null && hamster.getWorld() instanceof ServerWorld world) {
            RedstoneFeverCureCreditState.awardOrQueue(world, creditedPlayer);
        }

        // --- 2. Clear Condition Behavior ---
        hamster.getRedstoneFeverState().setCommissionedRollResolved(true);
        hamster.getRedstoneFeverState().setFevered(false);
        hamster.setRedstoneFeverBurstActive(false);
        hamster.synchronizeRedstoneFeverVisualState();
        hamster.setTarget(null);
        hamster.getNavigation().stop();

        // --- 3. Present Cure ---
        SoundEvent affection = ModSounds.getRandomSoundFrom(
                ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
        if (affection != null) hamster.playSound(affection, 1.0F, 1.0F);
        if (hamster.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    hamster.getX(), hamster.getBodyY(0.55D), hamster.getZ(),
                    12, 0.3D, 0.25D, 0.3D, 0.08D);
        }
    }

    public static void cureAdministratively(HamsterEntity hamster) {
        // Admin cures deliberately erase rescuers before shared cure transition
        hamster.getRedstoneFeverState().setFirstLeadRescuerUuid(null);
        hamster.getRedstoneFeverState().setFirstSunlightTargetUuid(null);
        cure(hamster);
    }

    // --- 2. Server Tick Policy ---
    public static void tick(HamsterEntity hamster) {
        if (!(hamster.getWorld() instanceof ServerWorld world)) return;
        if (!Configs.AHP_MAIN.enableRedstoneFever) {
            enforceFeatureToggle(hamster);
            return;
        }

        if (!hamster.hasRedstoneFever()) {
            hamster.setRedstoneFeverBurstActive(false);
            hamster.getRedstoneFeverState().clearScheduledShiver();
            tickCommissionedReveal(hamster, world);
            return;
        }

        if (hamster.isTamed()) {
            // Defensive invariant for transfers or external taming integrations
            cure(hamster);
            return;
        }

        tickAudio(hamster);
        captureLeadRescuer(hamster);

        // Sunlight and ambient particles need only one server check per second
        if (hamster.age % 20 != 0) return;
        tickSunlightCure(hamster, world);

        double severity = getSeverity(hamster);
        if (hamster.getRandom().nextDouble() < 0.45D * severity) {
            spawnRedstoneParticles(hamster, 2, 0.0F);
        }
    }

    // --- 3. Shared Severity and Presentation ---
    public static double getSeverity(HamsterEntity hamster) {
        long required = SUNLIGHT_TICKS_PER_DAY * Configs.AHP_MAIN.redstoneFeverSunlightCureDays.get();
        return 1.0D - MathHelper.clamp(
                (double) hamster.getRedstoneFeverState().getSunlightTicks() / required,
                0.0D,
                1.0D
        );
    }

    public static void reconcileMovementSpeed(HamsterEntity hamster) {
        if (hamster.getWorld().isClient()) return;

        EntityAttributeInstance speed = hamster.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) return;

        boolean shouldHaveModifier = hamster.getRedstoneFeverState().isFevered();
        boolean hasModifier = speed.getModifier(FEVER_MOVEMENT_SPEED_MODIFIER_ID) != null;
        if (shouldHaveModifier && !hasModifier) {
            speed.addTemporaryModifier(new EntityAttributeModifier(
                    FEVER_MOVEMENT_SPEED_MODIFIER_ID,
                    "Redstone Fever movement speed",
                    0.5D,
                    EntityAttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!shouldHaveModifier && hasModifier) {
            speed.removeModifier(FEVER_MOVEMENT_SPEED_MODIFIER_ID);
        }
    }

    public static void clearMovementSpeedModifier(HamsterEntity hamster) {
        if (hamster.getWorld().isClient()) return;

        EntityAttributeInstance speed = hamster.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(FEVER_MOVEMENT_SPEED_MODIFIER_ID) != null) {
            speed.removeModifier(FEVER_MOVEMENT_SPEED_MODIFIER_ID);
        }
    }

    public static boolean isEligiblePlayer(PlayerEntity player) {
        return RedstoneFeverPolicy.isEligiblePlayerState(
                player.isAlive(),
                player.isRemoved(),
                player.isCreative(),
                player.isSpectator(),
                player.getAbilities().invulnerable);
    }

    public static boolean isEligibleFeverTarget(
            HamsterEntity hamster, @Nullable LivingEntity target) {
        if (target == null
                || target == hamster
                || !target.isAlive()
                || target.isRemoved()) {
            return false;
        }
        if (target instanceof PlayerEntity player) {
            return isEligiblePlayer(player);
        }
        return Configs.AHP_MAIN.redstoneFeverAttackMostLivingMobs
                && !target.isInvulnerable();
    }

    public static boolean isWithinTargetingRange(
            HamsterEntity hamster, @Nullable LivingEntity target) {
        return target != null
                && hamster.squaredDistanceTo(target)
                        <= Math.pow(Configs.AHP_MAIN.redstoneFeverTargetingRange.get(), 2.0D);
    }

    public static void spawnRedstoneParticles(HamsterEntity hamster, int count, float velocity) {
        ParticleEffectsUtil.spawnParticlesOnEntity(
                hamster,
                new DustParticleEffect(new Vector3f(0.85F, 0.05F, 0.02F), 1.0F),
                count,
                0.5D / hamster.getWidth(),
                0.4D / hamster.getHeight(),
                velocity,
                hamster.getHeight() * 0.05D);
    }

    /**
     * Returns the positive portion of the owner-tuned tremor spike waveform.
     *
     * <p>The renderer and server audio both use this deterministic authority so a visible spike
     * can produce at most one synchronized shiver sound.
     */
    public static double getTremorSpike(double time, UUID entityUuid) {
        double entityPhase = entityUuid.hashCode() * TREMOR_SPIKE_PHASE_MULTIPLIER;
        double rawSin = Math.sin(time * TREMOR_SPIKE_FREQUENCY + entityPhase);
        return Math.max(0.0D, (rawSin - TREMOR_WINDOW_LOWER_BOUND) / (1.0D - TREMOR_WINDOW_LOWER_BOUND));
    }

    public static double getNextTremorSpikePeakTime(double time, UUID entityUuid) {
        double entityPhase = entityUuid.hashCode() * TREMOR_SPIKE_PHASE_MULTIPLIER;
        double period = Math.PI * 2.0D;
        double currentArgument = time * TREMOR_SPIKE_FREQUENCY + entityPhase;
        double nextPeakIndex = Math.floor((currentArgument - Math.PI / 2.0D) / period) + 1.0D;
        double nextPeakArgument = Math.PI / 2.0D + nextPeakIndex * period;
        return (nextPeakArgument - entityPhase) / TREMOR_SPIKE_FREQUENCY;
    }

    private static void tickAudio(HamsterEntity hamster) {
        long worldTime = hamster.getWorld().getTime();
        RedstoneFeverState state = hamster.getRedstoneFeverState();
        double tremorSpike = getTremorSpike(worldTime, hamster.getUuid());

        if (hamster.isRedstoneFeverBurstActive()) {
            state.clearScheduledShiver();
            if (tremorSpike <= VISIBLE_TREMOR_SPIKE_THRESHOLD) {
                state.setShiverPeakArmed(true);
            }
            return;
        }

        // Re-arm only after the shared visual waveform exits its visible spike.
        if (!state.isShiverPeakArmed()) {
            if (tremorSpike <= VISIBLE_TREMOR_SPIKE_THRESHOLD) {
                state.setShiverPeakArmed(true);
            }
            return;
        }

        if (state.getScheduledShiver() == null) {
            ModSounds.TimedSound selectedSound =
                    ModSounds.getRandomTimedShiverSound(hamster.getRandom());
            long durationTicks = selectedSound.durationTicks();
            double peakTime = getNextTremorSpikePeakTime(worldTime, hamster.getUuid());
            double tremorPeriodTicks = Math.PI * 2.0D / TREMOR_SPIKE_FREQUENCY;
            double triggerTime = peakTime - selectedSound.clipPeakOffsetTicks() + SHIVER_SOUND_ALIGNMENT_OFFSET_TICKS;
            while (triggerTime <= worldTime) {
                peakTime += tremorPeriodTicks;
                triggerTime = peakTime - selectedSound.clipPeakOffsetTicks() + SHIVER_SOUND_ALIGNMENT_OFFSET_TICKS;
            }
            long triggerTick = (long) Math.ceil(triggerTime);
            state.scheduleShiver(
                    selectedSound.sound().get(),
                    durationTicks,
                    triggerTick,
                    (long) Math.ceil(peakTime),
                    selectedSound.clipPeakOffsetTicks(),
                    0.95D + hamster.getRandom().nextDouble() * 0.1D);
        }

        RedstoneFeverState.ShiverSchedule schedule = state.getScheduledShiver();
        if (schedule != null && worldTime >= schedule.triggerTick()) {
            hamster.playSound(
                    schedule.sound(),
                    0.03F,
                    (float) (hamster.getSoundPitch() * schedule.pitchMultiplier()));
            state.clearScheduledShiver();
            state.setShiverPeakArmed(false);
        }
    }

    private static void tickCommissionedReveal(HamsterEntity hamster, ServerWorld world) {
        if (!Configs.AHP_MAIN.enableRedstoneFever
                || !Configs.AHP_MAIN.enableSurfaceSurpriseRedstoneFever.get()
                || hamster.isTamed()
                || hamster.getRedstoneFeverState().isCommissionedRollResolved()
                || hamster.isAiDisabled()
                || hamster.age % 10 != 0
                || !isAllowedDimension(world)) {
            return;
        }

        double radius = Configs.AHP_MAIN.surfaceSurpriseRevealDistance.get();
        PlayerEntity player = world.getClosestPlayer(
                hamster.getX(),
                hamster.getY(),
                hamster.getZ(),
                radius,
                candidate -> candidate instanceof PlayerEntity playerCandidate
                        && isEligiblePlayer(playerCandidate));
        if (player == null) return;

        // Resolve before rolling so toggle or config changes cannot grant rerolls
        hamster.getRedstoneFeverState().setCommissionedRollResolved(true);
        if (hamster.getRandom().nextInt(100) < Configs.AHP_MAIN.surfaceSurpriseFeverChance.get()) {
            applyFever(hamster, true);
            hamster.setTarget(player);
            spawnRedstoneParticles(hamster, 20, 0.18F);
        }
    }

    private static void captureLeadRescuer(HamsterEntity hamster) {
        // First eligible leash holder permanently owns lead-based cure credit
        if (hamster.getRedstoneFeverState().getFirstLeadRescuerUuid() != null) return;
        Entity leashHolder = hamster.getHoldingEntity();
        if (leashHolder instanceof PlayerEntity player && isEligiblePlayer(player)) {
            hamster.getRedstoneFeverState().setFirstLeadRescuerUuid(player.getUuid());
        }
    }

    private static void tickSunlightCure(HamsterEntity hamster, ServerWorld world) {
        // Failed exposure checks pause exact progress without resetting it
        if (!Configs.AHP_MAIN.enableRedstoneFeverSunlightCuring
                || hamster.getY() <= Configs.AHP_WORLDGEN.maximumRedstoneFeverSpawnY.get()
                || !world.getDimension().hasSkyLight()
                || !world.isDay()
                || !world.isSkyVisible(hamster.getBlockPos())
                || world.hasRain(hamster.getBlockPos())) {
            return;
        }

        // Lead rescuer remains preferred; this records only fallback ownership
        if (hamster.getRedstoneFeverState().getFirstSunlightTargetUuid() == null
                && hamster.getTarget() instanceof PlayerEntity player
                && isEligiblePlayer(player)) {
            hamster.getRedstoneFeverState().setFirstSunlightTargetUuid(player.getUuid());
        }

        long progress = hamster.getRedstoneFeverState().getSunlightTicks() + 20L;
        hamster.getRedstoneFeverState().setSunlightTicks(progress);
        hamster.synchronizeRedstoneFeverVisualState();
        long required = SUNLIGHT_TICKS_PER_DAY * Configs.AHP_MAIN.redstoneFeverSunlightCureDays.get();
        if (progress >= required) {
            cure(hamster);
        }
    }

    private static final class CaveBiomeTagHolder {

        private static final TagKey<net.minecraft.world.biome.Biome> CAVE_BIOMES =
                TagKey.of(RegistryKeys.BIOME, Identifier.of(AdorableHamsterPets.MOD_ID, "is_cave"));
    }

    // --- 4. Dimension Eligibility ---
    public static boolean isAllowedDimension(ServerWorld world) {
        Identifier currentDimension = world.getRegistryKey().getValue();
        for (String entry : Configs.AHP_WORLDGEN.allowedRedstoneFeverDimensions) {
            if (entry.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(entry.substring(1));
                if (tagId == null) {
                    warnInvalidDimension(entry);
                } else if (world.getDimensionEntry().isIn(TagKey.of(RegistryKeys.DIMENSION_TYPE, tagId))) {
                    return true;
                }
                continue;
            }
            Identifier dimensionId = Identifier.tryParse(entry);
            if (dimensionId == null) {
                warnInvalidDimension(entry);
                continue;
            }
            if (currentDimension.equals(dimensionId)) return true;
        }
        return false;
    }

    private static void warnInvalidDimension(String entry) {
        // Deduplicate warnings across repeated spawn and reveal checks
        if (WARNED_INVALID_DIMENSIONS.add(entry)) {
            AdorableHamsterPets.LOGGER.warn(
                    "Ignoring invalid Redstone Fever dimension entry '{}'. Expected namespace:path or #namespace:path.",
                    entry);
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────────*/

    private RedstoneFeverUtil() {}
}
