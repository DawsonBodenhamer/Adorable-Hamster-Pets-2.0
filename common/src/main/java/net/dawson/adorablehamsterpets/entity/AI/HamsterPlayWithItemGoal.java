package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * A unified AI goal that manages the hamster's interactions with dropped items, covering both
 * "Greedy/Theft" behaviors (stealing diamonds) and "Loyal/Retrieval" behaviors (fetching acorns).
 * <p>
 * This goal operates on a state machine that transitions through:
 * <ol>
 *     <li><b>SCANNING:</b> locating valid items defined in the config.</li>
 *     <li><b>MOVING/POUNCING:</b> pathfinding to and visually grabbing the item.</li>
 *     <li><b>DECISION:</b> determining if the action is hostile (theft) or friendly (delivery).</li>
 *     <li><b>REACTION:</b> either fleeing from the owner (Theft) or running to them (Delivery).</li>
 *     <li><b>PLAYING:</b> a terminal state where the hamster animates (Taunting vs. Presenting).</li>
 * </ol>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *     <li><b>Dual Modes:</b> Configurable lists determine if an item triggers Theft or Delivery.</li>
 *     <li><b>Diamond Armor Override:</b> Equipping Diamond Armor suppresses the theft instinct, converting
 *     stealable items into retrievable ones (curing kleptomania).</li>
 *     <li><b>Resume Logic:</b> Capable of resuming the behavior immediately upon spawning if the
 *     hamster is already holding an item (essential for the "Tree Heist" mechanic).</li>
 * </ul>
 */
public class HamsterPlayWithItemGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                    1. Constants, Fields & State
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int LUNGE_DURATION_TICKS = 5;

    private final HamsterEntity hamster;
    private final World world;

    // --- Target & Context Data ---
    @Nullable private ItemEntity targetItem;
    @Nullable private PlayerEntity owner;
    @Nullable private Vec3d pounceStartPos;
    @Nullable private Vec3d repositionTarget;

    // --- Timers & Counters ---
    private int bounceSoundDelayTicks;
    private int playAnimSettleTicks;
    private int repositionAttempts;
    private int lungeTicks;
    private int itemInterestTimer;
    private int checkTimer = 0;

    // --- State Management ---
    // Defines the mode for the current action:
    // true = Bringing item back to owner (Retrieval / Diamond Armor effect)
    // false = Running away with item (Theft)
    private boolean isFriendlyDelivery = false;
    private State currentState = State.SCANNING;

    private enum State {
        SCANNING,
        MOVING_TO_ITEM,
        REPOSITIONING,
        POUNCING,
        FLEEING,
        RETURNING,
        PLAYING_WITH_ITEM // Handles both "Taunting" and "Presenting"
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                             2. Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterPlayWithItemGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                         3. Core AI Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        AdorableHamsterPets.LOGGER.trace("[PlayWithItemGoal-{}] Evaluating canStart...", this.hamster.getId());

        // --- 1. Resume Logic ---
        if (this.hamster.isHoldingMouthItem()) {
            if (this.hamster.isSitting()) return false; // Don't resume if sitting
            if (this.hamster.isCelebratingRetrieval()) return false; // Don't resume during Tag Game gift sequence
            if (!(this.hamster.getOwner() instanceof PlayerEntity)) return false; // Can't resume without an owner

            this.owner = (PlayerEntity) this.hamster.getOwner();
            AdorableHamsterPets.LOGGER.trace("[PlayWithItemGoal-{}] canStart SUCCEEDED: Resuming existing interaction.", this.hamster.getId());
            return true; // Resume the goal
        }

        // --- 2. Throttle Queries ---
        if (this.checkTimer > 0) {
            this.checkTimer--;
            return false;
        }
        this.checkTimer = this.getTickCount(10); // Twice per second

        // --- 3. Start Logic ---
        // Initial Checks
        if (!Configs.AHP.enableItemStealing) {
            return false;
        }
        if (this.hamster.isHoldingMouthItem() || this.hamster.isSitting()) {
            return false;
        }
        long currentTime = this.world.getTime();
        if (this.hamster.stealingCooldownEndTick > currentTime) {
            return false;
        }

        // Owner Check
        if (!(this.hamster.getOwner() instanceof PlayerEntity playerOwner)) {
            return false;
        }
        this.owner = playerOwner;

        // Find Target Item
        List<ItemEntity> nearbyItems = this.world.getEntitiesByClass(
                ItemEntity.class,
                this.hamster.getBoundingBox().expand(10.0),
                itemEntity -> (ConfigDataCache.isStealableItem(itemEntity.getStack()) || ConfigDataCache.isRetrievableItem(itemEntity.getStack()))
                        && itemEntity.isOnGround()
        );

        Optional<ItemEntity> closestItem = nearbyItems.stream()
                .filter(item -> this.hamster.getNavigation().findPathTo(item, 0) != null)
                .min((item1, item2) -> Float.compare(item1.distanceTo(this.hamster), item2.distanceTo(this.hamster)));

        if (closestItem.isEmpty()) {
            return false;
        }

        this.targetItem = closestItem.get();
        ItemStack stack = this.targetItem.getStack();

        // Chance / Mode Logic
        boolean isRetrievable = ConfigDataCache.isRetrievableItem(stack);
        boolean isStealable = ConfigDataCache.isStealableItem(stack);

        // Check for Diamond Armor Override
        boolean hasDiamondArmor = false;
        ItemStack armor = this.hamster.getArmorStack();
        if (!armor.isEmpty() && armor.getItem() instanceof HamsterArmorItem armorItem && armorItem.getMaterial() == HamsterArmorItem.HamsterArmorMaterial.DIAMOND) {
            hasDiamondArmor = true;
        }

        // Scenario A: Friendly Delivery (Retrieval Item OR Diamond Armor + Stealable Item)
        // Behavior: 100% Chance to start.
        if (isRetrievable || (isStealable && hasDiamondArmor)) {
            AdorableHamsterPets.LOGGER.trace("[PlayWithItemGoal-{}] canStart SUCCEEDED (Guaranteed). Target: {}.", this.hamster.getId(), stack.getItem());
            return true;
        }

        // Scenario B: Theft (Stealable Item + No Diamond Armor)
        // Behavior: Apply RNG Thievery Chance.
        if (isStealable) {
            float randomVal = this.hamster.getRandom().nextFloat();
            float chance = Configs.AHP.itemThieveryChance.get();

            if (randomVal > chance) {
                return false; // Failed the roll
            }

            AdorableHamsterPets.LOGGER.trace("[PlayWithItemGoal-{}] canStart SUCCEEDED (Thievery Roll Passed). Target: {}.", this.hamster.getId(), stack.getItem());
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Check for external interruptions ---
        if (this.hamster.isSitting()) {
            return false;
        }
        if (this.hamster.isCelebratingRetrieval()) {
            return false;
        }
        if (this.owner == null || !this.owner.isAlive()) {
            return false;
        }
        if (this.itemInterestTimer <= 0) {
            return false; // Timer expired
        }

        // --- 2. State-aware logic ---
        // If it is fleeing, returning, or playing, the only thing that should stop it is the player taking the item.
        if (this.currentState == State.FLEEING || this.currentState == State.RETURNING || this.currentState == State.PLAYING_WITH_ITEM) {
            if (!this.hamster.isHoldingMouthItem()) {
                AdorableHamsterPets.LOGGER.trace("[PlayWithItemGoal-{}] shouldContinue check failed: Player retrieved item.", this.hamster.getId());
                return false;
            }
        }
        // If it is moving to or pouncing on the item, it must still exist in the world.
        else if (this.currentState == State.MOVING_TO_ITEM || this.currentState == State.POUNCING) {
            if (this.targetItem == null || !this.targetItem.isAlive()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());

        ItemStack interestStack;
        if (this.hamster.isHoldingMouthItem()) {
            interestStack = this.hamster.getMouthItemStack();
        } else if (this.targetItem != null) {
            interestStack = this.targetItem.getStack();
        } else {
            stop(); // Safe fallback
            return;
        }

        // --- Determine Mode (Friendly vs Theft) ---
        determineMode(interestStack);

        if (this.hamster.isHoldingMouthItem()) {
            // --- Resuming Logic ---
            this.itemInterestTimer = this.hamster.getGenericInteractionTimer();
            this.targetItem = null; // No item entity to target, it's already held
            AdorableHamsterPets.LOGGER.trace("[PlayWithItemGoal-{}] Resuming interaction. Mode: Friendly={}", this.hamster.getId(), this.isFriendlyDelivery);

            if (this.isFriendlyDelivery) {
                this.currentState = State.RETURNING;
            } else {
                // Theft Logic: Flee or Taunt
                if (HamsterMovementUtil.shouldFlee(this.hamster, this.owner, Configs.AHP.minMiniGameFleeDistance.get())) {
                    this.currentState = State.FLEEING;
                } else {
                    this.currentState = State.PLAYING_WITH_ITEM;
                }
            }
        } else {
            // --- Fresh Start Logic ---
            this.currentState = State.MOVING_TO_ITEM;
            this.hamster.getNavigation().startMovingTo(this.targetItem, 1.5D);
            this.itemInterestTimer = this.hamster.getRandom().nextBetween(
                    Configs.AHP.minMiniGameFleeDurationSeconds.get() * 20,
                    Configs.AHP.maxMiniGameFleeDurationSeconds.get() * 20
            );
            this.hamster.setGenericInteractionTimer(this.itemInterestTimer);
            this.repositionTarget = null;
            this.repositionAttempts = 0;
            AdorableHamsterPets.LOGGER.trace("[PlayWithItemGoal-{}] Goal started fresh. State: MOVING_TO_ITEM. Duration: {} ticks. Friendly: {}", this.hamster.getId(), this.itemInterestTimer, this.isFriendlyDelivery);
        }
    }

    @Override
    public void stop() {
        AdorableHamsterPets.LOGGER.trace("[PlayWithItemGoal-{}] Goal stopped. Final state was: {}.", this.hamster.getId(), this.currentState);

        // Apply cooldown regardless of how goal ended
        this.hamster.stealingCooldownEndTick = this.world.getTime() + Configs.AHP.stealCooldownTicks.get();

        // Drop item if goal stops while hamster is still holding it
        if (this.hamster.isHoldingMouthItem()) {
            ItemStack itemHeldInMouthStack = this.hamster.getMouthItemStack();
            if (!itemHeldInMouthStack.isEmpty()) {
                this.world.spawnEntity(new ItemEntity(this.world, this.hamster.getX(), this.hamster.getY(), this.hamster.getZ(), itemHeldInMouthStack.copy()));
                this.hamster.playSound(ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_HURT_SOUNDS, this.hamster.getRandom()), 1.0f, 1.0f);

                // Get and play dynamic sound
                SoundEvent pounceSound = ModSounds.getDynamicItemSound(itemHeldInMouthStack);
                float volume = ModSounds.getDynamicSoundVolume(pounceSound);
                this.world.playSound(null, this.hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7f);
            }
        }

        this.hamster.setMouthItemStack(ItemStack.EMPTY); // Clear stored item stack
        this.hamster.setGenericInteractionTimer(0);
        this.hamster.setTaunting(false); // Stop animation
        this.hamster.setPresentingItem(false);
        this.hamster.setHoldingMouthItem(false);
        this.hamster.getNavigation().stop();
        this.targetItem = null;
        this.owner = null;
        this.currentState = State.SCANNING;

        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                         4. Tick & State Machine
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void tick() {
        // --- Timer Decrement ---
        if (this.itemInterestTimer > 0) {
            this.itemInterestTimer--;
            // Sync local timer to generic entity timer
            this.hamster.setGenericInteractionTimer(this.itemInterestTimer);
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

        switch (this.currentState) {
            case MOVING_TO_ITEM:
                if (this.targetItem == null) return;
                // Look at item; fast turn speed
                HamsterMovementUtil.faceEntity(this.hamster, this.targetItem);

                // If navigation stops before reaching the target, try to reposition.
                if (this.hamster.getNavigation().isIdle()) {
                    this.currentState = State.REPOSITIONING;
                    return; // End this tick, start repositioning on the next
                }

                if (this.hamster.distanceTo(this.targetItem) < 1.5) {
                    this.currentState = State.POUNCING;
                    this.lungeTicks = LUNGE_DURATION_TICKS;
                    this.pounceStartPos = this.hamster.getPos();
                    this.hamster.getNavigation().stop();
                    this.hamster.triggerAnimOnServer("mainController", "anim_hamster_pounce_on_item");
                    this.bounceSoundDelayTicks = 5;
                    // Play celebration sound
                    SoundEvent celebrationSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.hamster.getRandom());
                    if (celebrationSound != null) {
                        this.hamster.playSound(celebrationSound, 0.7f, this.hamster.getSoundPitch());
                    }
                }
                break;

            case REPOSITIONING:
                if (this.targetItem == null) return;
                // Check if exceeded repositioning budget.
                if (this.repositionAttempts >= 3) {
                    this.itemInterestTimer = 0; // Force the goal to stop.
                    return;
                }
                // If we don't have a reposition target yet, find one.
                if (this.repositionTarget == null) {
                    this.repositionAttempts++;
                    // Use findTo to get a spot in the direction of the item.
                    this.repositionTarget = FuzzyTargeting.findTo(this.hamster, 2, 3, Vec3d.ofCenter(this.targetItem.getBlockPos()));
                    if (this.repositionTarget != null) {
                        this.hamster.getNavigation().startMovingTo(this.repositionTarget.x, this.repositionTarget.y, this.repositionTarget.z, 1.55D);
                    } else {
                        // If we can't find a random spot, the area is likely too cramped. Stop the goal.
                        this.itemInterestTimer = 0; // Force stop
                        return;
                    }
                }
                // If the navigator is idle, we've reached the reposition target or failed. Try again.
                if (this.hamster.getNavigation().isIdle()) {
                    this.repositionTarget = null; // Clear the target to find a new one next tick if needed
                    this.currentState = State.MOVING_TO_ITEM;
                    // Explicitly restart moving to the item
                    this.hamster.getNavigation().startMovingTo(this.targetItem, 1.5D);
                }
                break;

            case POUNCING:
                if (this.targetItem == null) return;
                this.lungeTicks--;

                // --- Pounce Lunge Interpolation ---
                if (this.pounceStartPos != null && this.lungeTicks >= 0) {
                    double progress = (double)(LUNGE_DURATION_TICKS - this.lungeTicks) / LUNGE_DURATION_TICKS;
                    double easedProgress = progress * progress; // Quadratic ease-in

                    double newX = pounceStartPos.x + easedProgress * (this.targetItem.getX() - pounceStartPos.x);
                    double newZ = pounceStartPos.z + easedProgress * (this.targetItem.getZ() - pounceStartPos.z);
                    this.hamster.setPosition(newX, this.hamster.getY(), newZ);
                }

                if (this.lungeTicks < 0) {
                    ItemStack stackToSteal = this.targetItem.getStack().copy();
                    if (stackToSteal.isEmpty()) {
                        this.itemInterestTimer = 0; // Stop the goal.
                        return;
                    }

                    this.hamster.setMouthItemStack(stackToSteal);
                    this.targetItem.discard();
                    this.hamster.setHoldingMouthItem(true);

                    // --- Play Sounds and Spawn Particles ---
                    SoundEvent pounceSound = ModSounds.getDynamicItemSound(stackToSteal);
                    float volume = ModSounds.getDynamicSoundVolume(pounceSound);
                    this.world.playSound(null, this.hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7f);

                    if (!this.world.isClient) {
                        ParticleEffectsUtil.spawnParticles(
                                this.world,
                                new Vec3d(this.hamster.getX(), this.hamster.getY() + 0.5, this.hamster.getZ()),
                                ParticleTypes.END_ROD,
                                5,
                                new Vec3d(0.1, 0.1, 0.1),
                                0.05
                        );

                        ParticleEffectsUtil.spawnParticles(
                                this.world,
                                new Vec3d(this.hamster.getX(), this.hamster.getY() + 0.5, this.hamster.getZ()),
                                new ItemStackParticleEffect(ParticleTypes.ITEM, stackToSteal),
                                18,
                                new Vec3d(0.2, 0.2, 0.2),
                                0.1
                        );
                    }

                    // --- Branch based on Friendly vs Theft ---
                    if (this.isFriendlyDelivery) {
                        this.currentState = State.RETURNING;
                    } else {
                        this.currentState = State.FLEEING;
                    }
                }
                break;

            case FLEEING:
                this.hamster.setTaunting(false); // Ensure taunting off while fleeing

                double minFleeDist = Configs.AHP.minMiniGameFleeDistance.get();
                double maxFleeDist = Configs.AHP.maxMiniGameFleeDistance.get();

                if (HamsterMovementUtil.shouldStopFleeing(this.hamster, this.owner, maxFleeDist)) {
                    // Safe distance reached, start taunting
                    this.currentState = State.PLAYING_WITH_ITEM;
                    this.hamster.getNavigation().stop();
                } else if (HamsterMovementUtil.shouldFlee(this.hamster, this.owner, minFleeDist)) {
                    // If owner too close, keep running
                    Vec3d fleePos = HamsterMovementUtil.findFleePosition(
                            this.hamster,
                            this.owner,
                            minFleeDist,
                            maxFleeDist
                    );

                    if (fleePos != null) {
                        this.hamster.getNavigation().startMovingTo(fleePos.x, fleePos.y, fleePos.z, 1.5D);
                    }
                }
                break;

            case RETURNING:
                this.hamster.setPresentingItem(false); // Ensure presenting animation is off while moving

                // Move towards owner
                this.hamster.getNavigation().startMovingTo(this.owner, 1.5D);

                // Check distance
                double distToOwner = this.hamster.distanceTo(this.owner);
                if (distToOwner <= 2.5D) {
                    // Reached owner, start presenting
                    this.currentState = State.PLAYING_WITH_ITEM;
                    this.hamster.getNavigation().stop();
                }
                break;

            case PLAYING_WITH_ITEM:
                // Look at owner; fast turn speed
                HamsterMovementUtil.faceEntity(this.hamster, this.owner);

                // Initial settle delay before starting the animation
                if (!this.hamster.isTaunting() && !this.hamster.isPresentingItem() && this.playAnimSettleTicks == 0) {
                    this.playAnimSettleTicks = 5;
                }
                if (this.playAnimSettleTicks > 0) {
                    this.playAnimSettleTicks--;
                }

                // Set specific flag based on Friendly mode
                if (this.hamster.getNavigation().isIdle() && this.playAnimSettleTicks == 0) {
                    if (this.isFriendlyDelivery) {
                        this.hamster.setPresentingItem(true);
                    } else {
                        this.hamster.setTaunting(true);
                    }
                }

                // Logic Branch for Reaction
                if (this.isFriendlyDelivery) {
                    // --- Presenting Behavior ---
                    // If owner moves away significantly, return to RETURNING state to follow
                    if (this.hamster.distanceTo(this.owner) > 5.0D) {
                        this.currentState = State.RETURNING;
                        this.hamster.setPresentingItem(false);
                        this.playAnimSettleTicks = 0;
                    }
                } else {
                    // --- Taunting Behavior ---
                    // If owner gets too close, flee
                    if (HamsterMovementUtil.shouldFlee(this.hamster, this.owner, Configs.AHP.minMiniGameFleeDistance.get())) {
                        this.currentState = State.FLEEING;
                        this.hamster.setTaunting(false);
                        this.playAnimSettleTicks = 0;
                    }
                }
                break;
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                            5. Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Determines if the hamster should deliver the item or steal it.
     */
    private void determineMode(ItemStack stack) {
        // 1. Is it explicitly retrievable? (e.g. Acorn)
        if (ConfigDataCache.isRetrievableItem(stack)) {
            this.isFriendlyDelivery = true;
            return;
        }

        // 2. Is it stealable? (e.g. Diamond)
        if (ConfigDataCache.isStealableItem(stack)) {
            // Check for Diamond Armor Override
            ItemStack armor = this.hamster.getArmorStack();
            if (!armor.isEmpty() && armor.getItem() instanceof HamsterArmorItem armorItem && armorItem.getMaterial() == HamsterArmorItem.HamsterArmorMaterial.DIAMOND) {
                this.isFriendlyDelivery = true; // Armor cures kleptomania
            } else {
                this.isFriendlyDelivery = false; // Steal it
            }
            return;
        }

        // Fallback
        this.isFriendlyDelivery = false;
    }
}