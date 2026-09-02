package net.dawson.adorablehamsterpets.util;

import dev.architectury.event.events.common.LootEvent;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpWorldGenConfig;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import java.util.List;

public class ModLootTableModifiers {
    private static final Identifier OAK_LEAVES_ID = Blocks.OAK_LEAVES.getLootTable().orElseThrow().identifier();

    // --- Loot Table Categories ---

    // Common Loot: Seeds
    // Locations: Basic surface structures, supply chests, low-tier village chests
    private static final List<Identifier> COMMON_LOOT_LOCATIONS = List.of(
            BuiltInLootTables.SPAWN_BONUS_CHEST.identifier(),
            BuiltInLootTables.SIMPLE_DUNGEON.identifier(),
            BuiltInLootTables.ABANDONED_MINESHAFT.identifier(),
            BuiltInLootTables.VILLAGE_PLAINS_HOUSE.identifier(),
            BuiltInLootTables.VILLAGE_SAVANNA_HOUSE.identifier(),
            BuiltInLootTables.VILLAGE_SNOWY_HOUSE.identifier(),
            BuiltInLootTables.VILLAGE_TAIGA_HOUSE.identifier(),
            BuiltInLootTables.VILLAGE_DESERT_HOUSE.identifier(),
            BuiltInLootTables.VILLAGE_SHEPHERD.identifier(),
            BuiltInLootTables.VILLAGE_BUTCHER.identifier(),
            BuiltInLootTables.SHIPWRECK_SUPPLY.identifier(),
            BuiltInLootTables.PILLAGER_OUTPOST.identifier(),
            BuiltInLootTables.UNDERWATER_RUIN_SMALL.identifier(),
            BuiltInLootTables.TRIAL_CHAMBERS_SUPPLY.identifier()
    );

    // Standard Gear: Acorn/Iron/Gold Armor
    // Locations: Slightly better structures, specific village professions
    private static final List<Identifier> UNCOMMON_LOOT_LOCATIONS = List.of(
            BuiltInLootTables.SIMPLE_DUNGEON.identifier(),
            BuiltInLootTables.ABANDONED_MINESHAFT.identifier(),
            BuiltInLootTables.DESERT_PYRAMID.identifier(),
            BuiltInLootTables.JUNGLE_TEMPLE.identifier(),
            BuiltInLootTables.IGLOO_CHEST.identifier(),
            BuiltInLootTables.RUINED_PORTAL.identifier(),
            BuiltInLootTables.SHIPWRECK_TREASURE.identifier(),
            BuiltInLootTables.UNDERWATER_RUIN_BIG.identifier(),
            BuiltInLootTables.VILLAGE_ARMORER.identifier(),
            BuiltInLootTables.VILLAGE_WEAPONSMITH.identifier(),
            BuiltInLootTables.VILLAGE_TOOLSMITH.identifier()
    );

    // High-End Gear: Diamond Armor, Netherite (if enabled), Basic Templates (Iron/Gold)
    // Locations: Nether, End, Strongholds, Major structures
    private static final List<Identifier> HIGH_TIER_LOOT_LOCATIONS = List.of(
            BuiltInLootTables.NETHER_BRIDGE.identifier(),
            BuiltInLootTables.BASTION_TREASURE.identifier(),
            BuiltInLootTables.BASTION_OTHER.identifier(),
            BuiltInLootTables.BASTION_BRIDGE.identifier(),
            BuiltInLootTables.BASTION_HOGLIN_STABLE.identifier(),
            BuiltInLootTables.END_CITY_TREASURE.identifier(),
            BuiltInLootTables.STRONGHOLD_CROSSING.identifier(),
            BuiltInLootTables.STRONGHOLD_CORRIDOR.identifier(),
            BuiltInLootTables.ANCIENT_CITY.identifier(),
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD.identifier(),
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE.identifier()
    );

    // Legendary Artifacts: Accessories, Advanced Templates (Diamond/Netherite)
    // Locations: Rarest containers in the game
    private static final List<Identifier> LEGENDARY_LOOT_LOCATIONS = List.of(
            BuiltInLootTables.ANCIENT_CITY.identifier(),
            BuiltInLootTables.WOODLAND_MANSION.identifier(),
            BuiltInLootTables.STRONGHOLD_LIBRARY.identifier(),
            BuiltInLootTables.END_CITY_TREASURE.identifier(),
            BuiltInLootTables.BURIED_TREASURE.identifier(),
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_UNIQUE.identifier(),
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE.identifier(),
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE.identifier()
    );

    public static void init() {
        LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> {
            Identifier tableId = key.identifier();
            final AhpWorldGenConfig config = AdorableHamsterPets.WORLD_GEN_CONFIG;

            // --- 1. Acorns from Oak Leaves ---
            if (OAK_LEAVES_ID.equals(tableId)) {
                float chance = config.oakLeavesAcornDropChance.get();
                if (chance > 0) {
                    context.addPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(chance))
                            .add(LootItem.lootTableItem(ModItems.ACORN.get())));
                }
            }

            // --- 2. Seeds (Common) ---
            if (COMMON_LOOT_LOCATIONS.contains(tableId)) {
                float chance = config.seedLootChance.get();
                if (chance > 0) {
                    context.addPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(chance)) // 60% chance to find a seed stash
                            .add(LootItem.lootTableItem(ModItems.GREEN_BEAN_SEEDS.get()).setWeight(1))
                            .add(LootItem.lootTableItem(ModItems.CUCUMBER_SEEDS.get()).setWeight(1))
                            .add(LootItem.lootTableItem(ModItems.SUNFLOWER_SEEDS.get()).setWeight(1))
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))) // 1-3 seeds
                    );
                }
            }

            // --- 3. Standard Armor (Uncommon) ---
            if (UNCOMMON_LOOT_LOCATIONS.contains(tableId)) {
                float chance = config.standardArmorLootChance.get();
                if (chance > 0) {
                    context.addPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(chance)) // 30% Chance
                            .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_ACORN.get()).setWeight(3)) // Common
                            .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_IRON.get()).setWeight(2))  // Uncommon
                            .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_GOLD.get()).setWeight(1))  // Rare
                    );
                }

                float acornRingChance = config.acornRingLootChance.get();
                if (acornRingChance > 0) {
                    context.addPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(acornRingChance))
                            .add(LootItem.lootTableItem(ModItems.ACORN_RING.get()))
                    );
                }
            }

            // --- 4. High-Tier Armor (Diamond/Netherite) ---
            if (HIGH_TIER_LOOT_LOCATIONS.contains(tableId)) {
                // Diamond
                float diamondChance = config.highTierArmorLootChance.get();
                if (diamondChance > 0) {
                    context.addPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(diamondChance)) // 20% chance
                            .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_DIAMOND.get()))
                    );
                }

                // Netherite (Configurable)
                if (config.enableNetheriteArmorLoot.get()) {
                    float netheriteChance = config.netheriteArmorLootChance.get();
                    if (netheriteChance > 0) {
                        context.addPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1))
                                .when(LootItemRandomChanceCondition.randomChance(netheriteChance)) // 10% chance
                                .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_NETHERITE.get()))
                        );
                    }
                }
            }

            // --- 5. Accessories (Legendary) ---
            if (LEGENDARY_LOOT_LOCATIONS.contains(tableId)) {
                float chance = config.accessoryLootChance.get();
                if (chance > 0) {
                    context.addPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(chance)) // 2.5% Chance
                            .add(LootItem.lootTableItem(ModItems.ACORN_HAT.get()))
                    );
                }
            }

            // --- 6. Basic Smithing Templates (Iron/Gold) -> High Tier ---
            if (HIGH_TIER_LOOT_LOCATIONS.contains(tableId)) {
                float basicChance = config.basicSmithingTemplateLootChance.get();
                if (basicChance > 0) {
                    context.addPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(basicChance)) // 20% chance
                            .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get()).setWeight(1))
                            .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get()).setWeight(1))
                    );
                }
            }

            // --- 7. Advanced Smithing Templates (Diamond/Netherite) -> Legendary ---
            if (LEGENDARY_LOOT_LOCATIONS.contains(tableId)) {
                float advancedChance = config.advancedSmithingTemplateLootChance.get();
                if (advancedChance > 0) {
                    context.addPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(advancedChance)) // 10% chance
                            .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get()).setWeight(1))
                            .add(LootItem.lootTableItem(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get()).setWeight(1))
                    );
                }
            }
        });
    }
}
