package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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

        // --- 3. Process Standard Food ---
        if (ConfigDataCache.isStandardFood(stack)) {
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
        }

        return 0; // Not interested
    }
}