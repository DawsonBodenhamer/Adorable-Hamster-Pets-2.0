package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class CheeseItem extends ConfigurableFoodItem {

    public CheeseItem(Settings settings) {
        super(settings, Configs.AHP.cheeseNutrition, Configs.AHP.cheeseSaturation, 8, 0.8F, "tooltip.adorablehamsterpets.cheese");
    }

    /**
     * Color the item name gold.
     */
    @Override
    public Text getName(ItemStack stack) {
        return super.getName(stack).copy().formatted(Formatting.GOLD);
    }

    @Override
    public SoundEvent getEatSound() {
        return ModSounds.CHEESE_EAT1.get();
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 20; // Custom eating time
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            // Manually apply hunger and saturation from config
            int nutrition = Configs.AHP.cheeseNutrition.get();
            float saturation = Configs.AHP.cheeseSaturation.get();
            player.getHungerManager().add(nutrition, saturation);
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            SoundEvent randomEatSound = ModSounds.getRandomSoundFrom(ModSounds.CHEESE_EAT_SOUNDS, world.random);
            if (randomEatSound != null) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), randomEatSound, player.getSoundCategory(), 1.2F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
            }
        }
        if (!(user instanceof PlayerEntity player) || !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return stack;
    }
}