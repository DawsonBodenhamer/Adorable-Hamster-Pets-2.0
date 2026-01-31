package net.dawson.adorablehamsterpets.fabric.datagen;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {

    // --- Custom Tags ---
    public static final TagKey<Item> HAMSTER_ARMOR_ENCHANTABLE = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of(AdorableHamsterPets.MOD_ID, "enchantable/hamster_armor")
    );

    // Frost Walker (Vanilla Foot Armor + Hamster Armor)
    public static final TagKey<Item> FROST_WALKER_SUPPORTED = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of(AdorableHamsterPets.MOD_ID, "enchantable/frost_walker_supported")
    );

    // Fire Protection (Vanilla Armor + Hamster Armor)
    public static final TagKey<Item> FIRE_PROTECTION_SUPPORTED = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of(AdorableHamsterPets.MOD_ID, "enchantable/fire_protection_supported")
    );

    // Soul Speed (Vanilla Foot Armor + Hamster Armor)
    public static final TagKey<Item> SOUL_SPEED_SUPPORTED = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of(AdorableHamsterPets.MOD_ID, "enchantable/soul_speed_supported")
    );

    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        // 1. Define Hamster Armor Group
        getOrCreateTagBuilder(HAMSTER_ARMOR_ENCHANTABLE)
                .add(ModItems.HAMSTER_ARMOR_ACORN.get())
                .add(ModItems.HAMSTER_ARMOR_IRON.get())
                .add(ModItems.HAMSTER_ARMOR_GOLD.get())
                .add(ModItems.HAMSTER_ARMOR_DIAMOND.get())
                .add(ModItems.HAMSTER_ARMOR_NETHERITE.get());

        // 2. Add to Vanilla Durability (Enables Unbreaking/Mending)
        getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .addTag(HAMSTER_ARMOR_ENCHANTABLE);

        // 3. Frost Walker Wrapper
        getOrCreateTagBuilder(FROST_WALKER_SUPPORTED)
                .forceAddTag(ItemTags.FOOT_ARMOR_ENCHANTABLE)
                .addTag(HAMSTER_ARMOR_ENCHANTABLE);

        // 4. Fire Protection Wrapper
        getOrCreateTagBuilder(FIRE_PROTECTION_SUPPORTED)
                .forceAddTag(ItemTags.ARMOR_ENCHANTABLE)
                .addTag(HAMSTER_ARMOR_ENCHANTABLE);

        // 5. Soul Speed Wrapper
        getOrCreateTagBuilder(SOUL_SPEED_SUPPORTED)
                .forceAddTag(ItemTags.FOOT_ARMOR_ENCHANTABLE)
                .addTag(HAMSTER_ARMOR_ENCHANTABLE);
    }
}