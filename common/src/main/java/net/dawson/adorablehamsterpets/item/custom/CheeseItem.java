package net.dawson.adorablehamsterpets.item.custom;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

public class CheeseItem extends Item {

    public CheeseItem(Settings settings) {
        super(settings);
    }

    /**
     * Dynamically builds the item's ComponentMap, ensuring the FoodComponent
     * always reflects the current configuration values. This is crucial for compatibility
     * with mods like AppleSkin that read this component for their HUD overlays.
     *
     * @return The updated ComponentMap.
     */
    @Override
    public ComponentMap getComponents() {
        // --- 1. Build the dynamic FoodComponent from config ---
        final FoodComponent dynamicFoodComponent = new FoodComponent.Builder()
                .nutrition(Configs.AHP.cheeseNutrition.get())
                .saturationModifier(Configs.AHP.cheeseSaturation.get())
                .build();

        // --- 2. Create an override map with just our dynamic component ---
        ComponentMap override = ComponentMap.builder()
                .add(DataComponentTypes.FOOD, dynamicFoodComponent)
                .build();

        // --- 3. Layer the override on top of the base components ---
        return ComponentMap.of(super.getComponents(), override);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (Configs.AHP.enableItemTooltips) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.hint1").formatted(Formatting.GOLD));
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.hint2").formatted(Formatting.GRAY));
            // --- Conditionally add the nutrition hint only if AppleSkin is NOT loaded ---
            boolean appleSkinLoaded = Platform.isModLoaded("appleskin");
            if (!appleSkinLoaded) {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.hint3",
                        Configs.AHP.cheeseNutrition.get(),
                        String.format("%.1f", Configs.AHP.cheeseSaturation.get() * Configs.AHP.cheeseNutrition.get() * 2.0F)
                ).formatted(Formatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, context, tooltip, type);
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
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
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

    /**
     * Called every tick for each instance of this item in any inventory.
     * This method proactively synchronizes the ItemStack's FoodComponent with the current config values,
     * ensuring compatibility with client-side HUD mods like AppleSkin.
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        // --- 1. Client-Side Only ---
        // This logic is purely for visual sync, so it only needs to run on the client.
        if (!world.isClient) {
            return;
        }

        // --- 2. Get Current and Expected Food Values ---
        FoodComponent currentFoodComponent = stack.get(DataComponentTypes.FOOD);
        int expectedNutrition = Configs.AHP.cheeseNutrition.get();
        float expectedSaturation = Configs.AHP.cheeseSaturation.get();

        // --- 3. Compare and Update if Necessary ---
        // Check if a food component exists and if its values are outdated.
        if (currentFoodComponent != null &&
                (currentFoodComponent.nutrition() != expectedNutrition ||
                        currentFoodComponent.saturation() != expectedSaturation))
        {
            // The stack's data is out of sync with the config. Rebuild and apply the correct component.
            FoodComponent updatedFoodComponent = new FoodComponent.Builder()
                    .nutrition(expectedNutrition)
                    .saturationModifier(expectedSaturation)
                    .build();
            stack.set(DataComponentTypes.FOOD, updatedFoodComponent);
        }
    }
}