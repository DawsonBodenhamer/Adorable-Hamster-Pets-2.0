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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoBlockEntity;

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

        ServerLevel bedWorld = server.getLevel(globalBedPos.dimension());
        if (bedWorld == null) return false;

        BlockPos bedPos = globalBedPos.pos();
        BlockState bedState = bedWorld.getBlockState(bedPos);

        // Verify bed exists
        if (!(bedState.getBlock() instanceof HamsterBedBlock)) {
            return false;
        }

        // Check bed-specific enablement
        BlockEntity beCheck = bedWorld.getBlockEntity(bedPos);
        if (!(beCheck instanceof HamsterBedBlockEntity bedEntity)) {
            return false;
        }

        if (!Configs.AHP_MAIN.freeBedRespawns.get() && !bedEntity.isRespawnEnabled()) {
            // Bed exists, but respawn is not paid for/enabled
            // Silent fail
            return false;
        }

        // Check occupancy to determine spawn mode
        boolean isBedFree = !bedState.getValue(HamsterBedBlock.OCCUPIED);
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
        CompoundTag data = new CompoundTag();
        hamster.addAdditionalSaveData(data);
        newHamster.readAdditionalSaveData(data);

        // Restore attributes that writeCustomDataToNbt might miss
        newHamster.setOwnerUUID(hamster.getOwnerUUID());
        newHamster.setTame(hamster.isTame(), false);
        newHamster.setCustomName(hamster.getCustomName());

        // Reset common states
        newHamster.setKnockedOut(false);
        newHamster.interactionCooldown = 0;

        // --- Spawn Logic Branch ---
        if (isBedFree) {
            // Scenario A: Bed free -> Sleep in it
            Vec3 bedCenter = Vec3.atCenterOf(bedPos).add(0, 0.1, 0);
            newHamster.moveTo(bedCenter.x, bedCenter.y, bedCenter.z, 0f, 0f);

            // Update block state & feedback
            bedWorld.setBlock(bedPos, bedState.setValue(HamsterBedBlock.OCCUPIED, true), Block.UPDATE_ALL);
            if (bedEntity instanceof GeoBlockEntity geoBlockEntity) {
                geoBlockEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_occupied");
            }
        } else {
            // Scenario B: Bed occupied -> Spawn nearby
            newHamster.moveTo(finalSpawnPos.getX() + 0.5, finalSpawnPos.getY(), finalSpawnPos.getZ() + 0.5, hamster.getYRot(), 0f);
        }

        // Set states
        newHamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
        newHamster.setSleeping(true);
        newHamster.setRescueSleeping(true);
        newHamster.setInSittingPose(true); // Lock AI
        newHamster.setHealth(Math.max(1.0f, newHamster.getMaxHealth() * 0.05f)); // 5% health

        // Select sleep pose based on personality ID
        int personality = newHamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
        newHamster.getEntityData().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, HamsterPoseUtil.getDeepSleepAnimId(personality));

        // --- Linkage Update & Charge Consumption ---
        // Created a new entity, so it has a new UUID. Update the bed block entity
        if (bedWorld.getBlockEntity(bedPos) instanceof HamsterBedBlockEntity finalBedEntity) {
            Component name = newHamster.hasCustomName() ? newHamster.getCustomName() : newHamster.getDisplayName();
            finalBedEntity.setLinkedHamster(newHamster.getUUID(), name, finalBedEntity.getWanderDistance());

            // Don't consume charge if respawns are free or infinite after tribute
            if (!Configs.AHP_MAIN.freeBedRespawns.get() && !Configs.AHP_MAIN.infiniteRespawnsAfterTribute.get()) {
                finalBedEntity.setRespawnEnabled(false);
            }
        }

        // Spawn
        bedWorld.addFreshEntity(newHamster);

        // Effects
        bedWorld.playSound(null, bedPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.0f, 1.0f);
        ParticleEffectsUtil.spawnParticles(
                bedWorld,
                Vec3.atCenterOf(bedPos),
                ParticleTypes.REVERSE_PORTAL,
                20,
                new Vec3(0.3, 0.3, 0.3),
                0.1
        );
        if (hamster.getOwner() instanceof Player owner) {
            owner.displayClientMessage(Component.translatable("message.adorablehamsterpets.respawn.success").withStyle(ChatFormatting.WHITE), true);
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
            hamster.setGoToBedCooldown(Configs.AHP_MAIN.bedWakeUpCooldown.get());
            hamster.setBypassNextSleepDelay(true);
        }

        // Set bed block to unoccupied and find a safe spot to move to
        hamster.getLinkedBedPos().ifPresent(globalPos -> {
            if (hamster.level().dimension() == globalPos.dimension()) {
                BlockPos bedPos = globalPos.pos();
                BlockState bedState = hamster.level().getBlockState(bedPos);

                // If bed still exists
                if (bedState.is(ModBlocks.HAMSTER_BED.get())) {
                    // Spawn wake-up particles with wood type
                    ParticleEffectsUtil.spawnParticles(
                            hamster.level(),
                            Vec3.atBottomCenterOf(bedPos).add(0, 0.3, 0),
                            ModParticles.getForVariant(bedState.getValue(HamsterBedBlock.WOOD_VARIANT)),
                            50,
                            new Vec3(0.2, 0.5, 0.2),
                            0.0
                    );

                    if (bedState.getValue(HamsterBedBlock.OCCUPIED)) {
                        hamster.level().setBlock(bedPos, bedState.setValue(HamsterBedBlock.OCCUPIED, false), Block.UPDATE_ALL);
                    }

                    // Trigger bed animation
                    BlockEntity be = hamster.level().getBlockEntity(bedPos);
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
                    hamster.level().playSound(null, hamster.blockPosition(), rustleSound, SoundSource.NEUTRAL, 0.2f, 1.8f);
                }

                // Find safe egress position and pathfind
                for (BlockPos checkPos : BlockPos.betweenClosed(bedPos.offset(-1, 0, -1), bedPos.offset(1, 0, 1))) {
                    // Don't move to the bed block itself
                    if (checkPos.equals(bedPos)) continue;

                    // Determine pos with safe spawning algorithm
                    if (HamsterPlacementUtil.isSafeSpawnLocation(checkPos, hamster.level(), hamster)) {
                        hamster.getNavigation().moveTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5, 1.2D);
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
        if (hamster.level().isClientSide()) return;

        // 1. Spawn the first burst of particles immediately
        hamster.getLinkedBedPos().ifPresent(globalPos -> {
            if (hamster.level().dimension() == globalPos.dimension()) {
                BlockPos bedPos = globalPos.pos();
                BlockState bedState = hamster.level().getBlockState(bedPos);

                // If bed exists
                if (bedState.is(ModBlocks.HAMSTER_BED.get())) {
                    ParticleEffectsUtil.spawnParticles(
                            hamster.level(),
                            Vec3.atBottomCenterOf(bedPos).add(0, 0.3, 0),
                            ModParticles.getForVariant(bedState.getValue(HamsterBedBlock.WOOD_VARIANT)),
                            70,
                            new Vec3(0.2, 0.5, 0.2),
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
            hamster.level().playSound(null, hamster.blockPosition(), rustleSound, SoundSource.NEUTRAL, 0.5f, 1.0f);
        }
        hamster.level().playSound(null, hamster.blockPosition(), ModSounds.HAMSTER_THUMP.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);

        // 4. Trigger advancement
        if (hamster.getOwner() instanceof ServerPlayer serverPlayerOwner) {
            ModCriteria.HAMSTER_SLEPT_IN_BED.get().trigger(serverPlayerOwner);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                            Pathing & Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Checks if the hamster is actively sleeping near its linked bed.
     */
    public static boolean isSleepingInBed(HamsterEntity hamster) {
        if (!hamster.isSleeping()) return false;
        Optional<GlobalPos> bedPosOpt = hamster.getLinkedBedPos();
        if (bedPosOpt.isEmpty()) return false;

        GlobalPos globalPos = bedPosOpt.get();
        if (hamster.level().dimension() != globalPos.dimension()) return false;

        return hamster.blockPosition().closerThan(globalPos.pos(), 2.0);
    }

    /**
     * Ensures the block state is correctly marked as occupied if a hamster is actively sleeping in it,
     * and correctly un-marked if the hamster has left.
     */
    public static void autoHealBedState(HamsterEntity hamster) {
        Optional<GlobalPos> bedPosOpt = hamster.getLinkedBedPos();
        if (bedPosOpt.isEmpty()) return;

        GlobalPos bedGlobalPos = bedPosOpt.get();
        Level world = hamster.level();

        if (world.dimension() == bedGlobalPos.dimension()) {
            BlockPos bedPos = bedGlobalPos.pos();
            // Ensure chunk is loaded to prevent loading newly generated chunks via ticks
            if (world.hasChunk(bedPos.getX() >> 4, bedPos.getZ() >> 4)) {
                BlockState bedState = world.getBlockState(bedPos);

                if (bedState.getBlock() instanceof HamsterBedBlock) {
                    boolean isOccupied = bedState.getValue(HamsterBedBlock.OCCUPIED);
                    boolean shouldBeOccupied = hamster.isSleeping() && !hamster.isKnockedOut() && hamster.blockPosition().closerThan(bedPos, 2.0);

                    if (shouldBeOccupied && !isOccupied) {
                        // Fix falsely unoccupied bed
                        world.setBlock(bedPos, bedState.setValue(HamsterBedBlock.OCCUPIED, true), Block.UPDATE_ALL);
                        BlockEntity be = world.getBlockEntity(bedPos);
                        if (be instanceof HamsterBedBlockEntity bedEntity) {
                            bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_occupied");
                        }
                        AdorableHamsterPets.LOGGER.debug("[HamsterBedUtil] Auto-healed unoccupied bed block state at {}", bedPos);
                    } else if (!shouldBeOccupied && isOccupied) {
                        // Fix falsely occupied bed
                        world.setBlock(bedPos, bedState.setValue(HamsterBedBlock.OCCUPIED, false), Block.UPDATE_ALL);
                        BlockEntity be = world.getBlockEntity(bedPos);
                        if (be instanceof HamsterBedBlockEntity bedEntity) {
                            bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_unoccupied");
                        }
                    }
                }
            }
        }
    }

    /**
     * Teleports a hamster safely into its bed and forces it into a deep sleep state.
     * Used by various rescue protocols.
     */
    public static void forceTeleportAndSleepInBed(HamsterEntity hamster, ServerLevel bedWorld, BlockPos bedPos, BlockState bedState) {
        // Set position slightly elevated inside the bed to prevent clipping
        Vec3 targetCenter = Vec3.atCenterOf(bedPos).add(0, 0.1, 0);

        // Request teleport to sync with client
        hamster.teleportTo(targetCenter.x, targetCenter.y, targetCenter.z);

        // Force delayed positional update to prevent Server/Client desync
        long currentWorldTime = bedWorld.getGameTime();
        hamster.scheduleTask(currentWorldTime + 5, "sledgehammer_teleport_sync", () -> {
            if (hamster.isAlive() && !hamster.isRemoved()) {
                hamster.teleportTo(hamster.getX(), hamster.getY(), hamster.getZ());
            }
        });

        hamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
        hamster.setSleeping(true);
        hamster.setRescueSleeping(true);
        hamster.setInSittingPose(true);

        bedWorld.setBlock(bedPos, bedState.setValue(HamsterBedBlock.OCCUPIED, true), Block.UPDATE_ALL);

        // Match personality pose
        int personality = hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
        hamster.getEntityData().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, HamsterPoseUtil.getDeepSleepAnimId(personality));

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

        Level world = hamster.level();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // Only scan the next 15 nodes
        int startIndex = path.getNextNodeIndex();
        int endIndex = Math.min(path.getNodeCount(), startIndex + 15);

        for (int i = startIndex; i < endIndex; ++i) {
            Node node = path.getNode(i);
            mutablePos.set(node.x, node.y, node.z);

            // Skip unloaded chunks
            if (!world.hasChunk(mutablePos.getX() >> 4, mutablePos.getZ() >> 4)) {
                continue;
            }

            if (isUnlinkedBed(world, mutablePos, linkedBed)) {
                return true;
            }

            mutablePos.move(Direction.DOWN);
            if (isUnlinkedBed(world, mutablePos, linkedBed)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnlinkedBed(Level world, BlockPos pos, BlockPos linkedBed) {
        if (world.getBlockState(pos).is(ModBlocks.HAMSTER_BED.get())) {
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
            if (!hamster.level().isClientSide()) {
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
                    BlockState bedState = hamster.level().getBlockState(bedPos);

                    // If bed exists
                    if (bedState.is(ModBlocks.HAMSTER_BED.get())) {
                        ParticleEffectsUtil.spawnParticles(
                                hamster.level(),
                                Vec3.atBottomCenterOf(bedPos).add(0, 0.3, 0),
                                ModParticles.getForVariant(bedState.getValue(HamsterBedBlock.WOOD_VARIANT)),
                                particleCount,
                                new Vec3(0.2, 0.3, 0.2),
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
        if (Configs.AHP_MAIN.circadianChaos.get()) {
            int min = Configs.AHP_MAIN.minNapInBedIntervalSeconds.get() * 20;
            int max = Configs.AHP_MAIN.maxNapInBedIntervalSeconds.get() * 20;
            hamster.setNapInBedDurationTimer(hamster.getRandom().nextIntBetweenInclusive(min, max));
        }
    }
}