package net.dawson.adorablehamsterpets.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class ModItemGroups {

    public static final DeferredRegister<ItemGroup> ITEM_GROUPS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.ITEM_GROUP);

    public static final RegistrySupplier<ItemGroup> ADORABLE_HAMSTER_PETS_GROUP = ITEM_GROUPS.register(
            "adorable_hamster_pets",
            () -> CreativeTabRegistry.create(builder -> builder
                    .displayName(Text.translatable("itemgroup.adorablehamsterpets.main"))
                    .icon(() -> new ItemStack(ModItems.HAMSTER_SPAWN_EGG.get()))
                    .entries((featureSet, output) -> {
                        output.add(ModItems.HAMSTER_GUIDE_BOOK.get());
                        output.add(ModItems.CHEESE.get());
                        output.add(ModItems.HAMSTER_FOOD_MIX.get());
                        output.add(ModItems.CUCUMBER.get());
                        output.add(ModItems.CUCUMBER_SEEDS.get());
                        output.add(ModItems.SLICED_CUCUMBER.get());
                        output.add(ModItems.GREEN_BEANS.get());
                        output.add(ModItems.GREEN_BEAN_SEEDS.get());
                        output.add(ModItems.STEAMED_GREEN_BEANS.get());
                        output.add(ModItems.SUNFLOWER_SEEDS.get());
                        output.add(ModItems.HAMSTER_SPAWN_EGG.get());
                        output.add(ModItems.SUNFLOWER_BLOCK_ITEM.get());
                        output.add(ModItems.WILD_GREEN_BEAN_BUSH_ITEM.get());
                        output.add(ModItems.WILD_CUCUMBER_BUSH_ITEM.get());
                        output.add(ModItems.HAMSTER_BEDDING.get());
                        output.add(ModItems.ACORN.get());
                        output.add(ModItems.ACORN_SHARD.get());
                        output.add(ModItems.ACORN_HAT.get());
                        output.add(ModItems.HAMSTER_ARMOR_ACORN.get());
                        output.add(ModItems.HAMSTER_ARMOR_IRON.get());
                        output.add(ModItems.HAMSTER_ARMOR_GOLD.get());
                        output.add(ModItems.HAMSTER_ARMOR_DIAMOND.get());
                        output.add(ModItems.HAMSTER_ARMOR_NETHERITE.get());
                        output.add(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get());
                        output.add(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get());
                        output.add(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get());
                        output.add(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get());
                        ModItems.HAMSTER_BED_ITEMS.values().forEach(supplier -> output.add(supplier.get()));
                    }))
    );

    public static void register() {
        ITEM_GROUPS.register();
    }
}