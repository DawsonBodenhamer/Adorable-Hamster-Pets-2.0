package net.dawson.adorablehamsterpets.world.gen;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpWorldGenConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Handles the registration of entity spawns within specific biomes using the Architectury API.
 */
public class ModEntitySpawns {

    public static final Set<Block> VALID_SPAWN_BLOCKS = new HashSet<>();

    // --- Caches for Parsed Config Values ---
    private static final Set<TagKey<Biome>> PARSED_TAGS = new HashSet<>();
    private static final Set<ResourceLocation> PARSED_INCLUDES = new HashSet<>();
    private static final Set<ResourceLocation> PARSED_EXCLUDES = new HashSet<>();
    private static final Set<TagKey<Biome>> PARSED_EXCLUDE_TAGS = new HashSet<>();

    static {
        VALID_SPAWN_BLOCKS.add(Blocks.SAND);
        VALID_SPAWN_BLOCKS.add(Blocks.RED_SAND);
        VALID_SPAWN_BLOCKS.add(Blocks.TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.WHITE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.ORANGE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.MAGENTA_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.LIGHT_BLUE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.YELLOW_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.LIME_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.PINK_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.GRAY_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.LIGHT_GRAY_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.CYAN_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.PURPLE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.BLUE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.BROWN_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.GREEN_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.RED_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.BLACK_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.STONE);
        VALID_SPAWN_BLOCKS.add(Blocks.DEEPSLATE);
        VALID_SPAWN_BLOCKS.add(Blocks.ANDESITE);
        VALID_SPAWN_BLOCKS.add(Blocks.DIORITE);
        VALID_SPAWN_BLOCKS.add(Blocks.GRANITE);
        VALID_SPAWN_BLOCKS.add(Blocks.GRAVEL);
        VALID_SPAWN_BLOCKS.add(Blocks.DIRT);
        VALID_SPAWN_BLOCKS.add(Blocks.MUD);
        VALID_SPAWN_BLOCKS.add(Blocks.PACKED_MUD);
        VALID_SPAWN_BLOCKS.add(Blocks.GRASS_BLOCK);
        VALID_SPAWN_BLOCKS.add(Blocks.MOSS_BLOCK);
        VALID_SPAWN_BLOCKS.add(Blocks.COARSE_DIRT);
        VALID_SPAWN_BLOCKS.add(Blocks.PODZOL);
        VALID_SPAWN_BLOCKS.add(Blocks.SNOW_BLOCK);
        VALID_SPAWN_BLOCKS.add(Blocks.MYCELIUM);
        VALID_SPAWN_BLOCKS.add(Blocks.SCULK);
    }

    /**
     * Parses the biome lists from the config file into Sets for efficient lookup.
     * This should be called once during mod initialization.
     */
    public static void parseConfig() {
        parseConfig(Configs.AHP_WORLDGEN);
    }

    /**
     * Parses the biome lists from the supplied config instance into Sets for efficient lookup.
     */
    public static void parseConfig(AhpWorldGenConfig config) {
        // Clear existing sets to allow for config reloading
        PARSED_TAGS.clear();
        PARSED_INCLUDES.clear();
        PARSED_EXCLUDES.clear();
        PARSED_EXCLUDE_TAGS.clear();

        // Parse Tags
        for (String tagStr : config.spawnBiomeTags) {
            try {
                PARSED_TAGS.add(TagKey.create(Registries.BIOME, ResourceLocation.parse(tagStr)));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.info("[BiomeConfig] Invalid biome tag identifier in config: '{}'", tagStr);
            }
        }
        for (String tagStr : config.spawnBiomeConventionTags) {
            try {
                PARSED_TAGS.add(TagKey.create(Registries.BIOME, ResourceLocation.parse(tagStr)));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.info("[BiomeConfig] Invalid biome tag identifier in config: '{}'", tagStr);
            }
        }

        // Parse Includes
        for (String biomeIdStr : config.includeBiomes) {
            try {
                PARSED_INCLUDES.add(ResourceLocation.parse(biomeIdStr));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.warn("[BiomeConfig] Invalid biome identifier in include list: '{}'", biomeIdStr);
            }
        }

        // Parse Excludes (IDs)
        for (String biomeIdStr : config.excludeBiomes) {
            try {
                PARSED_EXCLUDES.add(ResourceLocation.parse(biomeIdStr));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.warn("[BiomeConfig] Invalid biome identifier in exclude list: '{}'", biomeIdStr);
            }
        }

        // Parse Excludes (Tags)
        for (String tagStr : config.excludeBiomeTags) {
            try {
                PARSED_EXCLUDE_TAGS.add(TagKey.create(Registries.BIOME, ResourceLocation.parse(tagStr)));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.info("[BiomeConfig] Invalid biome exclusion tag identifier in config: '{}'", tagStr);
            }
        }

        AdorableHamsterPets.LOGGER.info("[BiomeConfig] Parsed {} tags, {} includes, {} exclude IDs, and {} exclude tags.",
                PARSED_TAGS.size(), PARSED_INCLUDES.size(), PARSED_EXCLUDES.size(), PARSED_EXCLUDE_TAGS.size());
    }

    // --- Biome Policy Entry Points ---

    /**
     * Fabric biome-registration entry point for the shared parsed policy.
     *
     * @param ctx The biome context provided by Architectury.
     * @return True if hamsters should spawn in this biome, false otherwise.
     */
    public static boolean shouldAddFabricSpawn(BiomeModifications.BiomeContext ctx) {
        ResourceLocation biomeId = ctx.getKey().orElse(null);
        return matchesConfiguredBiomePolicy(biomeId, ctx::hasTag);
    }

    /**
     * NeoForge biome-registration entry point for the shared parsed policy.
     *
     * @param biomeEntry Biome being evaluated.
     * @return True if hamsters should spawn in this biome, false otherwise.
     */
    public static boolean shouldAddNeoForgeSpawn(Holder<Biome> biomeEntry) {
        return isBiomeAllowed(biomeEntry);
    }

    // --- Spawn Placement ---

    /**
     * Applies the registered hamster floor predicate to vanilla and supplemental natural spawning.
     */
    public static boolean isValidHamsterNaturalSpawn(
            EntityType<? extends Animal> type,
            ServerLevelAccessor world,
            MobSpawnType reason,
            BlockPos position,
            RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, world, reason, position, random)
                || VALID_SPAWN_BLOCKS.contains(world.getBlockState(position.below()).getBlock());
    }

    // --- Shared Biome Policy ---

    /**
     * Applies the shared include/exclude policy to a biome registry entry.
     *
     * @param biomeEntry Biome being evaluated.
     * @return True when the biome permits hamster spawning.
     */
    public static boolean isBiomeAllowed(Holder<Biome> biomeEntry) {
        ResourceLocation biomeId = biomeEntry.unwrapKey().map(ResourceKey::location).orElse(null);
        return matchesConfiguredBiomePolicy(biomeId, biomeEntry::is);
    }

    private static boolean matchesConfiguredBiomePolicy(
            ResourceLocation biomeId, Predicate<TagKey<Biome>> matchesTag) {
        if (biomeId == null) return false;

        // 1. Exclusion check (ID) - Highest Priority
        if (PARSED_EXCLUDES.contains(biomeId)) {
            return false;
        }

        // 2. Exclusion check (Tag) - High Priority
        for (TagKey<Biome> tag : PARSED_EXCLUDE_TAGS) {
            if (matchesTag.test(tag)) {
                return false;
            }
        }

        // 3. Inclusion check (ID)
        if (PARSED_INCLUDES.contains(biomeId)) {
            return true;
        }

        // 4. Inclusion check (Tag)
        for (TagKey<Biome> tag : PARSED_TAGS) {
            if (matchesTag.test(tag)) {
                return true;
            }
        }
        return false;
    }
}
