package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
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
     * Checks if hamster should refuse being fed same item twice consecutively
     * Triggers refusal animation and chat feedback if so
     */
    public static boolean checkAndHandleRefusal(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        if (ConfigDataCache.isRepeatableFood(stack)) return false;

        ItemStack lastFood = hamster.getLastFoodItem();
        if (lastFood != null && !lastFood.isEmpty() && ItemStack.areItemsEqual(lastFood, stack)) {
            hamster.setRefusingFood(true);
            hamster.setRefuseTimer(REFUSE_FOOD_TIMER_TICKS);
            player.sendMessage(Text.translatable("message.adorablehamsterpets.food_refusal"), true);

            // Trigger refusal animation based on movement state
            hamster.playRefusalAnimation();

            return true;
        }
        return false;
    }

    /**
     * Processes feeding logic for pouch unlocking, buff application, healing, and breeding
     * Evaluates in strict priority order
     */
    public static boolean tryFeeding(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        World world = hamster.getWorld();
        AhpConfig config = AdorableHamsterPets.CONFIG;

        // --- 1. Process Pouch Unlock ---
        if (ConfigDataCache.isPouchUnlockFood(stack) && !hamster.isCheekPouchUnlocked()) {
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

            return true;
        }

        // --- 2. Process Buff Food ---
        if (ConfigDataCache.isBuffFood(stack)) {
            long currentTime = world.getTime();

            // Reject if still on cooldown
            if (hamster.getGreenBeanBuffEndTick() > currentTime) {
                long totalSeconds = (hamster.getGreenBeanBuffEndTick() - currentTime) / 20;
                player.sendMessage(Text.translatable("message.adorablehamsterpets.beans_cooldown", totalSeconds / 60, totalSeconds % 60).formatted(Formatting.RED), true);
                return false;
            }

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
            return true;
        }

        // --- 3. Process Standard Food ---
        if (ConfigDataCache.isStandardFood(stack)) {
            // Heal if injured
            if (hamster.getHealth() < hamster.getMaxHealth()) {
                hamster.heal(config.standardFoodHealing.get());
                return true;
            }

            // Enter love mode if healthy and ready
            if (hamster.getBreedingAge() == 0 && !hamster.isInCustomLove()) {
                hamster.setSitting(false, true);
                hamster.setCustomInLove(player);
                hamster.setInLove(true);
                return true;
            }
        }

        return false;
    }
}