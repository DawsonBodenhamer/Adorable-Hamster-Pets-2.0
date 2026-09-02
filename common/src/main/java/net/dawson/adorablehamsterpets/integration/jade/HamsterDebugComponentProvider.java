package net.dawson.adorablehamsterpets.integration.jade;

import com.geckolib.animation.RawAnimation;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import com.geckolib.animation.AnimationController;

public enum HamsterDebugComponentProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_debug_info");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!Configs.AHP_UI.enableJadeHamsterDebugInfo) {
            return;
        }

        Entity entity = accessor.getEntity();
        if (!(entity instanceof HamsterEntity hamster)) {
            return;
        }

        // --- Animation State ---
        tooltip.add(Component.literal("--- Current Animation ---").withStyle(ChatFormatting.GRAY));
        AnimationController<?> controller = hamster.getAnimatableInstanceCache().getManagerForId(hamster.getId()).getAnimationControllers().get("mainController");
        if (controller != null) {
            // Get the currently playing animation object from the controller
            RawAnimation currentAnim = controller.getCurrentRawAnimation();

            if (currentAnim != null) {
                // Get the name from the animation record itself
                tooltip.add(fText("Current Anim: %s", Component.literal(currentAnim.getAnimationStages().stream().map(RawAnimation.Stage::animationName).collect(java.util.stream.Collectors.joining(" > "))).withStyle(ChatFormatting.AQUA)));
            } else {
                tooltip.add(fText("Current Anim: %s", Component.literal("None").withStyle(ChatFormatting.GRAY)));
            }
        }

        // --- AI Goal & Action States ---
        tooltip.add(Component.literal("--- AI & Action States ---").withStyle(ChatFormatting.GRAY));
        tooltip.add(fText("Sitting (Command): %s", (hamster.isOrderedToSit() ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED))));
        tooltip.add(fText("Sitting (Vanilla Pose): %s", (hamster.isInSittingPose() ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED))));
        tooltip.add(fText("Sleeping (Wild/General): %s", (hamster.isSleeping() ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED))));
        tooltip.add(fText("Cleaning: %s", (hamster.isCleaning() ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED))));

        if (hamster.isKnockedOut()) {
            tooltip.add(fText("State: %s", Component.literal("Knocked Out").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        } else if (hamster.isSulking()) {
            tooltip.add(fText("State: %s", Component.literal("Sulking").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)));
        } else if (hamster.isCelebratingDiamond()) {
            tooltip.add(fText("State: %s", Component.literal("Celebrating Diamond").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
        }

        tooltip.add(fText("Is Navigating: %s", (!hamster.getNavigation().isDone() ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED)) ));
        LivingEntity target = hamster.getTarget();
        tooltip.add(fText("Has Target: %s", (target != null ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED))));
        if (target != null) {
            tooltip.add(fText("  Target: %s", Component.literal(target.getName().getString()).withStyle(ChatFormatting.WHITE)));
        }
        String activeGoalName = hamster.getActiveCustomGoalName();
        tooltip.add(fText("Current Custom Goal: %s", Component.literal(activeGoalName).withStyle(activeGoalName.equals("None") ? ChatFormatting.GRAY : ChatFormatting.AQUA)));

        // --- Bed Link Status ---
        tooltip.add(Component.literal("--- Bed Link ---").withStyle(ChatFormatting.GRAY));
        CompoundTag serverData = accessor.getServerData();
        boolean isWanderActive = serverData.getBooleanOr("IsWanderModeActive", false);
        boolean isOnTheWayToBed = serverData.getBooleanOr("IsOnTheWayToBed", false);
        int goToBedDelay = serverData.getIntOr("GoToBedDelay", 0);

        // Display wander mode status
        tooltip.add(fText("Wander Mode: %s", isWanderActive ? Component.literal("ACTIVE").withStyle(ChatFormatting.GREEN) : Component.literal("INACTIVE").withStyle(ChatFormatting.RED)));

        // If wandering, show distance. If pathfinding to bed, show that instead.
        if (isWanderActive) {
            if (isOnTheWayToBed) {
                if (goToBedDelay > 0) {
                    tooltip.add(fText("  Status: %s", Component.literal(String.format("Waiting... (starts in %.1f s)", goToBedDelay / 20.0)).withStyle(ChatFormatting.YELLOW)));
                } else {
                    tooltip.add(fText("  Status: %s", Component.literal("Pathfinding to bed...").withStyle(ChatFormatting.YELLOW)));
                }
            } else if (serverData.contains("WanderDistance")) {
                String distanceStr = serverData.getStringOr("WanderDistance", "");
                tooltip.add(fText("  Wander Distance: %s", Component.literal(distanceStr).withStyle(ChatFormatting.AQUA)));
            }
        }

        // --- Tamed Sleep Sequence ---
        if (hamster.isTame()) {
            tooltip.add(Component.literal("--- Tamed Sleep Sequence ---").withStyle(ChatFormatting.GRAY));
            HamsterEntity.DozingPhase phase = hamster.getDozingPhase();
            tooltip.add(fText("Dozing Phase: %s", Component.literal(phase.name()).withStyle(phase != HamsterEntity.DozingPhase.NONE ? ChatFormatting.AQUA : ChatFormatting.WHITE)));
            if (phase == HamsterEntity.DozingPhase.DEEP_SLEEP || phase == HamsterEntity.DozingPhase.SETTLING_INTO_SLUMBER) {
                tooltip.add(fText("  Deep Sleep Anim: %s", Component.literal(hamster.getCurrentDeepSleepAnimationIdFromTracker()).withStyle(ChatFormatting.AQUA)));
            }
        }

        // --- Ore Seeking States  ---
        tooltip.add(Component.literal("--- Ore Seeking ---").withStyle(ChatFormatting.GRAY));
        tooltip.add(fText("Primed to Seek: %s", hamster.isPrimedToSeekDiamonds ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED)));
        if (hamster.currentOreTarget != null) {
            tooltip.add(fText("  Current Ore Target: %s", Component.literal(hamster.currentOreTarget.toString()).withStyle(ChatFormatting.AQUA)));
        } else {
            tooltip.add(fText("  Current Ore Target: %s", Component.literal("None").withStyle(ChatFormatting.GRAY)));
        }
        long foundOreCooldown = hamster.foundOreCooldownEndTick - hamster.level().getGameTime();
        if (Configs.AHP_MAIN.enableIndependentDiamondSeekCooldown && foundOreCooldown > 0) {
            tooltip.add(fText("  Found Ore Cooldown: %s sec", Component.literal(String.format("%.1f", foundOreCooldown / 20.0)).withStyle(ChatFormatting.YELLOW)));
        } else if (Configs.AHP_MAIN.enableIndependentDiamondSeekCooldown) {
            tooltip.add(fText("  Found Ore Cooldown: %s", Component.literal("Ready").withStyle(ChatFormatting.GREEN)));
        } else {
            tooltip.add(fText("  Found Ore Cooldown: %s", Component.literal("Disabled").withStyle(ChatFormatting.GRAY)));
        }

        // --- Stealing/Fetching States ---
        tooltip.add(Component.literal("--- Item Stealing/Fetching ---").withStyle(ChatFormatting.GRAY));
        boolean isHolding = hamster.isHoldingMouthItem();
        tooltip.add(fText("Is Interested in Item: %s", isHolding ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED)));

        if (isHolding) {
            int remainingTicks = hamster.getGenericInteractionTimer();
            tooltip.add(fText("  Time Remaining: %s sec", Component.literal(String.format("%.1f", remainingTicks / 20.0)).withStyle(ChatFormatting.YELLOW)));

            if (hamster.isTaunting()) {
                tooltip.add(fText("  Action: %s", Component.literal("Taunting").withStyle(ChatFormatting.GOLD)));
            } else if (hamster.isPresentingItem()) {
                tooltip.add(fText("  Action: %s", Component.literal("Presenting").withStyle(ChatFormatting.AQUA)));
            } else {
                tooltip.add(fText("  Action: %s", Component.literal("Moving/Fleeing").withStyle(ChatFormatting.WHITE)));
            }
        }

        // --- Love & Interaction States ---
        tooltip.add(Component.literal("--- Love & Interaction ---").withStyle(ChatFormatting.GRAY));
        tooltip.add(fText("Begging: %s", (hamster.isBegging() ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED))));
        tooltip.add(fText("Refusing Food: %s", (hamster.isRefusingFood() ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED))));
        boolean inLoveDataTracker = hamster.isInLove(); // Checks DataTracker IS_IN_LOVE
        boolean inLoveCustomTimer = hamster.customLoveTimer > 0; // Checks the breeding timer directly
        tooltip.add(fText("In Love (Tracker): %s", (inLoveDataTracker ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED))));
        tooltip.add(fText("In Love (Timer): %s (%d ticks)", (inLoveCustomTimer ? Component.literal("true").withStyle(ChatFormatting.GREEN) : Component.literal("false").withStyle(ChatFormatting.RED)), hamster.customLoveTimer));

        tooltip.add(Component.literal("--- General Info ---").withStyle(ChatFormatting.GRAY));
        tooltip.add(fText("Tamed: %s", hamster.isTame() ? Component.literal("Yes").withStyle(ChatFormatting.GREEN) : Component.literal("No").withStyle(ChatFormatting.RED)));
        if (hamster.isTame() && hamster.getOwner() != null) {
            tooltip.add(fText("  Owner: %s", Component.literal(hamster.getOwner().getName().getString()).withStyle(ChatFormatting.WHITE)));
        }
        tooltip.add(fText("Age: %s", hamster.isBaby() ? Component.literal("Baby").withStyle(ChatFormatting.AQUA) : Component.literal("Adult").withStyle(ChatFormatting.WHITE)));
        tooltip.add(fText("Aggression State: %s", Component.literal(hamster.getAggressionState().name()).withStyle(ChatFormatting.AQUA)));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    // Helper for formatted text
    private Component fText(String format, Object... args) {
        Component[] formattedArgs = new Component[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Component textComponent) {
                formattedArgs[i] = textComponent;
            } else {
                formattedArgs[i] = Component.literal(String.valueOf(args[i])).withStyle(ChatFormatting.WHITE);
            }
        }
        MutableComponent result = Component.empty();
        String[] parts = format.split("%s", -1);
        for (int i = 0; i < parts.length; i++) {
            result.append(Component.literal(parts[i]).withStyle(ChatFormatting.GOLD));
            if (i < formattedArgs.length) {
                result.append(formattedArgs[i]);
            }
        }
        return result;
    }
}