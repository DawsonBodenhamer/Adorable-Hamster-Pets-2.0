package net.dawson.adorablehamsterpets.fabric.datagen;

import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.CucumberCropBlock;
import net.dawson.adorablehamsterpets.block.custom.GreenBeansCropBlock;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {
    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {

        // Keep existing crop loot tables
        LootItemBlockStatePropertyCondition.Builder builder3 = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.GREEN_BEANS_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(GreenBeansCropBlock.AGE, GreenBeansCropBlock.MAX_AGE));
        this.add(ModBlocks.GREEN_BEANS_CROP.get(), this.createCropDrops(ModBlocks.GREEN_BEANS_CROP.get(), ModItems.GREEN_BEANS.get(), ModItems.GREEN_BEAN_SEEDS.get(), builder3));

        LootItemBlockStatePropertyCondition.Builder builder4 = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.CUCUMBER_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CucumberCropBlock.AGE, CucumberCropBlock.MAX_AGE));
        this.add(ModBlocks.CUCUMBER_CROP.get(), this.createCropDrops(ModBlocks.CUCUMBER_CROP.get(), ModItems.CUCUMBER.get(), ModItems.CUCUMBER_SEEDS.get(), builder4));

        // --- Crate Drops ---
        this.dropSelf(ModBlocks.ACORN_CRATE.get());
        this.dropSelf(ModBlocks.CUCUMBER_CRATE.get());
        this.dropSelf(ModBlocks.GREEN_BEANS_CRATE.get());
        this.dropSelf(ModBlocks.HAMSTER_FOOD_MIX_CRATE.get());

        // --- Custom Sunflower Loot Table ---
        // Only drop the item when the LOWER half is broken
        LootItemCondition.Builder conditionBuilder = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.SUNFLOWER_BLOCK.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));

        // Define the drop (sunflower item)
        Item sunflowerItemToDrop = ModBlocks.SUNFLOWER_BLOCK.get().asItem();

        this.add(ModBlocks.SUNFLOWER_BLOCK.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F)) // Always try to roll once
                        .when(conditionBuilder) // Apply the condition (only drop if lower half)
                        .add(this.applyExplosionDecay(ModBlocks.SUNFLOWER_BLOCK.get(), LootItem.lootTableItem(sunflowerItemToDrop))) // Add the item entry
                        .when(ExplosionCondition.survivesExplosion()) // Standard condition for blocks
                )
        );

        // Drop 1-2 seeds when broken
        add(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f)) // Always try to roll once
                        .add(LootItem.lootTableItem(ModItems.GREEN_BEAN_SEEDS.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)))) // Drop 1 to 2 seeds
                        .when(ExplosionCondition.survivesExplosion()) // Only drop if not destroyed by explosion
                )
        );

    }


    // Keep your existing multipleOreDrops helper method if needed elsewhere
    public LootTable.Builder multipleOreDrops(Block drop, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> impl = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(drop, this.applyExplosionDecay(drop, ((LootPoolSingletonContainer.Builder<?>)
                LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops))))
                .apply(ApplyBonusCount.addOreBonusCount(impl.getOrThrow(Enchantments.FORTUNE)))));
    }
}
