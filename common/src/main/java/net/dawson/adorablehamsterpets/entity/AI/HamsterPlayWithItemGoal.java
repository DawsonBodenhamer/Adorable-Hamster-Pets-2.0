package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

public class HamsterPlayWithItemGoal extends HamsterAbstractItemInteractionGoal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants & Enums
     * ────────────────────────────────────────────────────────────────────────────*/

    private enum SubState {
        NONE,
        FLEEING,
        RETURNING,
        PLAYING
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private PlayerEntity owner;
    private SubState currentSubState = SubState.NONE;
    private int playAnimSettleTicks;
    private int itemInterestTimer;
    private boolean isFriendlyDelivery;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterPlayWithItemGoal(HamsterEntity hamster) {
        super(hamster);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        // Hook for resuming an interaction if already holding an item
        if (this.hamster.isHoldingMouthItem()) {
            if (HamsterMovementUtil.shouldNotMove(this.hamster)
                    || !(this.hamster.getOwner() instanceof PlayerEntity)) {
                return false;
            }

            this.owner = (PlayerEntity) this.hamster.getOwner();
            return true;
        }

        // Run standard ground search
        if (!super.canStart()) {
            return false;
        }

        // Apply RNG specifically for thievery items
        ItemStack stack = this.targetItem.getStack();
        boolean isRetrievable = ConfigDataCache.isRetrievableItem(stack);
        boolean isStealable = ConfigDataCache.isStealableItem(stack);
        boolean hasDiamondArmor = hasDiamondArmor();

        if (isRetrievable || (isStealable && hasDiamondArmor)) {
            return true;
        }

        if (isStealable && this.hamster.getRandom().nextFloat() <= Configs.AHP_MAIN.itemThieveryChance.get()) {
            return true;
        }

        this.targetItem = null;
        return false;
    }

    @Override
    public void start() {
        this.hamster.setPathfindingPenalty(PathNodeType.WATER, 0.0F);

        // Resume state machine directly to post-pounce if already holding an item
        if (this.hamster.isHoldingMouthItem()) {
            this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
            this.itemInterestTimer = this.hamster.getGenericInteractionTimer();
            determineMode(this.hamster.getMouthItemStack());

            this.currentState = State.POST_POUNCE_ACTION;

            if (this.isFriendlyDelivery) {
                this.currentSubState = SubState.RETURNING;
            } else {
                if (HamsterMovementUtil.shouldFlee(this.hamster, this.owner, Configs.AHP_MAIN.minMiniGameFleeDistance.get())) {
                    this.currentSubState = SubState.FLEEING;
                } else {
                    this.currentSubState = SubState.PLAYING;
                }
            }
        } else {
            // Standard start from scratch
            super.start();
            this.itemInterestTimer = this.hamster.getRandom().nextBetween(
                    Configs.AHP_MAIN.minMiniGameFleeDurationSeconds.get() * 20,
                    Configs.AHP_MAIN.maxMiniGameFleeDurationSeconds.get() * 20
            );
            this.hamster.setGenericInteractionTimer(this.itemInterestTimer);
            this.currentSubState = SubState.NONE;
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected boolean isValidTarget(ItemStack stack) {
        return ConfigDataCache.isStealableItem(stack) || ConfigDataCache.isRetrievableItem(stack);
    }

    @Override
    protected boolean canStartBaseChecks() {
        if (!Configs.AHP_MAIN.enableItemStealing) {
            return false;
        }

        if (this.hamster.isHoldingMouthItem() || HamsterMovementUtil.shouldNotMove(this.hamster)) {
            return false;
        }

        if (this.hamster.stealingCooldownEndTick > this.world.getTime()) {
            return false;
        }

        if (!(this.hamster.getOwner() instanceof PlayerEntity playerOwner)) {
            return false;
        }

        this.owner = playerOwner;
        return true;
    }

    @Override
    protected boolean shouldContinueBaseChecks() {
        if (HamsterMovementUtil.shouldNotMove(this.hamster)
                || this.owner == null
                || !this.owner.isAlive()) {
            return false;
        }

        if (this.itemInterestTimer <= 0) {
            return false;
        }

        if (this.currentState == State.POST_POUNCE_ACTION && !this.hamster.isHoldingMouthItem()) {
            return false;
        }

        return true;
    }

    @Override
    protected void onPounceComplete(ItemStack stackSnapshot) {
        this.hamster.setMouthItemStack(stackSnapshot);
        this.hamster.setHoldingMouthItem(true);

        if (this.targetItem != null) {
            this.targetItem.discard();
        }

        // Audio feedback
        SoundEvent pounceSound = ModSounds.getDynamicItemSound(stackSnapshot);
        float volume = ModSounds.getDynamicSoundVolume(pounceSound);
        this.world.playSound(null, this.hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7F);

        // Visual feedback
        if (!this.world.isClient()) {
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
                    new ItemStackParticleEffect(ParticleTypes.ITEM, stackSnapshot),
                    18,
                    new Vec3d(0.2, 0.2, 0.2),
                    0.1
            );
        }

        determineMode(stackSnapshot);

        if (this.isFriendlyDelivery) {
            this.currentSubState = SubState.RETURNING;
        } else {
            this.currentSubState = SubState.FLEEING;
        }
    }

    @Override
    protected void tickPostPounce() {
        // Manage generic interest timer
        if (this.itemInterestTimer > 0) {
            this.itemInterestTimer--;
            this.hamster.setGenericInteractionTimer(this.itemInterestTimer);
        }

        if (this.owner == null) {
            return;
        }

        switch (this.currentSubState) {
            case FLEEING -> {
                this.hamster.setTaunting(false);

                double minFleeDist = Configs.AHP_MAIN.minMiniGameFleeDistance.get();
                double maxFleeDist = Configs.AHP_MAIN.maxMiniGameFleeDistance.get();

                if (HamsterMovementUtil.shouldStopFleeing(this.hamster, this.owner, maxFleeDist)) {
                    this.currentSubState = SubState.PLAYING;
                    this.hamster.getNavigation().stop();
                } else if (HamsterMovementUtil.shouldFlee(this.hamster, this.owner, minFleeDist)) {
                    Vec3d fleePos = HamsterMovementUtil.findFleePosition(this.hamster, this.owner, minFleeDist, maxFleeDist);
                    if (fleePos != null) {
                        this.hamster.getNavigation().startMovingTo(fleePos.x, fleePos.y, fleePos.z, 1.5D);
                    }
                }
            }
            case RETURNING -> {
                this.hamster.setPresentingItem(false);
                this.hamster.getNavigation().startMovingTo(this.owner, 1.5D);

                if (this.hamster.distanceTo(this.owner) <= 2.5D) {
                    this.currentSubState = SubState.PLAYING;
                    this.hamster.getNavigation().stop();
                }
            }
            case PLAYING -> {
                HamsterMovementUtil.faceEntity(this.hamster, this.owner);

                // Add small delay before animation logic locks in
                if (!this.hamster.isTaunting() && !this.hamster.isPresentingItem() && this.playAnimSettleTicks == 0) {
                    this.playAnimSettleTicks = 5;
                }

                if (this.playAnimSettleTicks > 0) {
                    this.playAnimSettleTicks--;
                }

                // Force specific animation states
                if (this.hamster.getNavigation().isIdle() && this.playAnimSettleTicks == 0) {
                    if (this.isFriendlyDelivery) {
                        this.hamster.setPresentingItem(true);
                    } else {
                        this.hamster.setTaunting(true);
                    }
                }

                // Monitor owner distance to jump back to movement states
                if (this.isFriendlyDelivery) {
                    // Presenting
                    if (this.hamster.distanceTo(this.owner) > 5.0D) {
                        this.currentSubState = SubState.RETURNING;
                        this.hamster.setPresentingItem(false);
                        this.playAnimSettleTicks = 0;
                    }
                } else {
                    // Taunting
                    if (HamsterMovementUtil.shouldFlee(this.hamster, this.owner, Configs.AHP_MAIN.minMiniGameFleeDistance.get())) {
                        this.currentSubState = SubState.FLEEING;
                        this.hamster.setTaunting(false);
                        this.playAnimSettleTicks = 0;
                    }
                }
            }
        }
    }

    @Override
    protected void onGoalStopped() {
        this.hamster.stealingCooldownEndTick = this.world.getTime() + Configs.AHP_MAIN.stealCooldownTicks.get();

        // Safely drop item if still held when goal ends unexpectedly
        if (this.hamster.isHoldingMouthItem()) {
            ItemStack itemHeld = this.hamster.getMouthItemStack();
            if (!itemHeld.isEmpty()) {
                this.world.spawnEntity(new ItemEntity(
                        this.world, this.hamster.getX(), this.hamster.getY(), this.hamster.getZ(), itemHeld.copy()
                ));

                // Disappointment noise
                this.hamster.playSound(
                        ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_HURT_SOUNDS, this.hamster.getRandom()), 1.0F, 1.0F
                );

                SoundEvent pounceSound = ModSounds.getDynamicItemSound(itemHeld);
                float volume = ModSounds.getDynamicSoundVolume(pounceSound);
                this.world.playSound(null, this.hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7F);
            }
        }

        this.hamster.setMouthItemStack(ItemStack.EMPTY);
        this.hamster.setGenericInteractionTimer(0);
        this.hamster.setTaunting(false);
        this.hamster.setPresentingItem(false);
        this.hamster.setHoldingMouthItem(false);

        this.owner = null;
        this.currentSubState = SubState.NONE;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean hasDiamondArmor() {
        ItemStack armor = this.hamster.getArmorStack();
        if (!armor.isEmpty() && armor.getItem() instanceof HamsterArmorItem armorItem) {
            return armorItem.getMaterial() == HamsterArmorItem.HamsterArmorMaterial.DIAMOND;
        }
        return false;
    }

    private void determineMode(ItemStack stack) {
        if (ConfigDataCache.isRetrievableItem(stack)) {
            this.isFriendlyDelivery = true;
        } else if (ConfigDataCache.isStealableItem(stack)) {
            this.isFriendlyDelivery = hasDiamondArmor(); // Diamond armor cures kleptomania
        } else {
            this.isFriendlyDelivery = false;
        }
    }
}
