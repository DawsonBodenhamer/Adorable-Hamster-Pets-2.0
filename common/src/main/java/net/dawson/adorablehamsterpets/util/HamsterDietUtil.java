package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
    public static boolean checkAndHandleRefusal(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        if (ConfigDataCache.isRepeatableFood(stack)) return false;

        // Bypass for babies if config enabled
        if (hamster.isBaby() && AdorableHamsterPets.CONFIG.disableBabyFoodRefusal) return false;

        ItemStack lastFood = hamster.getLastFoodItem();
        if (lastFood != null && !lastFood.isEmpty() && ItemStack.areItemsEqual(lastFood, stack)) {
            if (!hamster.getWorld().isClient()) {
                hamster.setRefusingFood(true);
                hamster.setRefuseTimer(REFUSE_FOOD_TIMER_TICKS);
                player.sendMessage(Text.translatable("message.adorablehamsterpets.food_refusal"), true);

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
    public static int tryFeeding(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        World world = hamster.getWorld();
        AhpConfig config = AdorableHamsterPets.CONFIG;

        // --- 1. Process Pouch Unlock ---
        if (ConfigDataCache.isPouchUnlockFood(stack) && !hamster.isCheekPouchUnlocked()) {
            if (!world.isClient()) {
                hamster.setCheekPouchUnlocked(true);

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ModCriteria.CHEEK_POUCH_UNLOCKED.trigger(serverPlayer, hamster);
                }

                // Feedback
                world.playSound(null, hamster.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 0.5f, 1.5f);
                ParticleEffectsUtil.spawnParticles(
                        world,
                        new Vec3d(hamster.getX(), hamster.getBodyY(0.2D), hamster.getZ()),
                        new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                        25,
                        new Vec3d(0.25, 0.15, 0.25),
                        0.0
                );
            }
            return 1;
        }

        // --- 2. Process Buff Food ---
        if (ConfigDataCache.isBuffFood(stack)) {
            long currentTime = world.getTime();

            // Reject if still on cooldown
            if (hamster.getGreenBeanBuffEndTick() > currentTime) {
                if (!world.isClient()) {
                    long totalSeconds = (hamster.getGreenBeanBuffEndTick() - currentTime) / 20;
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.beans_cooldown", totalSeconds / 60, totalSeconds % 60).formatted(Formatting.RED), true);
                }
                return 2;
            }

            if (!world.isClient()) {
                // Apply config buffs
                int duration = config.greenBeanBuffDuration.get();
                hamster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, config.greenBeanBuffAmplifierSpeed.get()));
                hamster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, config.greenBeanBuffAmplifierStrength.get()));
                hamster.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, config.greenBeanBuffAmplifierAbsorption.get()));
                hamster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, config.greenBeanBuffAmplifierRegen.get()));

                // Init zoomies behavior
                hamster.enableZoomies(player);

                // Feedback
                world.playSound(null, hamster.getBlockPos(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, hamster.getRandom()), SoundCategory.NEUTRAL, 1.0F, 1.0F);

                // Update state trackers
                hamster.getDataTracker().set(HamsterEntity.GREEN_BEAN_BUFF_DURATION, currentTime + duration);
                hamster.setGreenBeanBuffEndTick(currentTime + config.steamedGreenBeansBuffCooldown.get());

                // Trigger Advancement
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ModCriteria.FED_HAMSTER_STEAMED_BEANS.trigger(serverPlayer, hamster);
                }
            }
            return 1;
        }

        // --- 3. Process Aggression State Changes ---
        if (ConfigDataCache.isPacifistItem(stack)) {
            int result = trySetAggressionState(hamster, player, stack, HamsterEntity.AggressionState.PACIFIST);
            if (result > 0) return result;
        } else if (ConfigDataCache.isStandardAggressionItem(stack)) {
            int result = trySetAggressionState(hamster, player, stack, HamsterEntity.AggressionState.STANDARD);
            if (result > 0) return result;
        } else if (ConfigDataCache.isMenaceItem(stack)) {
            int result = trySetAggressionState(hamster, player, stack, HamsterEntity.AggressionState.MENACE);
            if (result > 0) return result;
        }

        // --- 4. Process Standard Food ---
        if (ConfigDataCache.isStandardFood(stack) || ConfigDataCache.isTamingFood(stack)) {
            boolean consumed = false;

            // Heal if injured
            if (hamster.getHealth() < hamster.getMaxHealth()) {
                if (!world.isClient()) {
                    hamster.heal(config.standardFoodHealing.get());
                }
                consumed = true;
            }

            // Grow if baby
            if (hamster.isBaby()) {
                if (!world.isClient()) {
                    // Grow by ~10% of remaining time, but at least 1 minute per feed
                    int remainingTicks = -hamster.getBreedingAge();
                    int ticksToGrow = Math.max(1200, (int) (remainingTicks * 0.1F));

                    // Convert to seconds since vanilla's growUp() multiplies by 20
                    int secondsToGrow = ticksToGrow / 20;

                    // Track state before growth
                    boolean wasBaby = hamster.isBaby();

                    // Prevent vanilla from storing 'forcedAge' penalty
                    hamster.growUp(secondsToGrow, false);

                    // If just reached adulthood, apply configured cooldown
                    if (wasBaby && !hamster.isBaby()) {
                        hamster.setBreedingAge(config.breedingCooldownSeconds.get() * 20);
                    }
                }
                consumed = true;
            }

            if (consumed) {
                if (!world.isClient()) {
                    // Feedback
                    world.playSound(null, hamster.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 0.5f, 1.2f + (hamster.getRandom().nextFloat() - 0.5f) * 0.2f);
                }
                return 1;
            }

            // Enter love mode if healthy, adult, and ready
            if (hamster.getBreedingAge() == 0 && !hamster.isInCustomLove()) {
                // Evaluate breeding permissions
                boolean isBreedingAllowed = config.enableBreeding;
                if (!isBreedingAllowed && config.allowedBreeders.contains(player.getGameProfile().getName())) {
                    isBreedingAllowed = true;
                }

                if (isBreedingAllowed && hamster.timesBred < config.maxLittersPerHamster.get()) {

                    if (!world.isClient()) {
                        // --- Player Litter Limit Check ---
                        if (player instanceof PlayerEntityAccessor accessor) {
                            if (!accessor.ahp$canBreedHamsters()) {
                                player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.player_limit_reached").formatted(Formatting.RED), true);
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
                    if (!world.isClient()) {
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.disabled").formatted(Formatting.RED), true);
                        hamster.playRefusalAnimation();
                    }
                    return 2;
                } else {
                    if (!world.isClient()) {
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.hamster_limit_reached").formatted(Formatting.RED), true);
                        hamster.playRefusalAnimation();
                    }
                    return 2;
                }
            }

            // Reduce throw cooldown if active
            if (hamster.getHamsterFlag(HamsterEntity.THROW_COOLDOWN_FLAG)) {
                if (!world.isClient()) {
                    long currentTime = world.getTime();
                    long reduction = Math.max(100L, (long) (config.hamsterThrowCooldown.get() * 0.15F));
                    hamster.throwCooldownEndTick -= reduction;

                    if (hamster.throwCooldownEndTick <= currentTime) {
                        hamster.throwCooldownEndTick = 0L;
                        hamster.setHamsterFlag(HamsterEntity.THROW_COOLDOWN_FLAG, false); // Update flag for client sync
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.throw_cooldown_reset").formatted(Formatting.WHITE), true);
                    } else {
                        long remainingTicks = hamster.throwCooldownEndTick - currentTime;
                        long totalSecondsRemaining = Math.max(1, remainingTicks / 20);
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.throw_cooldown_decrement", totalSecondsRemaining).formatted(Formatting.YELLOW), true);
                    }

                    // Feedback
                    world.playSound(null, hamster.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 0.5f, 1.2f + (hamster.getRandom().nextFloat() - 0.5f) * 0.2f);
                }
                return 1;
            }
        }

        return 0; // Not interested
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static int trySetAggressionState(HamsterEntity hamster, PlayerEntity player, ItemStack stack, HamsterEntity.AggressionState targetState) {
        if (hamster.getAggressionState() == targetState) {
            return 0; // Already in this state. Fall through
        }

        if (!hamster.getWorld().isClient()) {
            hamster.setAggressionState(targetState);

            // Audio Feedback
            SoundEvent sound = targetState == HamsterEntity.AggressionState.MENACE ? SoundEvents.ENTITY_ENDER_DRAGON_GROWL : getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
            float volume = targetState == HamsterEntity.AggressionState.MENACE ? 0.15f : 1.0f;
            float pitch = targetState == HamsterEntity.AggressionState.MENACE ? 5.0f : 1.0f;
            hamster.getWorld().playSound(null, hamster.getBlockPos(), sound, SoundCategory.NEUTRAL, volume, pitch);

            // Visual Feedback
            ParticleEffect particle = targetState == HamsterEntity.AggressionState.MENACE ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.HAPPY_VILLAGER;
            ParticleEffectsUtil.spawnParticlesOnEntity(hamster, particle, 5, 0.5, 0.5, 0.0, 0.2);

            // Message Feedback
            String msgKey = switch (targetState) {
                case PACIFIST -> "message.adorablehamsterpets.aggression.pacifist";
                case MENACE -> "message.adorablehamsterpets.aggression.menace";
                default -> "message.adorablehamsterpets.aggression.standard";
            };
            player.sendMessage(Text.translatable(msgKey).formatted(Formatting.WHITE), true);

            if (targetState == HamsterEntity.AggressionState.MENACE) {
                // Aggression feedback
                hamster.triggerAnimOnServer("mainController", "attack");
                DamageSource damageSource = hamster.getDamageSources().mobAttack(hamster);
                float damageAmount = (float) hamster.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                player.damage(damageSource, damageAmount);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    MiscUtil.PlayerPhysicsUtil.applyKnockback(serverPlayer, hamster.getPos());
                }
                SoundEvent attackSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_ATTACK_SOUNDS, hamster.getRandom());
                if (attackSound != null) {
                    hamster.playSound(attackSound, 1.0F, hamster.getSoundPitch());
                }
            } else {
                // Clear active target if switching out of menace or into pacifist
                hamster.setTarget(null);
            }
        }
        return 1;
    }
}