package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;

import java.util.Optional;

/**
 * Encapsulates all interactions between Hamster entities and Hamster Beds,
 * including the complex respawn mechanics and sleep state transitions.
 */
public final class HamsterBedUtil {

    private HamsterBedUtil() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Bed Interactions
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- 1. Respawn Logic ---
    /**
     * Attempts to respawn the hamster at its linked bed.
     * @return True if respawn was successful, false otherwise.
     */
    public static boolean tryRespawnInBed(HamsterEntity hamster) {
        if (hamster.getLinkedBedPos().isEmpty()) return false;

        GlobalPos globalBedPos = hamster.getLinkedBedPos().get();
        MinecraftServer server = hamster.getServer();
        if (server == null) return false;

        ServerWorld bedWorld = server.getWorld(globalBedPos.dimension());
        if (bedWorld == null) return false;

        BlockPos bedPos = globalBedPos.pos();
        BlockState bedState = bedWorld.getBlockState(bedPos);

        // Verify bed exists
        if (!(bedState.getBlock() instanceof HamsterBedBlock)) {
            return false;
        }

        // Check bed-specific enablement
        BlockEntity beCheck = bedWorld.getBlockEntity(bedPos);
        if (!(beCheck instanceof HamsterBedBlockEntity bedEntity) || !bedEntity.isRespawnEnabled()) {
            // Bed exists, but respawn is not paid for/enabled
            // Silent fail
            return false;
        }

        // Check occupancy to determine spawn mode
        boolean isBedFree = !bedState.get(HamsterBedBlock.OCCUPIED);
        BlockPos finalSpawnPos = null;

        if (!isBedFree) {
            // Bed is occupied, determine pos with safe spawning algorithm
            Optional<BlockPos> safePosOpt = HamsterPlacementUtil.findSafeSpawnPosition(bedPos, bedWorld, 2, hamster);
            if (safePosOpt.isEmpty()) {
                // Silent fail
                return false;
            }
            finalSpawnPos = safePosOpt.get();
        }

        // --- Create Clone ---
        HamsterEntity newHamster = ModEntities.HAMSTER.get().create(bedWorld);
        if (newHamster == null) return false;

        // Copy NBT data
        NbtCompound data = new NbtCompound();
        hamster.writeCustomDataToNbt(data);
        newHamster.readCustomDataFromNbt(data);

        // Restore attributes that writeCustomDataToNbt might miss
        newHamster.setOwnerUuid(hamster.getOwnerUuid());
        newHamster.setTamed(hamster.isTamed(), false);
        newHamster.setCustomName(hamster.getCustomName());

        // Reset common states
        newHamster.setKnockedOut(false);
        newHamster.interactionCooldown = 0;

        // --- Spawn Logic Branch ---
        if (isBedFree) {
            // Scenario A: Bed is free -> Sleep in it
            Vec3d bedCenter = Vec3d.ofCenter(bedPos).add(0, 0.1, 0);
            newHamster.refreshPositionAndAngles(bedCenter.x, bedCenter.y, bedCenter.z, 0f, 0f);

            // Set to 5% health
            newHamster.setHealth(Math.max(1.0f, newHamster.getMaxHealth() * 0.05f));

            // Force sleep state
            newHamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
            newHamster.setSleeping(true);
            newHamster.setInSittingPose(true); // Lock AI

            // Select sleep pose based on personality ID to match original hamster
            int personality = newHamster.getDataTracker().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
            int poseIndex = (personality >= 1 && personality <= 3) ? personality : 1;
            String sleepAnim = "anim_hamster_sleep_pose" + poseIndex;
            newHamster.getDataTracker().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, sleepAnim);

            // Update block state
            bedWorld.setBlockState(bedPos, bedState.with(HamsterBedBlock.OCCUPIED, true), Block.NOTIFY_ALL);

            // Trigger bed animation
            if (bedEntity instanceof GeoBlockEntity geoBlockEntity) {
                geoBlockEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_occupied");
            }
        } else {
            // Scenario B: Bed occupied -> Spawn nearby standing up
            newHamster.refreshPositionAndAngles(finalSpawnPos.getX() + 0.5, finalSpawnPos.getY(), finalSpawnPos.getZ() + 0.5, hamster.getYaw(), 0f);
            newHamster.setHealth(newHamster.getMaxHealth());
            newHamster.setSitting(false);
        }

        // --- Linkage Update & Charge Consumption ---
        // Created a new entity, so it has a new UUID. Update the bed block entity
        if (bedWorld.getBlockEntity(bedPos) instanceof HamsterBedBlockEntity finalBedEntity) {
            Text name = newHamster.hasCustomName() ? newHamster.getCustomName() : newHamster.getDisplayName();
            finalBedEntity.setLinkedHamster(newHamster.getUuid(), name, finalBedEntity.getWanderDistance());

            // Consume charge
            finalBedEntity.setRespawnEnabled(false);
        }

        // Spawn
        bedWorld.spawnEntity(newHamster);

        // Effects
        bedWorld.playSound(null, bedPos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
        ParticleEffectsUtil.spawnParticles(
                bedWorld,
                Vec3d.ofCenter(bedPos),
                ParticleTypes.REVERSE_PORTAL,
                20,
                new Vec3d(0.3, 0.3, 0.3),
                0.1
        );
        if (hamster.getOwner() instanceof PlayerEntity owner) {
            owner.sendMessage(Text.translatable("message.adorablehamsterpets.respawn.success").formatted(Formatting.GOLD), true);
        }

        return true;
    }

    // --- 2. Wake Up Logic ---
    /**
     * Wakes the hamster up from its bed, setting the bed block to unoccupied
     * and applying a cooldown to prevent it from immediately going back to sleep.
     */
    public static void wakeUpFromBed(HamsterEntity hamster, boolean isManualWakeUp) {
        if (!hamster.isSleeping()) return;

        // Trigger animation and sound
        hamster.triggerWakeUpFromSleepAnimation(isManualWakeUp);

        hamster.setSleeping(false);
        hamster.setRescueSleeping(false); // Clear the rescue flag so normal logic resumes
        hamster.setInSittingPose(false);  // Explicitly re-enable AI movement

        // Apply a configurable cooldown if woken up by player interaction
        if (isManualWakeUp) {
            hamster.setGoToBedCooldown(Configs.AHP.bedWakeUpCooldown.get());
            hamster.setBypassNextSleepDelay(true);
        }

        // Set bed block to unoccupied and find a safe spot to move to
        hamster.getLinkedBedPos().ifPresent(globalPos -> {
            if (hamster.getWorld().getRegistryKey() == globalPos.dimension()) {
                BlockPos bedPos = globalPos.pos();
                BlockState bedState = hamster.getWorld().getBlockState(bedPos);

                // If bed still exists
                if (bedState.isOf(ModBlocks.HAMSTER_BED.get())) {
                    // Spawn wake-up particles with wood type
                    ParticleEffectsUtil.spawnParticles(
                            hamster.getWorld(),
                            Vec3d.ofBottomCenter(bedPos).add(0, 0.3, 0),
                            ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                            50,
                            new Vec3d(0.2, 0.5, 0.2),
                            0.0
                    );

                    if (bedState.get(HamsterBedBlock.OCCUPIED)) {
                        hamster.getWorld().setBlockState(bedPos, bedState.with(HamsterBedBlock.OCCUPIED, false), Block.NOTIFY_ALL);
                    }

                    // Trigger bed animation
                    BlockEntity be = hamster.getWorld().getBlockEntity(bedPos);
                    if (be instanceof GeoBlockEntity geoBlockEntity) {
                        geoBlockEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_unoccupied");
                    }
                } else {
                    // Bed is missing. Unlink
                    hamster.setWanderModeActive(false);
                    hamster.setLinkedBedPos(Optional.empty());
                }

                // Play leaf rustling sound
                SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, hamster.getRandom());
                if (rustleSound != null) {
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), rustleSound, SoundCategory.NEUTRAL, 0.2f, 1.8f);
                }

                // Find safe egress position and pathfind
                for (BlockPos checkPos : BlockPos.iterate(bedPos.add(-1, 0, -1), bedPos.add(1, 0, 1))) {
                    // Don't move to the bed block itself
                    if (checkPos.equals(bedPos)) continue;

                    // Determine pos with safe spawning algorithm
                    if (HamsterPlacementUtil.isSafeSpawnLocation(checkPos, hamster.getWorld(), hamster)) {
                        hamster.getNavigation().startMovingTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5, 1.2D);
                        return; // Exit after finding the first safe spot
                    }
                }
            }
        });
    }

    // --- 3. Sleep Initialization Effects ---
    /**
     * Initiates the sound and particle effects for when a hamster settles into its bed.
     * This is called by the AI goal when the hamster's state officially changes to sleeping in the bed.
     */
    public static void startBedSleepEffects(HamsterEntity hamster) {
        if (hamster.getWorld().isClient()) return;

        // 1. Spawn the first burst of particles immediately
        hamster.getLinkedBedPos().ifPresent(globalPos -> {
            if (hamster.getWorld().getRegistryKey() == globalPos.dimension()) {
                BlockPos bedPos = globalPos.pos();
                BlockState bedState = hamster.getWorld().getBlockState(bedPos);

                // If bed exists
                if (bedState.isOf(ModBlocks.HAMSTER_BED.get())) {
                    ParticleEffectsUtil.spawnParticles(
                            hamster.getWorld(),
                            Vec3d.ofBottomCenter(bedPos).add(0, 0.3, 0),
                            ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                            70,
                            new Vec3d(0.2, 0.5, 0.2),
                            1.0
                    );
                }
            }
        });

        // 2. Set the timer for the remaining bursts
        hamster.bedLeafParticleTicks = 4;

        // 3. Play sounds
        SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, hamster.getRandom());
        if (rustleSound != null) {
            hamster.getWorld().playSound(null, hamster.getBlockPos(), rustleSound, SoundCategory.NEUTRAL, 0.5f, 1.0f);
        }
        hamster.getWorld().playSound(null, hamster.getBlockPos(), ModSounds.HAMSTER_THUMP.get(), SoundCategory.NEUTRAL, 1.0f, 1.0f);

        // 4. Trigger advancement
        if (hamster.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
            ModCriteria.HAMSTER_SLEPT_IN_BED.get().trigger(serverPlayerOwner);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                            Pathing & Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Ensures the block state is correctly marked as occupied if a hamster is actively sleeping in it.
     */
    public static void autoHealBedState(HamsterEntity hamster) {
        if (hamster.isSleeping() && hamster.getLinkedBedPos().isPresent() && !hamster.isThrown() && !hamster.isKnockedOut()) {
            World world = hamster.getWorld();
            GlobalPos bedGlobalPos = hamster.getLinkedBedPos().get();

            if (world.getRegistryKey() == bedGlobalPos.dimension()) {
                BlockPos bedPos = bedGlobalPos.pos();

                // Ensure chunk is loaded to prevent loading newly generated chunks via ticks
                if (world.isChunkLoaded(bedPos.getX() >> 4, bedPos.getZ() >> 4)) {
                    BlockState bedState = world.getBlockState(bedPos);

                    if (bedState.getBlock() instanceof HamsterBedBlock && !bedState.get(HamsterBedBlock.OCCUPIED)) {
                        world.setBlockState(bedPos, bedState.with(HamsterBedBlock.OCCUPIED, true), Block.NOTIFY_ALL);

                        BlockEntity be = world.getBlockEntity(bedPos);
                        if (be instanceof HamsterBedBlockEntity bedEntity) {
                            bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_occupied");
                        }
                        AdorableHamsterPets.LOGGER.debug("[HamsterBedUtil] Auto-healed unoccupied bed block state at {}", bedPos);
                    }
                }
            }
        }
    }

    /**
     * Teleports a hamster safely into its bed and forces it into a deep sleep state.
     * Used by various rescue protocols.
     */
    public static void forceTeleportAndSleepInBed(HamsterEntity hamster, ServerWorld bedWorld, BlockPos bedPos, BlockState bedState) {
        // Set position slightly elevated inside the bed to prevent clipping
        Vec3d targetCenter = Vec3d.ofCenter(bedPos).add(0, 0.1, 0);
        hamster.refreshPositionAndAngles(targetCenter.x, targetCenter.y, targetCenter.z, 0f, 0f);

        hamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
        hamster.setSleeping(true);
        hamster.setRescueSleeping(true);
        hamster.setInSittingPose(true);

        bedWorld.setBlockState(bedPos, bedState.with(HamsterBedBlock.OCCUPIED, true), Block.NOTIFY_ALL);

        // Match personality pose
        int personality = hamster.getDataTracker().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
        int poseIndex = (personality >= 1 && personality <= 3) ? personality : 1;
        hamster.getDataTracker().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, "anim_hamster_sleep_pose" + poseIndex);

        startNapTimer(hamster);
    }

    /**
     * Checks if a given path traverses another hamster's linked bed.
     * Allows AI goals to validate a path around those beds before committing to it.
     */
    public static boolean isPathThroughUnlinkedBed(HamsterEntity hamster, @Nullable Path path) {
        if (path == null) return false;

        // Get position of this hamster's linked bed if it has one
        BlockPos linkedBed = hamster.getLinkedBedPos()
                .map(GlobalPos::pos)
                .orElse(null);

        for (int i = 0; i < path.getLength(); ++i) {
            PathNode node = path.getNode(i);
            // Use direct method to get BlockPos from the node
            BlockPos pos = node.getBlockPos();
            if (isUnlinkedBed(hamster, pos, linkedBed) || isUnlinkedBed(hamster, pos.down(), linkedBed)) {
                AdorableHamsterPets.LOGGER.trace(
                        "[AHP Nav Debug] Path intersects unlinked bed at {}, linkedBed={} for hamster {}",
                        pos, linkedBed, hamster.getUuid()
                );
                return true;
            }
        }
        return false;
    }

    private static boolean isUnlinkedBed(HamsterEntity hamster, BlockPos pos, BlockPos linkedBed) {
        if (hamster.getWorld().getBlockState(pos).getBlock() instanceof HamsterBedBlock) {
            // If the node is a bed, check if it's NOT this hamster's linked bed
            // True if no linked bed, or if position doesn't match
            return linkedBed == null || !pos.equals(linkedBed);
        }
        return false; // Valid path
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                            Ticking & Timers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Processes a sequenced burst of leaf particles when a hamster settles into bed.
     * Called from the entity's tick() method.
     */
    public static void tickBedLeafParticles(HamsterEntity hamster) {
        if (hamster.bedLeafParticleTicks > 0) {
            if (!hamster.getWorld().isClient()) {
                int particleCount = 0;
                // Check for specific moments in the 4-tick duration
                if (hamster.bedLeafParticleTicks == 3) { // Second burst
                    particleCount = 15;
                } else if (hamster.bedLeafParticleTicks == 2) { // Third burst
                    particleCount = 10;
                } else if (hamster.bedLeafParticleTicks == 1) { // Fourth burst
                    particleCount = 5;
                }

                if (particleCount > 0 && hamster.getLinkedBedPos().isPresent()) {
                    BlockPos bedPos = hamster.getLinkedBedPos().get().pos();
                    BlockState bedState = hamster.getWorld().getBlockState(bedPos);

                    // If bed exists
                    if (bedState.isOf(ModBlocks.HAMSTER_BED.get())) {
                        ParticleEffectsUtil.spawnParticles(
                                hamster.getWorld(),
                                Vec3d.ofBottomCenter(bedPos).add(0, 0.3, 0),
                                ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                                particleCount,
                                new Vec3d(0.2, 0.3, 0.2),
                                1.0
                        );
                    }
                }
            }
            // Decrement the timer after processing the current tick's effect
            hamster.bedLeafParticleTicks--;
        }
    }

    /**
     * Starts the nap timer for the Circadian Chaos feature.
     * Called by the AI goal when the hamster successfully enters its bed.
     */
    public static void startNapTimer(HamsterEntity hamster) {
        if (Configs.AHP.circadianChaos.get()) {
            int min = Configs.AHP.minNapInBedIntervalSeconds.get() * 20;
            int max = Configs.AHP.maxNapInBedIntervalSeconds.get() * 20;
            hamster.setNapInBedDurationTimer(hamster.getRandom().nextBetween(min, max));
        }
    }
}