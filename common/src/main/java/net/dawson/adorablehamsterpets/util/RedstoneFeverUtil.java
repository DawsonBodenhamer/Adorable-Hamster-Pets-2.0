package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.RedstoneFeverState;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
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
    public static final ResourceLocation FEVER_MOVEMENT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "redstone_fever_movement_speed");
    private static final Set<String> WARNED_INVALID_DIMENSIONS = ConcurrentHashMap.newKeySet();

    private static final TagKey<net.minecraft.world.level.biome.Biome> CAVE_BIOMES =
            TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "is_cave"));

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ───────────────────────────────────────────────────────────────────────────────*/

    // --- 1. Fever Eligibility and Transitions ---
    public static void tryApplyNaturalFever(
            HamsterEntity hamster, ServerLevel world, MobSpawnType spawnReason) {
        if (!Configs.AHP_MAIN.enableRedstoneFever) return;
        if (!RedstoneFeverPolicy.isEligibleFreshSpawnReason(spawnReason)) return;
        if (!Configs.AHP_WORLDGEN.enableNaturalRedstoneFeverSpawning) return;
        if (hamster.getY() > Configs.AHP_WORLDGEN.maximumRedstoneFeverSpawnY.get()) return;
        if (world.canSeeSky(hamster.blockPosition())) return;
        if (!isAllowedDimension(world)) return;
        if (Configs.AHP_WORLDGEN.requireRedstoneFeverCaveBiomeTags
                && !world.getBiome(hamster.blockPosition()).is(CAVE_BIOMES)) {
            return;
        }
        if (hamster.getRandom().nextInt(100) >= Configs.AHP_WORLDGEN.redstoneFeverChance.get()) return;
        applyFever(hamster, false);
    }

    public static boolean applyFever(HamsterEntity hamster, boolean resolveCommissionedRoll) {
        if (!Configs.AHP_MAIN.enableRedstoneFever) return false;
        if (hamster.isTame() || hamster.getRedstoneFeverState().isFevered()) return false;

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
            if (hiss != null) hamster.playSound(hiss, 0.7F, hamster.getVoicePitch());
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
        if (creditedPlayer != null && hamster.level() instanceof ServerLevel world) {
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
        if (hamster.level() instanceof ServerLevel world) {
            world.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    hamster.getX(), hamster.getY(0.55D), hamster.getZ(),
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
        if (!(hamster.level() instanceof ServerLevel world)) return;
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

        if (hamster.isTame()) {
            // Defensive invariant for transfers or external taming integrations
            cure(hamster);
            return;
        }

        tickAudio(hamster);
        captureLeadRescuer(hamster);

        // Sunlight and ambient particles need only one server check per second
        if (hamster.tickCount % 20 != 0) return;
        tickSunlightCure(hamster, world);

        double severity = getSeverity(hamster);
        if (hamster.getRandom().nextDouble() < 0.45D * severity) {
            spawnRedstoneParticles(hamster, 2, 0.0F);
        }
    }

    // --- 3. Shared Severity and Presentation ---
    public static double getSeverity(HamsterEntity hamster) {
        long required = SUNLIGHT_TICKS_PER_DAY * Configs.AHP_MAIN.redstoneFeverSunlightCureDays.get();
        return 1.0D - Math.clamp((double) hamster.getRedstoneFeverState().getSunlightTicks() / required, 0.0D, 1.0D);
    }

    public static void reconcileMovementSpeed(HamsterEntity hamster) {
        if (hamster.level().isClientSide()) return;

        AttributeInstance speed = hamster.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        boolean shouldHaveModifier = hamster.getRedstoneFeverState().isFevered();
        boolean hasModifier = speed.hasModifier(FEVER_MOVEMENT_SPEED_MODIFIER_ID);
        if (shouldHaveModifier && !hasModifier) {
            speed.addTransientModifier(new AttributeModifier(
                    FEVER_MOVEMENT_SPEED_MODIFIER_ID,
                    0.5D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!shouldHaveModifier && hasModifier) {
            speed.removeModifier(FEVER_MOVEMENT_SPEED_MODIFIER_ID);
        }
    }

    public static void clearMovementSpeedModifier(HamsterEntity hamster) {
        if (hamster.level().isClientSide()) return;

        AttributeInstance speed = hamster.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(FEVER_MOVEMENT_SPEED_MODIFIER_ID)) {
            speed.removeModifier(FEVER_MOVEMENT_SPEED_MODIFIER_ID);
        }
    }

    public static boolean isEligiblePlayer(Player player) {
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
        if (target instanceof Player player) {
            return isEligiblePlayer(player);
        }
        return Configs.AHP_MAIN.redstoneFeverAttackMostLivingMobs
                && !target.isInvulnerable();
    }

    public static boolean isWithinTargetingRange(
            HamsterEntity hamster, @Nullable LivingEntity target) {
        return target != null
                && hamster.distanceToSqr(target)
                        <= Math.pow(Configs.AHP_MAIN.redstoneFeverTargetingRange.get(), 2.0D);
    }

    public static void spawnRedstoneParticles(HamsterEntity hamster, int count, float velocity) {
        ParticleEffectsUtil.spawnParticlesOnEntity(
                hamster,
                new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.02F), 1.0F),
                count,
                0.5D / hamster.getBbWidth(),
                0.4D / hamster.getBbHeight(),
                velocity,
                hamster.getBbHeight() * 0.05D);
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
        long worldTime = hamster.level().getGameTime();
        RedstoneFeverState state = hamster.getRedstoneFeverState();
        double tremorSpike = getTremorSpike(worldTime, hamster.getUUID());

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
            double peakTime = getNextTremorSpikePeakTime(worldTime, hamster.getUUID());
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
                    (float) (hamster.getVoicePitch() * schedule.pitchMultiplier()));
            state.clearScheduledShiver();
            state.setShiverPeakArmed(false);
        }
    }

    private static void tickCommissionedReveal(HamsterEntity hamster, ServerLevel world) {
        if (!Configs.AHP_MAIN.enableRedstoneFever
                || !Configs.AHP_MAIN.enableSurfaceSurpriseRedstoneFever.get()
                || hamster.isTame()
                || hamster.getRedstoneFeverState().isCommissionedRollResolved()
                || hamster.isNoAi()
                || hamster.tickCount % 10 != 0
                || !isAllowedDimension(world)) {
            return;
        }

        double radius = Configs.AHP_MAIN.surfaceSurpriseRevealDistance.get();
        Player player = world.getNearestPlayer(
                hamster.getX(),
                hamster.getY(),
                hamster.getZ(),
                radius,
                candidate -> candidate instanceof Player playerCandidate
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
        Entity leashHolder = hamster.getLeashHolder();
        if (leashHolder instanceof Player player && isEligiblePlayer(player)) {
            hamster.getRedstoneFeverState().setFirstLeadRescuerUuid(player.getUUID());
        }
    }

    private static void tickSunlightCure(HamsterEntity hamster, ServerLevel world) {
        // Failed exposure checks pause exact progress without resetting it
        if (!Configs.AHP_MAIN.enableRedstoneFeverSunlightCuring
                || hamster.getY() <= Configs.AHP_WORLDGEN.maximumRedstoneFeverSpawnY.get()
                || !world.dimensionType().hasSkyLight()
                || !world.isDay()
                || !world.canSeeSky(hamster.blockPosition())
                || world.isRainingAt(hamster.blockPosition())) {
            return;
        }

        // Lead rescuer remains preferred; this records only fallback ownership
        if (hamster.getRedstoneFeverState().getFirstSunlightTargetUuid() == null
                && hamster.getTarget() instanceof Player player
                && isEligiblePlayer(player)) {
            hamster.getRedstoneFeverState().setFirstSunlightTargetUuid(player.getUUID());
        }

        long progress = hamster.getRedstoneFeverState().getSunlightTicks() + 20L;
        hamster.getRedstoneFeverState().setSunlightTicks(progress);
        hamster.synchronizeRedstoneFeverVisualState();
        long required = SUNLIGHT_TICKS_PER_DAY * Configs.AHP_MAIN.redstoneFeverSunlightCureDays.get();
        if (progress >= required) {
            cure(hamster);
        }
    }

    // --- 4. Dimension Eligibility ---
    public static boolean isAllowedDimension(ServerLevel world) {
        ResourceLocation currentDimension = world.dimension().location();
        for (String entry : Configs.AHP_WORLDGEN.allowedRedstoneFeverDimensions) {
            if (entry.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId == null) {
                    warnInvalidDimension(entry);
                } else if (world.dimensionTypeRegistration().is(TagKey.create(Registries.DIMENSION_TYPE, tagId))) {
                    return true;
                }
                continue;
            }
            ResourceLocation dimensionId = ResourceLocation.tryParse(entry);
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
