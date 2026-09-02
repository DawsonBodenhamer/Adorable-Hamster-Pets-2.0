package net.dawson.adorablehamsterpets.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {

    public static final DeferredRegister<CreativeModeTab> ITEM_GROUPS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> ADORABLE_HAMSTER_PETS_GROUP = ITEM_GROUPS.register(
            "adorable_hamster_pets",
            () -> CreativeTabRegistry.create(builder -> builder
                    .title(Component.translatable("itemgroup.adorablehamsterpets.main"))
                    .icon(() -> new ItemStack(ModItems.HAMSTER_SPAWN_EGG.get()))
                    .displayItems((featureSet, output) -> {
                        output.accept(ModItems.HAMSTER_GUIDE_BOOK.get());
                        output.accept(ModItems.MUSIC_DISC_CHEESE.get());
                        output.accept(ModItems.MUSIC_DISC_BLUE_CHEESE.get());
                        output.accept(ModItems.MUSIC_DISC_PARMESAN.get());
                        output.accept(ModItems.CHEESE.get());
                        output.accept(ModItems.HAMSTER_FOOD_MIX.get());
                        output.accept(ModItems.CUCUMBER.get());
                        output.accept(ModItems.CUCUMBER_SEEDS.get());
                        output.accept(ModItems.SLICED_CUCUMBER.get());
                        output.accept(ModItems.GREEN_BEANS.get());
                        output.accept(ModItems.GREEN_BEAN_SEEDS.get());
                        output.accept(ModItems.STEAMED_GREEN_BEANS.get());
                        output.accept(ModItems.SUNFLOWER_SEEDS.get());
                        output.accept(ModItems.HAMSTER_SPAWN_EGG.get());
                        output.accept(ModItems.SUNFLOWER_BLOCK_ITEM.get());
                        output.accept(ModItems.WILD_GREEN_BEAN_BUSH_ITEM.get());
                        output.accept(ModItems.WILD_CUCUMBER_BUSH_ITEM.get());
                        output.accept(ModItems.HAMSTER_BEDDING.get());
                        output.accept(ModItems.ACORN.get());
                        output.accept(ModItems.ACORN_SHARD.get());
                        output.accept(ModItems.ACORN_HAT.get());
                        output.accept(ModItems.ACORN_RING.get());
                        output.accept(ModItems.HAMSTER_ARMOR_ACORN.get());
                        output.accept(ModItems.HAMSTER_ARMOR_IRON.get());
                        output.accept(ModItems.HAMSTER_ARMOR_GOLD.get());
                        output.accept(ModItems.HAMSTER_ARMOR_DIAMOND.get());
                        output.accept(ModItems.HAMSTER_ARMOR_NETHERITE.get());
                        output.accept(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get());
                        output.accept(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get());
                        output.accept(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get());
                        output.accept(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get());
                        ModItems.HAMSTER_BED_ITEMS.values().forEach(supplier -> output.accept(supplier.get()));
                        output.accept(ModItems.ACORN_CRATE.get());
                        output.accept(ModItems.CUCUMBER_CRATE.get());
                        output.accept(ModItems.GREEN_BEANS_CRATE.get());
                        output.accept(ModItems.HAMSTER_FOOD_MIX_CRATE.get());
                    }))
    );

    public static void register() {
        ITEM_GROUPS.register();
    }
}
