package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class CheeseItem extends ConfigurableFoodItem {

    public CheeseItem(Properties settings) {
        super(settings, Configs.AHP_ITEMS.cheeseNutrition, Configs.AHP_ITEMS.cheeseSaturation, "tooltip.adorablehamsterpets.cheese");
    }

    /**
     * Color the item name gold.
     */
    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.GOLD);
    }

    @Override
    public SoundEvent getEatingSound() {
        return ModSounds.CHEESE_EAT1.get();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 20; // Custom eating time
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (user instanceof Player player) {
            // Manually apply hunger and saturation from config
            int nutrition = Configs.AHP_ITEMS.cheeseNutrition.get();
            float saturation = Configs.AHP_ITEMS.cheeseSaturation.get();
            player.getFoodData().eat(nutrition, saturation);
            player.awardStat(Stats.ITEM_USED.get(this));
            SoundEvent randomEatSound = ModSounds.getRandomSoundFrom(ModSounds.CHEESE_EAT_SOUNDS, world.random);
            if (randomEatSound != null) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), randomEatSound, player.getSoundSource(), 1.2F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
            }
        }
        if (!(user instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }
}