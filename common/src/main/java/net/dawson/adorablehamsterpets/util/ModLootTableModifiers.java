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
    // 1.20.1: Use getLootTableId()
    private static final Identifier OAK_LEAVES_ID = Blocks.OAK_LEAVES.getLootTableId();

    // --- Loot Table Categories ---

    // Agriculture & Provisions: Villages, Dungeons, Mineshafts, Shipwrecks, Outposts
    private static final List<Identifier> SEED_LOCATIONS = List.of(
            LootTables.SPAWN_BONUS_CHEST,
            LootTables.SIMPLE_DUNGEON_CHEST,
            LootTables.ABANDONED_MINESHAFT_CHEST,
            LootTables.VILLAGE_PLAINS_CHEST,
            LootTables.VILLAGE_SAVANNA_HOUSE_CHEST,
            LootTables.VILLAGE_SNOWY_HOUSE_CHEST,
            LootTables.VILLAGE_TAIGA_HOUSE_CHEST,
            LootTables.VILLAGE_DESERT_HOUSE_CHEST,
            LootTables.SHIPWRECK_SUPPLY_CHEST,
            LootTables.PILLAGER_OUTPOST_CHEST,
            LootTables.WOODLAND_MANSION_CHEST
    );

    // Standard Gear: Dungeons, Temples, Villages, Mineshafts
    private static final List<Identifier> STANDARD_ARMOR_LOCATIONS = List.of(
            LootTables.SIMPLE_DUNGEON_CHEST,
            LootTables.ABANDONED_MINESHAFT_CHEST,
            LootTables.DESERT_PYRAMID_CHEST,
            LootTables.JUNGLE_TEMPLE_CHEST,
            LootTables.VILLAGE_ARMORER_CHEST,
            LootTables.VILLAGE_WEAPONSMITH_CHEST,
            LootTables.VILLAGE_TOOLSMITH_CHEST,
            LootTables.IGLOO_CHEST_CHEST
    );

    // High-End Gear: Nether, End, Strongholds, Ancient Cities
    private static final List<Identifier> HIGH_TIER_ARMOR_LOCATIONS = List.of(
            LootTables.NETHER_BRIDGE_CHEST,
            LootTables.BASTION_TREASURE_CHEST,
            LootTables.BASTION_OTHER_CHEST,
            LootTables.BASTION_BRIDGE_CHEST,
            LootTables.BASTION_HOGLIN_STABLE_CHEST,
            LootTables.END_CITY_TREASURE_CHEST,
            LootTables.STRONGHOLD_CROSSING_CHEST,
            LootTables.STRONGHOLD_CORRIDOR_CHEST,
            LootTables.ANCIENT_CITY_CHEST
    );

    // Legendary Artifacts: The rarest locations
    // 1.20.1 Fix: Removed Trial Chambers
    private static final List<Identifier> LEGENDARY_LOCATIONS = List.of(
            LootTables.ANCIENT_CITY_CHEST,
            LootTables.WOODLAND_MANSION_CHEST,
            LootTables.STRONGHOLD_LIBRARY_CHEST,
            LootTables.END_CITY_TREASURE_CHEST,
            LootTables.BURIED_TREASURE_CHEST
    );

    // Smithing Template Specifics
    private static final Identifier MINESHAFT_ID = LootTables.ABANDONED_MINESHAFT_CHEST;
    private static final Identifier DUNGEON_ID = LootTables.SIMPLE_DUNGEON_CHEST;
    private static final Identifier DESERT_PYRAMID_ID = LootTables.DESERT_PYRAMID_CHEST;
    private static final Identifier NETHER_BRIDGE_ID = LootTables.NETHER_BRIDGE_CHEST;
    private static final Identifier STRONGHOLD_LIBRARY_ID = LootTables.STRONGHOLD_LIBRARY_CHEST;
    private static final Identifier ANCIENT_CITY_ID = LootTables.ANCIENT_CITY_CHEST;
    private static final Identifier END_CITY_ID = LootTables.END_CITY_TREASURE_CHEST;
    private static final Identifier BASTION_TREASURE_ID = LootTables.BASTION_TREASURE_CHEST;

    public static void init() {
        // 1.20.1: Register takes 4 arguments: (resourceManager, tableId, builder, getter)
        // We name the third arg 'context' to match your existing logic (it is the LootTable.Builder)
        LootEvent.MODIFY_LOOT_TABLE.register((resourceManager, tableId, context, builtin) -> {

            // --- 1. Acorns from Oak Leaves ---
            if (OAK_LEAVES_ID.equals(tableId)) {
                // 1.20.1: Use .pool() instead of .addPool() on the Builder
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