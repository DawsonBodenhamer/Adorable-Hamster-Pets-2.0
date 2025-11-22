package net.dawson.adorablehamsterpets.integration.jade;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;

public enum HamsterDebugComponentProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_debug_info");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!Configs.AHP.enableJadeHamsterDebugInfo) {
            return;
        }

        Entity entity = accessor.getEntity();
        if (!(entity instanceof HamsterEntity hamster)) {
            return;
        }

        // --- Animation State ---
        tooltip.add(Text.literal("--- Current Animation ---").formatted(Formatting.GRAY));
        AnimationController<?> controller = hamster.getAnimatableInstanceCache().getManagerForId(hamster.getId()).getAnimationControllers().get("mainController");
        if (controller != null) {
            // Get the currently playing animation object from the controller
            AnimationProcessor.QueuedAnimation currentAnim = controller.getCurrentAnimation();

            if (currentAnim != null) {
                // Get the name from the animation record itself
                tooltip.add(fText("Current Anim: %s", Text.literal(currentAnim.animation().name()).formatted(Formatting.AQUA)));
            } else {
                tooltip.add(fText("Current Anim: %s", Text.literal("None").formatted(Formatting.GRAY)));
            }
        }

        // --- AI Goal & Action States ---
        tooltip.add(Text.literal("--- AI & Action States ---").formatted(Formatting.GRAY));
        tooltip.add(fText("Sitting (Command): %s", (hamster.isSitting() ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED))));
        tooltip.add(fText("Sitting (Vanilla Pose): %s", (hamster.isInSittingPose() ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED))));
        tooltip.add(fText("Sleeping (Wild/General): %s", (hamster.isSleeping() ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED))));
        tooltip.add(fText("Cleaning: %s", (hamster.isCleaning() ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED))));

        if (hamster.isKnockedOut()) {
            tooltip.add(fText("State: %s", Text.literal("Knocked Out").formatted(Formatting.RED, Formatting.BOLD)));
        } else if (hamster.isThrown()) {
            tooltip.add(fText("State: %s", Text.literal("Thrown").formatted(Formatting.AQUA)));
        } else if (hamster.isSulking()) {
            tooltip.add(fText("State: %s", Text.literal("Sulking").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)));
        } else if (hamster.isCelebratingDiamond()) {
            tooltip.add(fText("State: %s", Text.literal("Celebrating Diamond").formatted(Formatting.AQUA, Formatting.BOLD)));
        }

        tooltip.add(fText("Is Navigating: %s", (!hamster.getNavigation().isIdle() ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED)) ));
        LivingEntity target = hamster.getTarget();
        tooltip.add(fText("Has Target: %s", (target != null ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED))));
        if (target != null) {
            tooltip.add(fText("  Target: %s", Text.literal(target.getName().getString()).formatted(Formatting.WHITE)));
        }
        String activeGoalName = hamster.getActiveCustomGoalDebugName();
        tooltip.add(fText("Current Custom Goal: %s", Text.literal(activeGoalName).formatted(activeGoalName.equals("None") ? Formatting.GRAY : Formatting.AQUA)));

        // --- Bed Link Status ---
        tooltip.add(Text.literal("--- Bed Link ---").formatted(Formatting.GRAY));
        NbtCompound serverData = accessor.getServerData();
        boolean isWanderActive = serverData.getBoolean("IsWanderModeActive");
        boolean isOnTheWayToBed = serverData.getBoolean("IsOnTheWayToBed");
        int goToBedDelay = serverData.getInt("GoToBedDelay");

        // Display wander mode status
        tooltip.add(fText("Wander Mode: %s", isWanderActive ? Text.literal("ACTIVE").formatted(Formatting.GREEN) : Text.literal("INACTIVE").formatted(Formatting.RED)));

        // If wandering, show distance. If pathfinding to bed, show that instead.
        if (isWanderActive) {
            if (isOnTheWayToBed) {
                if (goToBedDelay > 0) {
                    tooltip.add(fText("  Status: %s", Text.literal(String.format("Waiting... (starts in %.1f s)", goToBedDelay / 20.0)).formatted(Formatting.YELLOW)));
                } else {
                    tooltip.add(fText("  Status: %s", Text.literal("Pathfinding to bed...").formatted(Formatting.YELLOW)));
                }
            } else if (serverData.contains("WanderDistance")) {
                String distanceStr = serverData.getString("WanderDistance");
                tooltip.add(fText("  Wander Distance: %s", Text.literal(distanceStr).formatted(Formatting.AQUA)));
            }
        }

        // --- Tamed Sleep Sequence ---
        if (hamster.isTamed()) {
            tooltip.add(Text.literal("--- Tamed Sleep Sequence ---").formatted(Formatting.GRAY));
            HamsterEntity.DozingPhase phase = hamster.getDozingPhase();
            tooltip.add(fText("Dozing Phase: %s", Text.literal(phase.name()).formatted(phase != HamsterEntity.DozingPhase.NONE ? Formatting.AQUA : Formatting.WHITE)));
            if (phase == HamsterEntity.DozingPhase.DEEP_SLEEP || phase == HamsterEntity.DozingPhase.SETTLING_INTO_SLUMBER) {
                tooltip.add(fText("  Deep Sleep Anim: %s", Text.literal(hamster.getCurrentDeepSleepAnimationIdFromTracker()).formatted(Formatting.AQUA)));
            }
        }

        // --- Ore Seeking States  ---
        tooltip.add(Text.literal("--- Ore Seeking ---").formatted(Formatting.GRAY));
        tooltip.add(fText("Primed to Seek: %s", hamster.isPrimedToSeekDiamonds ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED)));
        if (hamster.currentOreTarget != null) {
            tooltip.add(fText("  Current Ore Target: %s", Text.literal(hamster.currentOreTarget.toString()).formatted(Formatting.AQUA)));
        } else {
            tooltip.add(fText("  Current Ore Target: %s", Text.literal("None").formatted(Formatting.GRAY)));
        }
        long foundOreCooldown = hamster.foundOreCooldownEndTick - hamster.getWorld().getTime();
        if (Configs.AHP.enableIndependentDiamondSeekCooldown && foundOreCooldown > 0) {
            tooltip.add(fText("  Found Ore Cooldown: %s sec", Text.literal(String.format("%.1f", foundOreCooldown / 20.0)).formatted(Formatting.YELLOW)));
        } else if (Configs.AHP.enableIndependentDiamondSeekCooldown) {
            tooltip.add(fText("  Found Ore Cooldown: %s", Text.literal("Ready").formatted(Formatting.GREEN)));
        } else {
            tooltip.add(fText("  Found Ore Cooldown: %s", Text.literal("Disabled").formatted(Formatting.GRAY)));
        }

        // --- Diamond Stealing States ---
        tooltip.add(Text.literal("--- Diamond Stealing ---").formatted(Formatting.GRAY));
        tooltip.add(fText("Is Stealing: %s", hamster.isStealingDiamond() ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED)));
        if (hamster.isStealingDiamond()) {
            int remainingTicks = hamster.getStealDurationTimer();
            tooltip.add(fText("  Time Remaining: %s sec", Text.literal(String.format("%.1f", remainingTicks / 20.0)).formatted(Formatting.YELLOW)));
        }

        // --- Love & Interaction States ---
        tooltip.add(Text.literal("--- Love & Interaction ---").formatted(Formatting.GRAY));
        tooltip.add(fText("Begging: %s", (hamster.isBegging() ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED))));
        tooltip.add(fText("Refusing Food: %s", (hamster.isRefusingFood() ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED))));
        boolean inLoveDataTracker = hamster.isInLove(); // Checks DataTracker IS_IN_LOVE
        boolean inLoveCustomTimer = hamster.customLoveTimer > 0; // Checks the breeding timer directly
        tooltip.add(fText("In Love (Tracker): %s", (inLoveDataTracker ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED))));
        tooltip.add(fText("In Love (Timer): %s (%d ticks)", (inLoveCustomTimer ? Text.literal("true").formatted(Formatting.GREEN) : Text.literal("false").formatted(Formatting.RED)), hamster.customLoveTimer));

        // --- General Info ---
        tooltip.add(Text.literal("--- General Info ---").formatted(Formatting.GRAY));
        tooltip.add(fText("Tamed: %s", hamster.isTamed() ? Text.literal("Yes").formatted(Formatting.GREEN) : Text.literal("No").formatted(Formatting.RED)));
        if (hamster.isTamed() && hamster.getOwner() != null) {
            tooltip.add(fText("  Owner: %s", Text.literal(hamster.getOwner().getName().getString()).formatted(Formatting.WHITE)));
        }
        tooltip.add(fText("Variant: %s (ID: %d)", Text.literal(hamster.getVariantEnum().name()).formatted(Formatting.AQUA), hamster.getVariant()));
        tooltip.add(fText("Age: %s", hamster.isBaby() ? Text.literal("Baby").formatted(Formatting.AQUA) : Text.literal("Adult").formatted(Formatting.WHITE)));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(NbtCompound data, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (entity instanceof HamsterEntity hamster) {
            data.putBoolean("IsWanderModeActive", hamster.isWanderModeActive());
            data.putBoolean("IsOnTheWayToBed", hamster.isOnTheWayToBed());
            data.putInt("GoToBedDelay", hamster.getGoToBedDelayTicks());

            if (hamster.isWanderModeActive()) {
                hamster.getLinkedBedPos().ifPresent(globalPos -> {
                    World world = hamster.getWorld();
                    if (world instanceof ServerWorld serverWorld && serverWorld.getRegistryKey() == globalPos.getDimension()) {
                        if (serverWorld.getBlockEntity(globalPos.getPos()) instanceof HamsterBedBlockEntity bedEntity) {
                            data.putString("WanderDistance", bedEntity.getWanderDistance().asString());
                        }
                    }
                });
            }
        }
    }

    // Helper for formatted text
    private Text fText(String format, Object... args) {
        Text[] formattedArgs = new Text[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Text textComponent) {
                formattedArgs[i] = textComponent;
            } else {
                formattedArgs[i] = Text.literal(String.valueOf(args[i])).formatted(Formatting.WHITE);
            }
        }
        MutableText result = Text.empty();
        String[] parts = format.split("%s", -1);
        for (int i = 0; i < parts.length; i++) {
            result.append(Text.literal(parts[i]).formatted(Formatting.GOLD));
            if (i < formattedArgs.length) {
                result.append(formattedArgs[i]);
            }
        }
        return result;
    }
}