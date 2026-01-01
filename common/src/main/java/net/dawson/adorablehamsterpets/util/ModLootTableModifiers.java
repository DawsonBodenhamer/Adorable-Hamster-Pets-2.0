package net.dawson.adorablehamsterpets.util;

import dev.architectury.event.events.common.LootEvent;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModLootTableModifiers {
    private static final Identifier OAK_LEAVES_ID = Blocks.OAK_LEAVES.getLootTableKey().getValue();

    // --- Loot Table Categories ---

    // Agriculture & Provisions: Villages, Dungeons, Mineshafts, Shipwrecks, Outposts
    private static final List<Identifier> SEED_LOCATIONS = List.of(
            LootTables.SPAWN_BONUS_CHEST.getValue(),
            LootTables.SIMPLE_DUNGEON_CHEST.getValue(),
            LootTables.ABANDONED_MINESHAFT_CHEST.getValue(),
            LootTables.VILLAGE_PLAINS_CHEST.getValue(),
            LootTables.VILLAGE_SAVANNA_HOUSE_CHEST.getValue(),
            LootTables.VILLAGE_SNOWY_HOUSE_CHEST.getValue(),
            LootTables.VILLAGE_TAIGA_HOUSE_CHEST.getValue(),
            LootTables.VILLAGE_DESERT_HOUSE_CHEST.getValue(),
            LootTables.SHIPWRECK_SUPPLY_CHEST.getValue(),
            LootTables.PILLAGER_OUTPOST_CHEST.getValue(),
            LootTables.WOODLAND_MANSION_CHEST.getValue()
    );

    // Standard Gear: Dungeons, Temples, Villages, Mineshafts
    private static final List<Identifier> STANDARD_ARMOR_LOCATIONS = List.of(
            LootTables.SIMPLE_DUNGEON_CHEST.getValue(),
            LootTables.ABANDONED_MINESHAFT_CHEST.getValue(),
            LootTables.DESERT_PYRAMID_CHEST.getValue(),
            LootTables.JUNGLE_TEMPLE_CHEST.getValue(),
            LootTables.VILLAGE_ARMORER_CHEST.getValue(),
            LootTables.VILLAGE_WEAPONSMITH_CHEST.getValue(),
            LootTables.VILLAGE_TOOLSMITH_CHEST.getValue(),
            LootTables.IGLOO_CHEST_CHEST.getValue()
    );

    // High-End Gear: Nether, End, Strongholds, Ancient Cities
    private static final List<Identifier> HIGH_TIER_ARMOR_LOCATIONS = List.of(
            LootTables.NETHER_BRIDGE_CHEST.getValue(),
            LootTables.BASTION_TREASURE_CHEST.getValue(),
            LootTables.BASTION_OTHER_CHEST.getValue(),
            LootTables.BASTION_BRIDGE_CHEST.getValue(),
            LootTables.BASTION_HOGLIN_STABLE_CHEST.getValue(),
            LootTables.END_CITY_TREASURE_CHEST.getValue(),
            LootTables.STRONGHOLD_CROSSING_CHEST.getValue(),
            LootTables.STRONGHOLD_CORRIDOR_CHEST.getValue(),
            LootTables.ANCIENT_CITY_CHEST.getValue()
    );

    // Legendary Artifacts: The rarest locations
    private static final List<Identifier> LEGENDARY_LOCATIONS = List.of(
            LootTables.ANCIENT_CITY_CHEST.getValue(),
            LootTables.WOODLAND_MANSION_CHEST.getValue(),
            LootTables.STRONGHOLD_LIBRARY_CHEST.getValue(),
            LootTables.END_CITY_TREASURE_CHEST.getValue(),
            LootTables.BURIED_TREASURE_CHEST.getValue(),
            LootTables.TRIAL_CHAMBERS_REWARD_UNIQUE_CHEST.getValue(),
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE_CHEST.getValue()
    );

    // Smithing Template Specifics
    private static final Identifier MINESHAFT_ID = LootTables.ABANDONED_MINESHAFT_CHEST.getValue();
    private static final Identifier DUNGEON_ID = LootTables.SIMPLE_DUNGEON_CHEST.getValue();
    private static final Identifier DESERT_PYRAMID_ID = LootTables.DESERT_PYRAMID_CHEST.getValue();
    private static final Identifier NETHER_BRIDGE_ID = LootTables.NETHER_BRIDGE_CHEST.getValue();
    private static final Identifier STRONGHOLD_LIBRARY_ID = LootTables.STRONGHOLD_LIBRARY_CHEST.getValue();
    private static final Identifier ANCIENT_CITY_ID = LootTables.ANCIENT_CITY_CHEST.getValue();
    private static final Identifier END_CITY_ID = LootTables.END_CITY_TREASURE_CHEST.getValue();
    private static final Identifier BASTION_TREASURE_ID = LootTables.BASTION_TREASURE_CHEST.getValue();

    public static void init() {
        LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> {
            Identifier tableId = key.getValue();

            // --- 1. Acorns from Oak Leaves ---
            if (OAK_LEAVES_ID.equals(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.0033f))
                        .with(ItemEntry.builder(ModItems.ACORN.get())));
            }

            // --- 2. Seeds (Common, Wheat-like rarity) ---
            if (SEED_LOCATIONS.contains(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.60f)) // 60% chance to find a seed stash
                        .with(ItemEntry.builder(ModItems.GREEN_BEAN_SEEDS.get()).weight(1))
                        .with(ItemEntry.builder(ModItems.CUCUMBER_SEEDS.get()).weight(1))
                        .with(ItemEntry.builder(ModItems.SUNFLOWER_SEEDS.get()).weight(1))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 3.0f))) // 1-3 seeds
                );
            }

            // --- 3. Standard Armor (Acorn/Iron/Gold, Horse Armor rarity) ---
            if (STANDARD_ARMOR_LOCATIONS.contains(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.30f)) // 30% Chance
                        .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_ACORN.get()).weight(3)) // Common
                        .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_IRON.get()).weight(2))  // Uncommon
                        .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_GOLD.get()).weight(1))  // Rare
                );
            }

            // --- 4. High-Tier Armor (Diamond Only) ---
            if (HIGH_TIER_ARMOR_LOCATIONS.contains(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.15f)) // 15% Chance
                        .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_DIAMOND.get()))
                );
            }

            // --- 5. Accessories (God Apple Rarity) ---
            if (LEGENDARY_LOCATIONS.contains(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.025f)) // 2.5% Chance
                        .with(ItemEntry.builder(ModItems.ACORN_HAT.get()))
                );
            }

            // --- 6. Smithing Templates Locations ---

            // Iron: Mineshafts & Dungeons
            if (MINESHAFT_ID.equals(tableId) || DUNGEON_ID.equals(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.2f)) // 20% Chance
                        .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get())));
            }

            // Gold: Desert Pyramids & Nether Fortresses
            if (DESERT_PYRAMID_ID.equals(tableId) || NETHER_BRIDGE_ID.equals(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.2f)) // 20% Chance
                        .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get())));
            }

            // Diamond: Strongholds, Ancient Cities, End Cities
            if (STRONGHOLD_LIBRARY_ID.equals(tableId) || ANCIENT_CITY_ID.equals(tableId) || END_CITY_ID.equals(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.1f)) // 10% Chance
                        .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get())));
            }

            // Netherite: Bastion Treasure
            if (BASTION_TREASURE_ID.equals(tableId)) {
                context.addPool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.1f)) // 10% Chance
                        .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get())));
            }
        });
    }
}