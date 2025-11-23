package net.dawson.adorablehamsterpets.item.custom;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CheeseItem extends Item {

    public CheeseItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (Configs.AHP.enableItemTooltips) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.hint1").formatted(Formatting.GOLD));
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.hint2").formatted(Formatting.GRAY));

            boolean appleSkinLoaded = Platform.isModLoaded("appleskin");
            if (appleSkinLoaded) {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.appleskin_warning").formatted(Formatting.DARK_GRAY));
            } else {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.hint3",
                        Configs.AHP.cheeseNutrition.get(),
                        String.format("%.1f", Configs.AHP.cheeseSaturation.get() * Configs.AHP.cheeseNutrition.get() * 2.0F)
                ).formatted(Formatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, world, tooltip, context);
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