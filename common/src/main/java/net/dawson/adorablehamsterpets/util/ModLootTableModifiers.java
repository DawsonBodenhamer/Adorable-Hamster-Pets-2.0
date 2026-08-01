package net.dawson.adorablehamsterpets.util;

import dev.architectury.event.events.common.LootEvent;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpWorldGenConfig;
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

    // Common Loot: Seeds
    // Locations: Basic surface structures, supply chests, low-tier village chests
    private static final List<Identifier> COMMON_LOOT_LOCATIONS = List.of(
            LootTables.SPAWN_BONUS_CHEST.getValue(),
            LootTables.SIMPLE_DUNGEON_CHEST.getValue(),
            LootTables.ABANDONED_MINESHAFT_CHEST.getValue(),
            LootTables.VILLAGE_PLAINS_CHEST.getValue(),
            LootTables.VILLAGE_SAVANNA_HOUSE_CHEST.getValue(),
            LootTables.VILLAGE_SNOWY_HOUSE_CHEST.getValue(),
            LootTables.VILLAGE_TAIGA_HOUSE_CHEST.getValue(),
            LootTables.VILLAGE_DESERT_HOUSE_CHEST.getValue(),
            LootTables.VILLAGE_SHEPARD_CHEST.getValue(),
            LootTables.VILLAGE_BUTCHER_CHEST.getValue(),
            LootTables.SHIPWRECK_SUPPLY_CHEST.getValue(),
            LootTables.PILLAGER_OUTPOST_CHEST.getValue(),
            LootTables.UNDERWATER_RUIN_SMALL_CHEST.getValue(),
            LootTables.TRIAL_CHAMBERS_SUPPLY_CHEST.getValue()
    );

    // Standard Gear: Acorn/Iron/Gold Armor
    // Locations: Slightly better structures, specific village professions
    private static final List<Identifier> UNCOMMON_LOOT_LOCATIONS = List.of(
            LootTables.SIMPLE_DUNGEON_CHEST.getValue(),
            LootTables.ABANDONED_MINESHAFT_CHEST.getValue(),
            LootTables.DESERT_PYRAMID_CHEST.getValue(),
            LootTables.JUNGLE_TEMPLE_CHEST.getValue(),
            LootTables.IGLOO_CHEST_CHEST.getValue(),
            LootTables.RUINED_PORTAL_CHEST.getValue(),
            LootTables.SHIPWRECK_TREASURE_CHEST.getValue(),
            LootTables.UNDERWATER_RUIN_BIG_CHEST.getValue(),
            LootTables.VILLAGE_ARMORER_CHEST.getValue(),
            LootTables.VILLAGE_WEAPONSMITH_CHEST.getValue(),
            LootTables.VILLAGE_TOOLSMITH_CHEST.getValue()
    );

    // High-End Gear: Diamond Armor, Netherite (if enabled), Basic Templates (Iron/Gold)
    // Locations: Nether, End, Strongholds, Major structures
    private static final List<Identifier> HIGH_TIER_LOOT_LOCATIONS = List.of(
            LootTables.NETHER_BRIDGE_CHEST.getValue(),
            LootTables.BASTION_TREASURE_CHEST.getValue(),
            LootTables.BASTION_OTHER_CHEST.getValue(),
            LootTables.BASTION_BRIDGE_CHEST.getValue(),
            LootTables.BASTION_HOGLIN_STABLE_CHEST.getValue(),
            LootTables.END_CITY_TREASURE_CHEST.getValue(),
            LootTables.STRONGHOLD_CROSSING_CHEST.getValue(),
            LootTables.STRONGHOLD_CORRIDOR_CHEST.getValue(),
            LootTables.ANCIENT_CITY_CHEST.getValue(),
            LootTables.TRIAL_CHAMBERS_REWARD_CHEST.getValue(),
            LootTables.TRIAL_CHAMBERS_REWARD_RARE_CHEST.getValue()
    );

    // Legendary Artifacts: Accessories, Advanced Templates (Diamond/Netherite)
    // Locations: Rarest containers in the game
    private static final List<Identifier> LEGENDARY_LOOT_LOCATIONS = List.of(
            LootTables.ANCIENT_CITY_CHEST.getValue(),
            LootTables.WOODLAND_MANSION_CHEST.getValue(),
            LootTables.STRONGHOLD_LIBRARY_CHEST.getValue(),
            LootTables.END_CITY_TREASURE_CHEST.getValue(),
            LootTables.BURIED_TREASURE_CHEST.getValue(),
            LootTables.TRIAL_CHAMBERS_REWARD_UNIQUE_CHEST.getValue(),
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE_CHEST.getValue(),
            LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE_CHEST.getValue()
    );

    public static void init() {
        LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> {
            Identifier tableId = key.getValue();
            final AhpWorldGenConfig config = AdorableHamsterPets.WORLD_GEN_CONFIG;

            // --- 1. Acorns from Oak Leaves ---
            if (OAK_LEAVES_ID.equals(tableId)) {
                float chance = config.oakLeavesAcornDropChance.get();
                if (chance > 0) {
                    context.addPool(LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(chance))
                            .with(ItemEntry.builder(ModItems.ACORN.get())));
                }
            }

            // --- 2. Seeds (Common) ---
            if (COMMON_LOOT_LOCATIONS.contains(tableId)) {
                float chance = config.seedLootChance.get();
                if (chance > 0) {
                    context.addPool(LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(chance)) // 60% chance to find a seed stash
                            .with(ItemEntry.builder(ModItems.GREEN_BEAN_SEEDS.get()).weight(1))
                            .with(ItemEntry.builder(ModItems.CUCUMBER_SEEDS.get()).weight(1))
                            .with(ItemEntry.builder(ModItems.SUNFLOWER_SEEDS.get()).weight(1))
                            .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 3.0f))) // 1-3 seeds
                    );
                }
            }

            // --- 3. Standard Armor (Uncommon) ---
            if (UNCOMMON_LOOT_LOCATIONS.contains(tableId)) {
                float chance = config.standardArmorLootChance.get();
                if (chance > 0) {
                    context.addPool(LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(chance)) // 30% Chance
                            .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_ACORN.get()).weight(3)) // Common
                            .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_IRON.get()).weight(2))  // Uncommon
                            .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_GOLD.get()).weight(1))  // Rare
                    );
                }

                float acornRingChance = config.acornRingLootChance.get();
                if (acornRingChance > 0) {
                    context.addPool(LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(acornRingChance))
                            .with(ItemEntry.builder(ModItems.ACORN_RING.get()))
                    );
                }
            }

            // --- 4. High-Tier Armor (Diamond/Netherite) ---
            if (HIGH_TIER_LOOT_LOCATIONS.contains(tableId)) {
                // Diamond
                float diamondChance = config.highTierArmorLootChance.get();
                if (diamondChance > 0) {
                    context.addPool(LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(diamondChance)) // 20% chance
                            .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_DIAMOND.get()))
                    );
                }

                // Netherite (Configurable)
                if (config.enableNetheriteArmorLoot.get()) {
                    float netheriteChance = config.netheriteArmorLootChance.get();
                    if (netheriteChance > 0) {
                        context.addPool(LootPool.builder()
                                .rolls(ConstantLootNumberProvider.create(1))
                                .conditionally(RandomChanceLootCondition.builder(netheriteChance)) // 10% chance
                                .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_NETHERITE.get()))
                        );
                    }
                }
            }

            // --- 5. Accessories (Legendary) ---
            if (LEGENDARY_LOOT_LOCATIONS.contains(tableId)) {
                float chance = config.accessoryLootChance.get();
                if (chance > 0) {
                    context.addPool(LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(chance)) // 2.5% Chance
                            .with(ItemEntry.builder(ModItems.ACORN_HAT.get()))
                    );
                }
            }

            // --- 6. Basic Smithing Templates (Iron/Gold) -> High Tier ---
            if (HIGH_TIER_LOOT_LOCATIONS.contains(tableId)) {
                float basicChance = config.basicSmithingTemplateLootChance.get();
                if (basicChance > 0) {
                    context.addPool(LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(basicChance)) // 20% chance
                            .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get()).weight(1))
                            .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get()).weight(1))
                    );
                }
            }

            // --- 7. Advanced Smithing Templates (Diamond/Netherite) -> Legendary ---
            if (LEGENDARY_LOOT_LOCATIONS.contains(tableId)) {
                float advancedChance = config.advancedSmithingTemplateLootChance.get();
                if (advancedChance > 0) {
                    context.addPool(LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .conditionally(RandomChanceLootCondition.builder(advancedChance)) // 10% chance
                            .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get()).weight(1))
                            .with(ItemEntry.builder(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get()).weight(1))
                    );
                }
            }
        });
    }
}
