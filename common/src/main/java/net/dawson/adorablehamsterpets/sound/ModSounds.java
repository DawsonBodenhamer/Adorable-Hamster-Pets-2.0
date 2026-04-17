package net.dawson.adorablehamsterpets.sound;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.mixin.accessor.LivingEntityInvoker;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class ModSounds {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.SOUND_EVENT);

    // --- Ambient Weather Sounds ---
    public static final RegistrySupplier<SoundEvent> GENTLE_BREEZE = registerSoundEvent("gentle_breeze"); // Used for hamster bedding particles

    // --- General Movement Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_BOUNCE = registerSoundEvent("hamster_bounce");
    public static final RegistrySupplier<SoundEvent> HAMSTER_THUMP = registerSoundEvent("hamster_thump");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SWISH = registerSoundEvent("hamster_swish");

    // --- Impact & Throw Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_IMPACT = registerSoundEvent("hamster_impact");
    public static final RegistrySupplier<SoundEvent> HAMSTER_THROW = registerSoundEvent("hamster_throw");
    public static final RegistrySupplier<SoundEvent> HAMSTER_INCOMING = registerSoundEvent("hamster_throw_reversed");

    // --- Flying & Special Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_AIRBORNE_CELEBRATION = registerSoundEvent("hamster_airborne_celebration");
    public static final RegistrySupplier<SoundEvent> HAMSTER_WOW = registerSoundEvent("hamster_wow");

    // --- Attack Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_ATTACK1 = registerSoundEvent("hamster_attack1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_ATTACK2 = registerSoundEvent("hamster_attack2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_ATTACK3 = registerSoundEvent("hamster_attack3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_ATTACK4 = registerSoundEvent("hamster_attack4");

    // --- Beg Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_BEG1 = registerSoundEvent("hamster_beg1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_BEG2 = registerSoundEvent("hamster_beg2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_BEG3 = registerSoundEvent("hamster_beg3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_BEG4 = registerSoundEvent("hamster_beg4");
    public static final RegistrySupplier<SoundEvent> HAMSTER_BEG5 = registerSoundEvent("hamster_beg5");

    // --- Celebrate Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_CELEBRATE1 = registerSoundEvent("hamster_celebrate1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_CELEBRATE2 = registerSoundEvent("hamster_celebrate2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_CELEBRATE3 = registerSoundEvent("hamster_celebrate3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_CELEBRATE4 = registerSoundEvent("hamster_celebrate4");

    // --- Creeper Detect Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_CREEPER_DETECT1 = registerSoundEvent("hamster_creeper_detect1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_CREEPER_DETECT2 = registerSoundEvent("hamster_creeper_detect2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_CREEPER_DETECT3 = registerSoundEvent("hamster_creeper_detect3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_CREEPER_DETECT4 = registerSoundEvent("hamster_creeper_detect4");

    // --- Sniff Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_SNIFF1 = registerSoundEvent("hamster_sniff1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SNIFF2 = registerSoundEvent("hamster_sniff2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SNIFF3 = registerSoundEvent("hamster_sniff3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SNIFF4 = registerSoundEvent("hamster_sniff4");

    // --- Death Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_DEATH1 = registerSoundEvent("hamster_death1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_DEATH2 = registerSoundEvent("hamster_death2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_DEATH3 = registerSoundEvent("hamster_death3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_DEATH4 = registerSoundEvent("hamster_death4");

    // --- Hurt Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT1 = registerSoundEvent("hamster_hurt1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT2 = registerSoundEvent("hamster_hurt2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT3 = registerSoundEvent("hamster_hurt3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT4 = registerSoundEvent("hamster_hurt4");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT5 = registerSoundEvent("hamster_hurt5");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT6 = registerSoundEvent("hamster_hurt6");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT7 = registerSoundEvent("hamster_hurt7");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT8 = registerSoundEvent("hamster_hurt8");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT9 = registerSoundEvent("hamster_hurt9");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HURT10 = registerSoundEvent("hamster_hurt10");

    // --- Idle Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE1 = registerSoundEvent("hamster_idle1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE2 = registerSoundEvent("hamster_idle2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE3 = registerSoundEvent("hamster_idle3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE4 = registerSoundEvent("hamster_idle4");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE5 = registerSoundEvent("hamster_idle5");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE6 = registerSoundEvent("hamster_idle6");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE7 = registerSoundEvent("hamster_idle7");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE8 = registerSoundEvent("hamster_idle8");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE9 = registerSoundEvent("hamster_idle9");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE10 = registerSoundEvent("hamster_idle10");
    public static final RegistrySupplier<SoundEvent> HAMSTER_IDLE11 = registerSoundEvent("hamster_idle11");

    // --- Sleep Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP1 = registerSoundEvent("hamster_sleep1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP2 = registerSoundEvent("hamster_sleep2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP3 = registerSoundEvent("hamster_sleep3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP4 = registerSoundEvent("hamster_sleep4");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP5 = registerSoundEvent("hamster_sleep5");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP6 = registerSoundEvent("hamster_sleep6");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP7 = registerSoundEvent("hamster_sleep7");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP8 = registerSoundEvent("hamster_sleep8");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SLEEP9 = registerSoundEvent("hamster_sleep9");

    // --- Wake Up Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_WAKE_UP1 = registerSoundEvent("hamster_wake_up1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_WAKE_UP2 = registerSoundEvent("hamster_wake_up2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_WAKE_UP3 = registerSoundEvent("hamster_wake_up3");

    // --- Cheese Sounds ---
    public static final RegistrySupplier<SoundEvent> CHEESE_USE_SOUND = registerSoundEvent("cheese_use");
    public static final RegistrySupplier<SoundEvent> CHEESE_EAT1 = registerSoundEvent("cheese_eat1");
    public static final RegistrySupplier<SoundEvent> CHEESE_EAT2 = registerSoundEvent("cheese_eat2");
    public static final RegistrySupplier<SoundEvent> CHEESE_EAT3 = registerSoundEvent("cheese_eat3");

    // --- Shoulder Mount/Dismount Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_MOUNT1 = registerSoundEvent("hamster_mount1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_MOUNT2 = registerSoundEvent("hamster_mount2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_MOUNT3 = registerSoundEvent("hamster_mount3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_DISMOUNT = registerSoundEvent("hamster_dismount");

    // --- Cleaning/Scratching Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_SCRATCH1 = registerSoundEvent("hamster_scratch1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SCRATCH2 = registerSoundEvent("hamster_scratch2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SCRATCH3 = registerSoundEvent("hamster_scratch3");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SCRATCH4 = registerSoundEvent("hamster_scratch4");

    // --- Shocked Sounds ---
    public static final RegistrySupplier<SoundEvent> ALARM_ORCHESTRA_HIT = registerSoundEvent("alarm_orchestra_hit");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SHOCKED = registerSoundEvent("hamster_shocked");

    // --- Affection Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_AFFECTION1 = registerSoundEvent("hamster_affection1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_AFFECTION2 = registerSoundEvent("hamster_affection2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_AFFECTION3 = registerSoundEvent("hamster_affection3");

    // --- Diamond Sparkle Sounds ---
    public static final RegistrySupplier<SoundEvent> DIAMOND_SPARKLE1 = registerSoundEvent("diamond_sparkle1");
    public static final RegistrySupplier<SoundEvent> DIAMOND_SPARKLE2 = registerSoundEvent("diamond_sparkle2");
    public static final RegistrySupplier<SoundEvent> DIAMOND_SPARKLE3 = registerSoundEvent("diamond_sparkle3");

    // --- Pounce Sound ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_DIAMOND_POUNCE = registerSoundEvent("hamster_diamond_pounce");

    // --- Shoulder Impact Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_SHOULDER_IMPACT1 = registerSoundEvent("hamster_shoulder_impact1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SHOULDER_IMPACT2 = registerSoundEvent("hamster_shoulder_impact2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_SHOULDER_IMPACT3 = registerSoundEvent("hamster_shoulder_impact3");

    // --- Hamster Bed Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_BED_LEAVES_RUSTLE1 = registerSoundEvent("hamster_bed_leaves_rustle1");
    public static final RegistrySupplier<SoundEvent> HAMSTER_BED_LEAVES_RUSTLE2 = registerSoundEvent("hamster_bed_leaves_rustle2");
    public static final RegistrySupplier<SoundEvent> HAMSTER_BED_LEAVES_RUSTLE3 = registerSoundEvent("hamster_bed_leaves_rustle3");

    // --- Tree Heist Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_ACORN_SEARCH_LOOP = registerSoundEvent("hamster_acorn_search_in_leaves");

    // --- Generic Misc Sounds ---
    public static final RegistrySupplier<SoundEvent> HAMSTER_DING = registerSoundEvent("ding");
    public static final RegistrySupplier<SoundEvent> MAGIC_SHIMMER = registerSoundEvent("magic_shimmer");
    public static final RegistrySupplier<SoundEvent> MAGIC_SPARKLING1 = registerSoundEvent("magic_sparkling1");
    public static final RegistrySupplier<SoundEvent> MAGIC_SPARKLING2 = registerSoundEvent("magic_sparkling2");
    public static final RegistrySupplier<SoundEvent> MAGIC_SPARKLING3 = registerSoundEvent("magic_sparkling3");
    public static final RegistrySupplier<SoundEvent> MAGIC_SPARKLING4 = registerSoundEvent("magic_sparkling4");
    public static final RegistrySupplier<SoundEvent> MAGIC_SPARKLING5 = registerSoundEvent("magic_sparkling5");
    public static final RegistrySupplier<SoundEvent> HAMSTER_HEAD_SHAKE_FAST = registerSoundEvent("hamster_head_shake_fast");

    // --- Sound Lists ---
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_ATTACK_SOUNDS = List.of(
            HAMSTER_ATTACK1, HAMSTER_ATTACK2, HAMSTER_ATTACK3, HAMSTER_ATTACK4
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_IDLE_SOUNDS = List.of(
            HAMSTER_IDLE1, HAMSTER_IDLE2, HAMSTER_IDLE3, HAMSTER_IDLE4, HAMSTER_IDLE5,
            HAMSTER_IDLE6, HAMSTER_IDLE7, HAMSTER_IDLE8, HAMSTER_IDLE9, HAMSTER_IDLE10, HAMSTER_IDLE11
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_SLEEP_SOUNDS = List.of(
            HAMSTER_SLEEP1, HAMSTER_SLEEP2, HAMSTER_SLEEP3, HAMSTER_SLEEP4, HAMSTER_SLEEP5,
            HAMSTER_SLEEP6, HAMSTER_SLEEP7, HAMSTER_SLEEP8, HAMSTER_SLEEP9
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_HURT_SOUNDS = List.of(
            HAMSTER_HURT1, HAMSTER_HURT2, HAMSTER_HURT3, HAMSTER_HURT4, HAMSTER_HURT5,
            HAMSTER_HURT6, HAMSTER_HURT7, HAMSTER_HURT8, HAMSTER_HURT9, HAMSTER_HURT10
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_DEATH_SOUNDS = List.of(
            HAMSTER_DEATH1, HAMSTER_DEATH2, HAMSTER_DEATH3, HAMSTER_DEATH4
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_BEG_SOUNDS = List.of(
            HAMSTER_BEG1, HAMSTER_BEG2, HAMSTER_BEG3, HAMSTER_BEG4, HAMSTER_BEG5
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_CREEPER_DETECT_SOUNDS = List.of(
            HAMSTER_CREEPER_DETECT1, HAMSTER_CREEPER_DETECT2, HAMSTER_CREEPER_DETECT3, HAMSTER_CREEPER_DETECT4
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_DIAMOND_SNIFF_SOUNDS = List.of(
            HAMSTER_SNIFF1, HAMSTER_SNIFF2, HAMSTER_SNIFF3, HAMSTER_SNIFF4
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_CELEBRATE_SOUNDS = List.of(
            HAMSTER_CELEBRATE1, HAMSTER_CELEBRATE2, HAMSTER_CELEBRATE3, HAMSTER_CELEBRATE4
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_FLYING_SOUNDS = List.of(
            HAMSTER_WOW, HAMSTER_AIRBORNE_CELEBRATION
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_WAKE_UP_SOUNDS = List.of(
            HAMSTER_WAKE_UP1, HAMSTER_WAKE_UP2, HAMSTER_WAKE_UP3
    );
    public static final List<RegistrySupplier<SoundEvent>> CHEESE_EAT_SOUNDS = List.of(
            CHEESE_EAT1, CHEESE_EAT2, CHEESE_EAT3
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_SHOULDER_MOUNT_SOUNDS = List.of(
            HAMSTER_MOUNT1, HAMSTER_MOUNT2, HAMSTER_MOUNT3
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_SCRATCH_SOUNDS = List.of(
            HAMSTER_SCRATCH1, HAMSTER_SCRATCH2, HAMSTER_SCRATCH3, HAMSTER_SCRATCH4
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_BOUNCE_SOUNDS = List.of(
            HAMSTER_BOUNCE
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_AFFECTION_SOUNDS = List.of(
            HAMSTER_AFFECTION1, HAMSTER_AFFECTION2, HAMSTER_AFFECTION3
    );
    public static final List<RegistrySupplier<SoundEvent>> DIAMOND_SPARKLE_SOUNDS = List.of(
            DIAMOND_SPARKLE1, DIAMOND_SPARKLE2, DIAMOND_SPARKLE3
    );
    public static final List<RegistrySupplier<SoundEvent>> CROWN_SPARKLE_SOUNDS = List.of(
            MAGIC_SPARKLING1, MAGIC_SPARKLING2, MAGIC_SPARKLING3, MAGIC_SPARKLING4, MAGIC_SPARKLING5
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_SHOULDER_IMPACT_SOUNDS = List.of(
            HAMSTER_SHOULDER_IMPACT1, HAMSTER_SHOULDER_IMPACT2, HAMSTER_SHOULDER_IMPACT3
    );
    public static final List<RegistrySupplier<SoundEvent>> HAMSTER_BED_LEAVES_RUSTLE_SOUNDS = List.of(
            HAMSTER_BED_LEAVES_RUSTLE1, HAMSTER_BED_LEAVES_RUSTLE2, HAMSTER_BED_LEAVES_RUSTLE3
    );

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Registration
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void register() {
        SOUND_EVENTS.register();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Determines an appropriate interaction sound based on the properties of an item.
     * Categorizes items using extensive keyword lists and component checks.
     *
     * @param stack The ItemStack to be evaluated.
     * @return The most fitting SoundEvent for the item.
     */
    public static SoundEvent getDynamicItemSound(ItemStack stack) {
        if (stack.isEmpty()) {
            return SoundEvents.BLOCK_WOOL_PLACE; // Fallback for safety
        }
        Item item = stack.getItem();
        String translationKey = item.getTranslationKey();

        // --- Keyword Lists ---
        List<String> clinkKeywords = List.of(
                "diamond", "emerald", "amethyst", "lapis", "quartz", "raw", "coal",
                "ingot", "nugget", "netherite", "gold", "iron", "copper", "scrap", "shard",
                "brick", "sherd", "flint", "prismarine", "rod", "glass", "bottle", "spyglass",
                "tear", "pearl", "eye", "bell", "trim", "charcoal", "bucket", "shears", "hoe",
                "axe", "pickaxe", "shovel", "sword"
        );

        List<String> stoneKeywords = List.of(
                "stone", "rock", "ore", "andesite", "diorite", "granite", "deepslate",
                "tuff", "calcite", "netherrack", "dust", "basalt", "obsidian", "gravel", "clay",
                "terracotta", "concrete", "powder", "sugar", "bone_meal", "egg", "snowball"
        );

        List<String> woodKeywords = List.of(
                "log", "wood", "acorn", "planks", "stick", "sapling", "door", "trapdoor", "sign",
                "boat", "bowl", "chest", "table", "lectern", "loom", "composter", "barrel", "ladder",
                "fence", "gate", "plate", "button", "torch", "arrow", "bow", "scaffolding", "bamboo",
                "propagule", "roots", "cherry", "acacia", "birch", "dark_oak", "jungle", "oak", "spruce"
        );

        List<String> squishKeywords = List.of(
                "cheese", "flesh", "slime", "magma", "honey", "kelp", "moss", "fungus", "wart", "guts",
                "ink", "moist", "wet", "leaf", "lily", "pad", "vine", "pickle", "cucumber", "beans", "rice",
                "chorus_fruit", "berries", "cabbage", "tomato", "pumpkin", "corn", "egg", "pork", "poisonous",
                "beef", "mutton", "chicken", "rabbit", "cod", "salmon", "spore", "dripleaf", "warped", "stem"
        );

        // --- Category Checks ---
        for (String keyword : clinkKeywords) {
            if (translationKey.contains(keyword)) {
                return ModSounds.HAMSTER_DIAMOND_POUNCE.get(); // "Clink" (for metallic items)
            }
        }
        for (String keyword : stoneKeywords) {
            if (translationKey.contains(keyword)) {
                return SoundEvents.BLOCK_STONE_PLACE; // "Scuff" (for stony items)
            }
        }
        for (String keyword : woodKeywords) {
            if (translationKey.contains(keyword)) {
                return SoundEvents.BLOCK_WOOD_PLACE; // "Thud" (for wooden items)
            }
        }
        for (String keyword : squishKeywords) {
            if (translationKey.contains(keyword)) {
                return ModSounds.CHEESE_USE_SOUND.get(); // "Squish" (for wet/moist items)
            }
        }
        if (item.getComponents().contains(DataComponentTypes.FOOD)) {
            return SoundEvents.ENTITY_GENERIC_EAT; // "Crunch" (for food items)
        }

        // --- Fallback ---
        return SoundEvents.BLOCK_WOOL_PLACE; // "Fump" (for generic/soft items)
    }

    /**
     * Returns the normalized volume for a dynamic sound event.
     * Specifically handles generic eating sound which is naturally much louder.
     *
     * @param sound The sound event to check.
     * @return The normalized volume multiplier.
     */
    public static float getDynamicSoundVolume(SoundEvent sound) {
        return (sound == SoundEvents.ENTITY_GENERIC_EAT) ? 0.35f : 1.0f;
    }

    /**
     * Selects a random sound event from a provided list of suppliers.
     */
    public static SoundEvent getRandomSoundFrom(List<RegistrySupplier<SoundEvent>> sounds, Random random) {
        if (sounds == null || sounds.isEmpty()) {
            AdorableHamsterPets.LOGGER.warn("Attempted to get random sound from empty or null list");
            return null;
        }
        return sounds.get(random.nextInt(sounds.size())).get();
    }

    /**
     * Extracts the native fall sound of the impacted block state.
     */
    public static SoundEvent getDynamicBlockSound(BlockState state) {
        if (state == null || state.isAir()) {
            return SoundEvents.ENTITY_GENERIC_SMALL_FALL;
        }
        return state.getSoundGroup().getFallSound();
    }

    /**
     * Extracts the native hurt or death sound of the impacted entity via Mixin.
     */
    public static SoundEvent getDynamicEntitySound(Entity entity, boolean isDeath, DamageSource source) {
        if (entity instanceof LivingEntity living) {
            try {
                LivingEntityInvoker invoker = (LivingEntityInvoker) living;
                SoundEvent sound = isDeath ? invoker.adorablehamsterpets$callGetDeathSound() : invoker.adorablehamsterpets$callGetHurtSound(source);
                if (sound != null) {
                    return sound;
                }
            } catch (Exception e) {
                // Silently fallback if exception occurs
            }
        }

        // Fallbacks for common non-living entities
        if (entity instanceof AbstractMinecartEntity) return SoundEvents.ENTITY_MINECART_RIDING;
        if (entity instanceof BoatEntity) return SoundEvents.ENTITY_BOAT_PADDLE_LAND;

        // --- Fallback ---
        return SoundEvents.ENTITY_GENERIC_SMALL_FALL;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static RegistrySupplier<SoundEvent> registerSoundEvent(String name) {
        Identifier id = Identifier.of(AdorableHamsterPets.MOD_ID, name);
        return SOUND_EVENTS.register(id, () -> SoundEvent.of(id));
    }
}