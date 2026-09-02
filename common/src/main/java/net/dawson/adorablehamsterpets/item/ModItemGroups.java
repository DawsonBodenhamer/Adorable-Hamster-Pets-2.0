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

    // 26.2: CreativeModeTab.Output is no longer accessible from mod code, so the
    // tab is created empty and filled through Architectury's append() queue.
    public static final RegistrySupplier<CreativeModeTab> ADORABLE_HAMSTER_PETS_GROUP = ITEM_GROUPS.register(
            "adorable_hamster_pets",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemgroup.adorablehamsterpets.main"),
                    () -> new ItemStack(ModItems.HAMSTER_SPAWN_EGG.get()))
    );

    @SuppressWarnings("unchecked")
    public static void register() {
        ITEM_GROUPS.register();
        CreativeTabRegistry.append(ADORABLE_HAMSTER_PETS_GROUP,
                ModItems.HAMSTER_GUIDE_BOOK,
                ModItems.MUSIC_DISC_CHEESE,
                ModItems.MUSIC_DISC_BLUE_CHEESE,
                ModItems.MUSIC_DISC_PARMESAN,
                ModItems.CHEESE,
                ModItems.HAMSTER_FOOD_MIX,
                ModItems.CUCUMBER,
                ModItems.CUCUMBER_SEEDS,
                ModItems.SLICED_CUCUMBER,
                ModItems.GREEN_BEANS,
                ModItems.GREEN_BEAN_SEEDS,
                ModItems.STEAMED_GREEN_BEANS,
                ModItems.SUNFLOWER_SEEDS,
                ModItems.HAMSTER_SPAWN_EGG,
                ModItems.SUNFLOWER_BLOCK_ITEM,
                ModItems.WILD_GREEN_BEAN_BUSH_ITEM,
                ModItems.WILD_CUCUMBER_BUSH_ITEM,
                ModItems.HAMSTER_BEDDING,
                ModItems.ACORN,
                ModItems.ACORN_SHARD,
                ModItems.ACORN_HAT,
                ModItems.ACORN_RING,
                ModItems.HAMSTER_ARMOR_ACORN,
                ModItems.HAMSTER_ARMOR_IRON,
                ModItems.HAMSTER_ARMOR_GOLD,
                ModItems.HAMSTER_ARMOR_DIAMOND,
                ModItems.HAMSTER_ARMOR_NETHERITE,
                ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON,
                ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD,
                ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND,
                ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE);
        CreativeTabRegistry.append(ADORABLE_HAMSTER_PETS_GROUP,
                ModItems.HAMSTER_BED_ITEMS.values().toArray(RegistrySupplier[]::new));
        CreativeTabRegistry.append(ADORABLE_HAMSTER_PETS_GROUP,
                ModItems.ACORN_CRATE,
                ModItems.CUCUMBER_CRATE,
                ModItems.GREEN_BEANS_CRATE,
                ModItems.HAMSTER_FOOD_MIX_CRATE);
    }
}
