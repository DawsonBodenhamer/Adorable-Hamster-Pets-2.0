package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

public class HamsterSnackOnItemGoal extends HamsterAbstractItemInteractionGoal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean isFinished = false;
    private int postPounceTimer = 0;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterSnackOnItemGoal(HamsterEntity hamster) {
        super(hamster);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void start() {
        super.start();
        this.hamster.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected boolean isValidTarget(ItemStack stack) {
        if (!ConfigDataCache.isSnackableItem(stack)) {
            return false;
        }

        // Check explicit seed refusal config
        if (Configs.AHP_MAIN.ignoreSeeds && (stack.isIn(ItemTags.VILLAGER_PLANTABLE_SEEDS) || stack.getTranslationKey().contains("seed"))) {
            return false;
        }

        // Check for invalid items
        if (!HamsterInventoryUtil.canInsertIntoPouch(stack)) {
            return false;
        }

        return HamsterInventoryUtil.hasRoomInCheeks(this.hamster, stack);
    }

    @Override
    protected boolean canStartBaseChecks() {
        if (!this.hamster.isTamed()) {
            return false;
        }

        // Check config
        if (Configs.AHP_MAIN.restrictItemSnackingToWanderMode && !this.hamster.isWanderModeActive()) {
            return false;
        }

        // Exclusions
        if (this.hamster.isOnTheWayToBed()
                || this.hamster.isSitting()
                || this.hamster.isSleeping()
                || this.hamster.isKnockedOut()
                || this.hamster.isSulking()
                || this.hamster.isHoldingMouthItem()
                || this.hamster.isFrozenMovement()
                || this.hamster.isCelebratingBaby()
                || this.hamster.isCelebratingDiamond()
        ) {
            return false;
        }

        // Pre-check inventory space for performance
        return HamsterInventoryUtil.hasRoomInCheeks(this.hamster, ItemStack.EMPTY);
    }

    @Override
    protected boolean shouldContinueBaseChecks() {
        if (this.isFinished) {
            return false;
        }

        return !this.hamster.isSitting() && !this.hamster.isOnTheWayToBed();
    }

    @Override
    protected void onPounceComplete(ItemStack stackSnapshot) {
        // Attempt insertion into cheek pouches
        ItemStack remaining = HamsterInventoryUtil.insertIntoCheeks(this.hamster, stackSnapshot);

        if (this.targetItem != null) {
            if (remaining.isEmpty()) {
                this.targetItem.discard();
            } else {
                this.targetItem.setStack(remaining);
            }
        }

        // Audio feedback
        SoundEvent pounceSound = ModSounds.getDynamicItemSound(stackSnapshot);
        float volume = ModSounds.getDynamicSoundVolume(pounceSound);
        this.world.playSound(null, this.hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.0F);

        // Visual feedback
        if (!this.world.isClient()) {
            ParticleEffectsUtil.spawnParticles(
                    this.world,
                    new Vec3d(this.hamster.getX(), this.hamster.getY() + 0.2, this.hamster.getZ()),
                    new ItemStackParticleEffect(ParticleTypes.ITEM, stackSnapshot),
                    15,
                    new Vec3d(0.15, 0.15, 0.15),
                    0.0
            );
        }

        // If in liquid, skip animation wait time
        if (this.hamster.isTouchingWater() || this.hamster.isInLava()) {
            this.postPounceTimer = 0;
        } else {
            // The pounce lunge took 5 ticks, wait 18 more ticks for the full 23-tick animation to finish
            this.postPounceTimer = 18;
        }
    }

    @Override
    protected void tickPostPounce() {
        this.postPounceTimer--;
        if (this.postPounceTimer <= 0) {
            this.isFinished = true;
        }
    }

    @Override
    protected void onGoalStopped() {
        this.isFinished = false;
        this.postPounceTimer = 0;
        this.hamster.cropSnackCooldownEndTick = this.world.getTime() + Configs.AHP_MAIN.cropSnackCooldownTicks.get();
    }
}