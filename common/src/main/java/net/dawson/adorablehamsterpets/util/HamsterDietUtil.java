package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.AhpMainConfig;
import net.dawson.adorablehamsterpets.config.AhpItemConfig;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static net.dawson.adorablehamsterpets.sound.ModSounds.getRandomSoundFrom;

/**
 * Handles diet, feeding, healing, buffs, and refusal logic for tamed hamsters.
 */
public final class HamsterDietUtil {

    public static final int REFUSE_FOOD_TIMER_TICKS = 40;

    private HamsterDietUtil() {}

    /**
     * Checks if hamster should refuse being fed same item twice consecutively.
     * Triggers refusal animation and chat feedback if so.
     */
    public static boolean checkAndHandleRefusal(HamsterEntity hamster, Player player, ItemStack stack) {
        if (ConfigDataCache.isRepeatableFood(stack)) return false;

        // Bypass for babies if config enabled
        if (hamster.isBaby() && AdorableHamsterPets.ITEM_CONFIG.disableBabyFoodRefusal) return false;

        ItemStack lastFood = hamster.getLastFoodItem();
        if (lastFood != null && !lastFood.isEmpty() && ItemStack.isSameItem(lastFood, stack)) {
            if (!hamster.level().isClientSide()) {
                hamster.setRefusingFood(true);
                hamster.setRefuseTimer(REFUSE_FOOD_TIMER_TICKS);
                player.displayClientMessage(Component.translatable("message.adorablehamsterpets.food_refusal"), true);

                // Trigger refusal animation based on movement state
                hamster.playRefusalAnimation();
            }
            return true;
        }
        return false;
    }

    /**
     * Processes feeding logic for pouch unlocking, buff application, healing, and breeding.
     * Evaluates in strict priority order.
     *
     * @return 1 if successfully fed, 2 if refused, 0 if not handled (hamster doesn't want it).
     */
    public static int tryFeeding(HamsterEntity hamster, Player player, ItemStack stack) {
        Level world = hamster.level();
        AhpMainConfig baseConfig = AdorableHamsterPets.MAIN_CONFIG;
        AhpItemConfig itemConfig = AdorableHamsterPets.ITEM_CONFIG;

        // --- 1. Process Pouch Unlock ---
        if (ConfigDataCache.isPouchUnlockFood(stack) && !hamster.isCheekPouchUnlocked()) {
            if (!world.isClientSide()) {
                hamster.setCheekPouchUnlocked(true);

                if (player instanceof ServerPlayer serverPlayer) {
                    ModCriteria.CHEEK_POUCH_UNLOCKED.get().trigger(serverPlayer, hamster);
                }

                // Feedback
                world.playSound(null, hamster.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.5f, 1.5f);
                ParticleEffectsUtil.spawnParticles(
                        world,
                        new Vec3(hamster.getX(), hamster.getY(0.2D), hamster.getZ()),
                        new ItemParticleOption(ParticleTypes.ITEM, stack.copy()),
                        25,
                        new Vec3(0.25, 0.15, 0.25),
                        0.0
                );
            }
            return 1;
        }

        // --- 2. Process Buff Food ---
        if (ConfigDataCache.isBuffFood(stack)) {
            long currentTime = world.getGameTime();

            // Reject if still on cooldown
            if (hamster.getGreenBeanBuffEndTick() > currentTime) {
                if (!world.isClientSide()) {
                    long totalSeconds = (hamster.getGreenBeanBuffEndTick() - currentTime) / 20;
                    player.displayClientMessage(Component.translatable("message.adorablehamsterpets.beans_cooldown", totalSeconds / 60, totalSeconds % 60).withStyle(ChatFormatting.RED), true);
                }
                return 2;
            }

            if (!world.isClientSide()) {
                // Apply config buffs
                int duration = itemConfig.greenBeanBuffDuration.get();
                hamster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, itemConfig.greenBeanBuffAmplifierSpeed.get()));
                hamster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, itemConfig.greenBeanBuffAmplifierStrength.get()));
                hamster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, itemConfig.greenBeanBuffAmplifierAbsorption.get()));
                hamster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, itemConfig.greenBeanBuffAmplifierRegen.get()));

                // Init zoomies behavior
                hamster.enableZoomies(player);

                // Feedback
                world.playSound(null, hamster.blockPosition(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, hamster.getRandom()), SoundSource.NEUTRAL, 1.0F, 1.0F);

                // Update state trackers
                hamster.getEntityData().set(HamsterEntity.GREEN_BEAN_BUFF_DURATION, currentTime + duration);
                hamster.setGreenBeanBuffEndTick(currentTime + baseConfig.steamedGreenBeansBuffCooldown.get());

                // Trigger Advancement
                if (player instanceof ServerPlayer serverPlayer) {
                    ModCriteria.FED_HAMSTER_STEAMED_BEANS.get().trigger(serverPlayer, hamster);
                }
            }
            return 1;
        }

        // --- 3. Process Standard Food ---
        if (ConfigDataCache.isStandardFood(stack) || ConfigDataCache.isTamingFood(stack)) {
            boolean consumed = false;

            // Heal if injured
            if (hamster.getHealth() < hamster.getMaxHealth()) {
                if (!world.isClientSide()) {
                    hamster.heal(itemConfig.standardFoodHealing.get());
                }
                consumed = true;
            }

            // Grow if baby
            if (hamster.isBaby()) {
                if (!world.isClientSide()) {
                    // Grow by ~10% of remaining time, but at least 1 minute per feed
                    int remainingTicks = -hamster.getAge();
                    int ticksToGrow = Math.max(1200, (int) (remainingTicks * 0.1F));

                    // Convert to seconds since vanilla's growUp() multiplies by 20
                    int secondsToGrow = ticksToGrow / 20;

                    // Track state before growth
                    boolean wasBaby = hamster.isBaby();

                    // Prevent vanilla from storing 'forcedAge' penalty
                    hamster.ageUp(secondsToGrow, false);

                    // If just reached adulthood, apply configured cooldown
                    if (wasBaby && !hamster.isBaby()) {
                        hamster.setAge(baseConfig.breedingCooldownSeconds.get() * 20);
                    }
                }
                consumed = true;
            }

            if (consumed) {
                if (!world.isClientSide()) {
                    // Feedback
                    world.playSound(null, hamster.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.5f, 1.2f + (hamster.getRandom().nextFloat() - 0.5f) * 0.2f);
                }
                return 1;
            }

            // Enter love mode if healthy, adult, and ready
            if (hamster.getAge() == 0 && !hamster.isInCustomLove()) {
                // Evaluate breeding permissions
                boolean isBreedingAllowed = baseConfig.enableBreeding;
                if (!isBreedingAllowed && baseConfig.allowedBreeders.contains(player.getGameProfile().getName())) {
                    isBreedingAllowed = true;
                }

                if (isBreedingAllowed && hamster.timesBred < baseConfig.maxLittersPerHamster.get()) {

                    if (!world.isClientSide()) {
                        // --- Player Litter Limit Check ---
                        if (player instanceof PlayerEntityAccessor accessor) {
                            if (!accessor.ahp$canBreedHamsters()) {
                                player.displayClientMessage(Component.translatable("message.adorablehamsterpets.breeding.player_limit_reached").withStyle(ChatFormatting.RED), true);
                                hamster.playRefusalAnimation();
                                return 2;
                            }
                            accessor.ahp$incrementHamstersFedForBreeding();
                        }

                        hamster.setSitting(false, true);
                        hamster.setCustomInLove(player);
                        hamster.setInLove(true);
                    }
                    return 1;
                } else if (!isBreedingAllowed) {
                    if (!world.isClientSide()) {
                        player.displayClientMessage(Component.translatable("message.adorablehamsterpets.breeding.disabled").withStyle(ChatFormatting.RED), true);
                        hamster.playRefusalAnimation();
                    }
                    return 2;
                } else {
                    if (!world.isClientSide()) {
                        player.displayClientMessage(Component.translatable("message.adorablehamsterpets.breeding.hamster_limit_reached").withStyle(ChatFormatting.RED), true);
                        hamster.playRefusalAnimation();
                    }
                    return 2;
                }
            }

            // Reduce throw cooldown if active
            if (hamster.getHamsterFlag(HamsterEntity.THROW_COOLDOWN_FLAG)) {
                if (!world.isClientSide()) {
                    long currentTime = world.getGameTime();
                    long reduction = Math.max(100L, (long) (baseConfig.hamsterThrowCooldown.get() * 0.15F));
                    hamster.throwCooldownEndTick -= reduction;

                    if (hamster.throwCooldownEndTick <= currentTime) {
                        hamster.throwCooldownEndTick = 0L;
                        hamster.setHamsterFlag(HamsterEntity.THROW_COOLDOWN_FLAG, false); // Update flag for client sync
                        player.displayClientMessage(Component.translatable("message.adorablehamsterpets.throw_cooldown_reset").withStyle(ChatFormatting.WHITE), true);
                    } else {
                        long remainingTicks = hamster.throwCooldownEndTick - currentTime;
                        long totalSecondsRemaining = Math.max(1, remainingTicks / 20);
                        player.displayClientMessage(Component.translatable("message.adorablehamsterpets.throw_cooldown_decrement", totalSecondsRemaining).withStyle(ChatFormatting.YELLOW), true);
                    }

                    // Feedback
                    world.playSound(null, hamster.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.5f, 1.2f + (hamster.getRandom().nextFloat() - 0.5f) * 0.2f);
                }
                return 1;
            }
        }

        return 0; // Not interested
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Attempts to toggle the hamster's aggression state based on the provided item.
     */
    public static AggressionToggleResult tryAggressionToggle(
        HamsterEntity hamster, Player player, ItemStack stack) {
        if (ConfigDataCache.isPacifistItem(stack)) {
            HamsterEntity.AggressionState currentState = hamster.getAggressionState();
            if (currentState == HamsterEntity.AggressionState.STANDARD) {
                if (hamster.level().isClientSide()) {
                    return AggressionToggleResult.ACCEPTED_WITH_CONSUMPTION;
                }
                if (HamsterCombatUtil.deescalateStandardCombat(hamster)) {
                    applyDeescalationFeedback(hamster);
                    return AggressionToggleResult.ACCEPTED_WITH_CONSUMPTION;
                }
            }
            HamsterInteractionGestureUtil.PacifistItemAction action =
                    HamsterInteractionGestureUtil.resolvePacifistItemAction(
                            currentState == HamsterEntity.AggressionState.PACIFIST,
                            currentState == HamsterEntity.AggressionState.MENACE
                                    && hamster.getTarget() != null);
            return switch (action) {
                case FALL_THROUGH -> AggressionToggleResult.NOT_HANDLED;
                case END_FIGHT_IN_STANDARD ->
                        trySetAggressionState(
                                hamster, player, HamsterEntity.AggressionState.STANDARD);
                case ENABLE_PACIFIST ->
                        trySetAggressionState(
                                hamster, player, HamsterEntity.AggressionState.PACIFIST);
            };
        } else if (ConfigDataCache.isStandardAggressionItem(stack)) {
            return trySetAggressionState(hamster, player, HamsterEntity.AggressionState.STANDARD);
        } else if (ConfigDataCache.isMenaceItem(stack)) {
            return trySetAggressionState(hamster, player, HamsterEntity.AggressionState.MENACE);
        }
        return AggressionToggleResult.NOT_HANDLED;
    }

    private static AggressionToggleResult trySetAggressionState(
            HamsterEntity hamster,
            Player player,
            HamsterEntity.AggressionState targetState) {
        if (hamster.getAggressionState() == targetState) {
            return AggressionToggleResult.NOT_HANDLED;
        }

        if (!hamster.level().isClientSide()) {
            hamster.setAggressionState(targetState);
            applyAggressionStateFeedback(hamster, player, targetState);

            if (targetState == HamsterEntity.AggressionState.MENACE) {
                applyMenaceFeedback(hamster, player);
            } else {
                // Clear active target if switching out of menace or into pacifist
                hamster.setTarget(null);
            }
        }
        return AggressionToggleResult.ACCEPTED_WITH_CONSUMPTION;
    }

    private static void applyAggressionStateFeedback(
            HamsterEntity hamster,
            Player player,
            HamsterEntity.AggressionState targetState) {
        boolean isMenace = targetState == HamsterEntity.AggressionState.MENACE;
        SoundEvent sound = isMenace
                ? SoundEvents.ENDER_DRAGON_GROWL
                : getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
        float volume = isMenace ? 0.15F : 1.0F;
        float pitch = isMenace ? 5.0F : 1.0F;
        hamster.level()
                .playSound(
                        null,
                        hamster.blockPosition(),
                        sound,
                        SoundSource.NEUTRAL,
                        volume,
                        pitch);

        ParticleOptions particle =
                isMenace ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.HAPPY_VILLAGER;
        ParticleEffectsUtil.spawnParticlesOnEntity(hamster, particle, 5, 0.5, 0.5, 0.0, 0.2);

        String messageKey = switch (targetState) {
            case PACIFIST -> "message.adorablehamsterpets.aggression.pacifist";
            case MENACE -> "message.adorablehamsterpets.aggression.menace";
            default -> "message.adorablehamsterpets.aggression.standard";
        };
        player.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.WHITE), true);
    }

    private static void applyDeescalationFeedback(HamsterEntity hamster) {
        SoundEvent sound =
                getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
        hamster.level()
                .playSound(
                        null,
                        hamster.blockPosition(),
                        sound,
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F);
        ParticleEffectsUtil.spawnParticlesOnEntity(
                hamster, ParticleTypes.HAPPY_VILLAGER, 5, 0.5, 0.5, 0.0, 0.2);
    }

    private static void applyMenaceFeedback(HamsterEntity hamster, Player player) {
        hamster.triggerAnimOnServer("mainController", "attack");
        DamageSource damageSource = hamster.damageSources().mobAttack(hamster);
        float damageAmount =
                (float) hamster.getAttributeValue(Attributes.ATTACK_DAMAGE);
        player.hurt(damageSource, damageAmount);
        if (player instanceof ServerPlayer serverPlayer) {
            MiscUtil.PlayerPhysicsUtil.applyKnockback(serverPlayer, hamster.position());
        }

        SoundEvent attackSound =
                ModSounds.getRandomSoundFrom(
                        ModSounds.HAMSTER_ATTACK_SOUNDS, hamster.getRandom());
        if (attackSound != null) {
            hamster.playSound(attackSound, 1.0F, hamster.getVoicePitch());
        }
    }

    public enum AggressionToggleResult {
        NOT_HANDLED(false, false),
        ACCEPTED_WITH_CONSUMPTION(true, true);

        private final boolean accepted;
        private final boolean consumesItem;

        AggressionToggleResult(boolean accepted, boolean consumesItem) {
            this.accepted = accepted;
            this.consumesItem = consumesItem;
        }

        public boolean isAccepted() {
            return this.accepted;
        }

        public boolean consumesItem() {
            return this.consumesItem;
        }
    }
}
