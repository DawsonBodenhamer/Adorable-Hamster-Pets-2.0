package net.dawson.adorablehamsterpets.entity.AI;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.tag.ModItemTags;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HamsterStealDiamondGoal extends Goal {

    private final HamsterEntity hamster;
    private final World world;
    @Nullable private ItemEntity targetItem;
    @Nullable private PlayerEntity owner;
    @Nullable private Vec3d pounceStartPos;
    private int bounceSoundDelayTicks;
    private int tauntSettleTicks;
    @Nullable private Vec3d repositionTarget;
    private int repositionAttempts;

    private static final int LUNGE_DURATION_TICKS = 5;

    private enum State {
        SCANNING,
        MOVING_TO_DIAMOND,
        REPOSITIONING,
        POUNCING,
        FLEEING,
        TAUNTING
    }

    private int lungeTicks;
    private State currentState = State.SCANNING;
    private int stealDurationTimer;

    public HamsterStealDiamondGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }
    
    @Override
    public boolean canStart() {
        AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Evaluating canStart...", this.hamster.getId());
        // --- 1. Resume Logic ---
        if (this.hamster.isStealingDiamond()) {
            if (this.hamster.isSitting()) return false; // Don't resume if sitting
            if (!(this.hamster.getOwner() instanceof PlayerEntity)) return false; // Can't resume without an owner

            this.owner = (PlayerEntity) this.hamster.getOwner();
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] canStart SUCCEEDED: Resuming existing steal.", this.hamster.getId());
            return true; // Resume the goal
        }
        // --- 2. Steal Logic ---
        // --- Initial Checks ---
        if (!Configs.AHP.enableDiamondStealing) {
            return false; // Silent return
        }
        if (this.hamster.isStealingDiamond()) {
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] canStart FAILED: Hamster is already in a stealing state.", this.hamster.getId());
            return false;
        }
        if (this.hamster.isSitting()) {
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] canStart FAILED: Hamster is sitting.", this.hamster.getId());
            return false;
        }
        long currentTime = this.world.getTime();
        if (this.hamster.stealCooldownEndTick > currentTime) {
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] canStart FAILED: Steal cooldown is active for another {} ticks.", this.hamster.getId(), this.hamster.stealCooldownEndTick - currentTime);
            return false;
        }

        // --- Pounce Chance Check ---
        float randomVal = this.hamster.getRandom().nextFloat();
        float chance = Configs.AHP.diamondPounceChance.get();
        if (randomVal > chance) {
            // This is a common failure case, so we use trace to avoid spamming the log.
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] canStart FAILED: Pounce chance check failed (Rolled {} > Chance {})", this.hamster.getId(), String.format("%.2f", randomVal), String.format("%.2f", chance));
            return false;
        }

        // --- Owner Check ---
        if (!(this.hamster.getOwner() instanceof PlayerEntity playerOwner)) {
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] canStart FAILED: Hamster has no valid owner.", this.hamster.getId());
            return false;
        }
        this.owner = playerOwner;

        // --- Find Target Item ---
        List<Item> stealableItems = Configs.AHP.stealableItems.stream()
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .map(Registries.ITEM::get)
                .filter(item -> item != Items.AIR)
                .toList();

        if (stealableItems.isEmpty()) {
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] canStart FAILED: No valid stealable items configured or parsed.", this.hamster.getId());
            return false;
        }

        List<ItemEntity> nearbyItems = this.world.getEntitiesByClass(
                ItemEntity.class,
                this.hamster.getBoundingBox().expand(10.0),
                itemEntity -> ModItemTags.isStealableItem(itemEntity.getStack()) && itemEntity.isOnGround()
        );

        Optional<ItemEntity> closestItem = nearbyItems.stream()
                .filter(item -> this.hamster.getNavigation().findPathTo(item, 0) != null)
                .min((item1, item2) -> Float.compare(item1.distanceTo(this.hamster), item2.distanceTo(this.hamster)));

        if (closestItem.isPresent()) {
            this.targetItem = closestItem.get();
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] canStart SUCCEEDED. Target item: {} at {}. Owner: {}", this.hamster.getId(), this.targetItem.getStack().getItem(), this.targetItem.getBlockPos(), this.owner.getName().getString());
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Check for external interruptions that should ALWAYS stop the goal ---
        if (this.hamster.isSitting()) {
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] shouldContinue check failed: Hamster is sitting.", this.hamster.getId());
            return false;
        }
        if (this.owner == null || !this.owner.isAlive()) {
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] shouldContinue check failed: Owner is null or not alive.", this.hamster.getId());
            return false;
        }
        if (this.stealDurationTimer <= 0) {
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] shouldContinue check failed: Steal duration timer expired.", this.hamster.getId());
            return false;
        }

        // --- 2. State-aware logic ---
        // If it is fleeing or taunting, the ONLY thing that should stop it (besides the checks above) is the player
        if (this.currentState == State.FLEEING || this.currentState == State.TAUNTING) {
            if (!this.hamster.isStealingDiamond()) {
                AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] shouldContinue check failed: Player retrieved diamond (isStealingDiamond is false).", this.hamster.getId());
                return false;
            }
        }
        // If it is moving to or pouncing on the item, it MUST still exist in the world.
        else if (this.currentState == State.MOVING_TO_DIAMOND || this.currentState == State.POUNCING) {
            if (this.targetItem == null || !this.targetItem.isAlive()) {
                AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] shouldContinue check failed: Target item disappeared before pounce.", this.hamster.getId());
                return false;
            }
        }

        return true; // All checks passed for the current state, continue the goal.
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());

        if (this.hamster.isStealingDiamond()) {
            // --- RESUME LOGIC ---
            this.stealDurationTimer = this.hamster.getStealDurationTimer();
            this.targetItem = null; // No item entity to target, it's already "stolen"
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Resuming steal. Duration left: {} ticks.", this.hamster.getId(), this.stealDurationTimer);

            // Immediately decide whether to flee or taunt based on distance to owner
            if (this.hamster.distanceTo(this.owner) < Configs.AHP.minFleeDistance.get()) {
                this.currentState = State.FLEEING;
            } else {
                this.currentState = State.TAUNTING;
            }
        } else {
            // --- FRESH START LOGIC ---
            this.currentState = State.MOVING_TO_DIAMOND;
            this.hamster.getNavigation().startMovingTo(this.targetItem, 0.75D);
            this.stealDurationTimer = this.hamster.getRandom().nextBetween(
                    Configs.AHP.minStealDurationSeconds.get() * 20,
                    Configs.AHP.maxStealDurationSeconds.get() * 20
            );
            this.hamster.setStealDurationTimer(this.stealDurationTimer);
            this.repositionTarget = null;
            this.repositionAttempts = 0;
            AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Goal started fresh. State: MOVING_TO_DIAMOND. Duration: {} ticks.", this.hamster.getId(), this.stealDurationTimer);
        }
    }

    @Override
    public void stop() {
        AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Goal stopped. Final state was: {}.", this.hamster.getId(), this.currentState);

        // Apply cooldown regardless of how the goal ended.
        this.hamster.stealCooldownEndTick = this.world.getTime() + Configs.AHP.stealCooldownTicks.get();

        // Only drop the item if the goal is stopping because the timer ran out.
        // If it stops for any other reason (like player interaction), the timer will be > 0.
        if (this.hamster.isStealingDiamond() && this.stealDurationTimer <= 0) {
            ItemStack stolenStack = this.hamster.getStolenItemStack();
            if (!stolenStack.isEmpty()) {
                this.world.spawnEntity(new ItemEntity(this.world, this.hamster.getX(), this.hamster.getY(), this.hamster.getZ(), stolenStack.copy()));
                this.hamster.playSound(ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_HURT_SOUNDS, this.hamster.getRandom()), 1.0f, 1.0f);
                // Get and play the dynamic sound
                SoundEvent pounceSound = ModSounds.getDynamicItemSound(stolenStack);
                float volume = (pounceSound == SoundEvents.ENTITY_GENERIC_EAT) ? 0.35f : 1.0f;
                this.world.playSound(null, this.hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7f);
                AdorableHamsterPets.LOGGER.trace ("[StealGoal-{}] Dropped stolen item {} because timer expired.", this.hamster.getId(), stolenStack.getItem());
            }
        }
        this.hamster.setStolenItemStack(ItemStack.EMPTY); // Clear the stolen item stack
        this.hamster.setStealDurationTimer(0);
        this.hamster.setTaunting(false);
        this.hamster.setStealingDiamond(false);
        this.hamster.getNavigation().stop();
        this.targetItem = null;
        this.owner = null;
        this.currentState = State.SCANNING;
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    @Override
    public void tick() {
        // --- Timer Decrement ---
        if (this.stealDurationTimer > 0) {
            this.stealDurationTimer--;
            this.hamster.setStealDurationTimer(this.stealDurationTimer);
        }

        // --- Owner Check ---
        if (this.owner == null) {
            return; // Cannot proceed without an owner.
        }

        // --- Handle Delayed Bounce and Celebrate Sound ---
        if (this.bounceSoundDelayTicks > 0) {
            this.bounceSoundDelayTicks--;
            if (this.bounceSoundDelayTicks == 0) {
                this.hamster.playSound(ModSounds.HAMSTER_BOUNCE.get(), 0.6f, this.hamster.getSoundPitch() * 1.2f);
            }
        }

        this.stealDurationTimer--;
        this.hamster.setStealDurationTimer(this.stealDurationTimer);

        switch (this.currentState) {
            case MOVING_TO_DIAMOND:
                if (this.targetItem == null) return;
                this.hamster.getLookControl().lookAt(this.targetItem, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);
                // If navigation stops before reaching the target, try to reposition.
                if (this.hamster.getNavigation().isIdle()) {
                    this.currentState = State.REPOSITIONING;
                    AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Navigator is idle, transitioning to REPOSITIONING.", this.hamster.getId());
                    return; // End this tick, start repositioning on the next
                }
                if (this.hamster.distanceTo(this.targetItem) < 1.5) {
                    this.currentState = State.POUNCING;
                    this.lungeTicks = LUNGE_DURATION_TICKS; // Use the constant
                    this.pounceStartPos = this.hamster.getPos(); // Store starting position for the lunge
                    this.hamster.getNavigation().stop();
                    this.hamster.triggerAnimOnServer("mainController", "anim_hamster_diamond_pounce");
                    this.bounceSoundDelayTicks = 5;
                    // --- Play celebration sound ---
                    SoundEvent celebrationSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.hamster.getRandom());
                    if (celebrationSound != null) {
                        this.hamster.playSound(celebrationSound, 0.7f, this.hamster.getSoundPitch());
                    }
                    AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] State changed to POUNCING.", this.hamster.getId());
                }
                break;

            case REPOSITIONING:
                if (this.targetItem == null) return;
                // Check if exceeded repositioning budget.
                if (this.repositionAttempts >= 3) {
                    AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Exceeded max reposition attempts. Stopping goal.", this.hamster.getId());
                    this.stealDurationTimer = 0; // Force the goal to stop.
                    // Also apply the cooldown to prevent an immediate restart loop.
                    return;
                }
                // If we don't have a reposition target yet, find one.
                if (this.repositionTarget == null) {
                    this.repositionAttempts++; // Increment the attempt counter.
                    // Use findTo to get a spot in the direction of the item.
                    this.repositionTarget = FuzzyTargeting.findTo(this.hamster, 2, 3, Vec3d.ofCenter(this.targetItem.getBlockPos()));
                    if (this.repositionTarget != null) {
                        this.hamster.getNavigation().startMovingTo(this.repositionTarget.x, this.repositionTarget.y, this.repositionTarget.z, 0.75D);
                        AdorableHamsterPets.LOGGER.trace ("[StealGoal-{}] Attempt #{}: Found repositioning target at {}. Moving now.", this.hamster.getId(), this.repositionAttempts, this.repositionTarget);
                    } else {
                        // If we can't find a random spot, the area is likely too cramped. Stop the goal.
                        AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Could not find a repositioning target. Stopping goal.", this.hamster.getId());
                        this.stealDurationTimer = 0; // Force stop
                        return;
                    }
                }
                // If the navigator is idle, we've reached the reposition target or failed. Try again.
                if (this.hamster.getNavigation().isIdle()) {
                    this.repositionTarget = null; // Clear the target to find a new one next tick if needed
                    this.currentState = State.MOVING_TO_DIAMOND;
                    AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Repositioning move complete. Transitioning back to MOVING_TO_DIAMOND.", this.hamster.getId());
                }
                break;

            case POUNCING:
                if (this.targetItem == null) return;
                this.lungeTicks--;

                // --- Pounce Lunge Interpolation ---
                if (this.pounceStartPos != null) {
                    // The pounce lunge uses the constant
                    if (this.lungeTicks >= 0) {
                        // Calculate progress (from 0.0 to 1.0 over the lunge duration)
                        double progress = (double)(LUNGE_DURATION_TICKS - this.lungeTicks) / LUNGE_DURATION_TICKS;
                        // Apply a quadratic ease-in curve for acceleration
                        double easedProgress = progress * progress;

                        // Interpolate X and Z coordinates. Y is left alone to be controlled by the animation's jump.
                        double newX = pounceStartPos.x + easedProgress * (this.targetItem.getX() - pounceStartPos.x);
                        double newZ = pounceStartPos.z + easedProgress * (this.targetItem.getZ() - pounceStartPos.z);
                        this.hamster.setPosition(newX, this.hamster.getY(), newZ);
                    }
                }

                if (this.lungeTicks < 0) {
                    // Get the stack from the target ItemEntity
                    ItemStack stackToSteal = this.targetItem.getStack().copy();
                    if (stackToSteal.isEmpty()) {
                        // Safety check in case the item entity's stack somehow became empty.
                        this.stealDurationTimer = 0; // Stop the goal.
                        return;
                    }

                    this.hamster.setStolenItemStack(stackToSteal);
                    this.targetItem.discard();
                    this.hamster.setStealingDiamond(true);

                    // --- Play Sounds and Spawn Particles Simultaneously ---
                    // Get the dynamic sound for the item
                    SoundEvent pounceSound = ModSounds.getDynamicItemSound(stackToSteal);
                    float volume = (pounceSound == SoundEvents.ENTITY_GENERIC_EAT) ? 0.35f : 1.0f;
                    this.world.playSound(null, this.hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7f);

                    // Spawn particles
                    if (!this.world.isClient) {
                        ((ServerWorld)this.world).spawnParticles(ParticleTypes.END_ROD, this.hamster.getX(), this.hamster.getY() + 0.5, this.hamster.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                        // Use the actual stolenStack for the particle effect
                        ((ServerWorld)this.world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stackToSteal), this.hamster.getX(), this.hamster.getY() + 0.5, this.hamster.getZ(), 18, 0.2, 0.2, 0.2, 0.1);
                    }

                    this.currentState = State.FLEEING;
                    AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] Pounce finished. Item stolen. State changed to FLEEING.", this.hamster.getId());
                }
                break;

            case FLEEING:
                this.hamster.setTaunting(false); // Ensure taunting is off while fleeing
                if (this.hamster.distanceTo(this.owner) < Configs.AHP.minFleeDistance.get()) {
                    Vec3d fleePos = FuzzyTargeting.findFrom(this.hamster, Configs.AHP.maxFleeDistance.get(), 7, this.owner.getPos());
                    // First, check if a valid flee position was found.
                    if (fleePos != null) {
                        // Then, attempt to start moving to it.
                        this.hamster.getNavigation().startMovingTo(fleePos.x, fleePos.y, fleePos.z, 1.5D);
                        AdorableHamsterPets.LOGGER.trace ("[StealGoal-{}] Fleeing: Owner too close, found new flee point at {}.", this.hamster.getId(), fleePos);
                    }
                } else {
                    this.currentState = State.TAUNTING;
                    this.hamster.getNavigation().stop();
                    AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] State changed to TAUNTING.", this.hamster.getId());
                }
                break;

            case TAUNTING:
                // Immediately start looking at the owner as soon as we enter the taunting state.
                this.hamster.getLookControl().lookAt(this.owner, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);

                // If we just entered the taunting state, start the settle timer.
                if (!this.hamster.isTaunting() && this.tauntSettleTicks == 0) {
                    this.tauntSettleTicks = 5; // 5-tick (0.25s) taunt delay
                }

                if (this.tauntSettleTicks > 0) {
                    this.tauntSettleTicks--;
                }

                // Only set the taunting animation flag to true if the hamster has stopped moving and the settle timer is done.
                if (this.hamster.getNavigation().isIdle() && this.tauntSettleTicks == 0) {
                    this.hamster.setTaunting(true);
                }

                // Check if we need to switch back to fleeing.
                if (this.hamster.distanceTo(this.owner) < Configs.AHP.minFleeDistance.get()) {
                    this.currentState = State.FLEEING;
                    this.hamster.setTaunting(false); // Immediately turn off taunting when fleeing
                    this.tauntSettleTicks = 0; // Reset the settle timer
                    AdorableHamsterPets.LOGGER.trace("[StealGoal-{}] State changed back to FLEEING.", this.hamster.getId());
                }
                break;
        }
    }
}