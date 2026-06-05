package net.dawson.adorablehamsterpets.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterColorZone;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.entity.custom.genetics.PaletteDefinition;
import net.dawson.adorablehamsterpets.util.HamsterGeneticsUtil;
import net.dawson.adorablehamsterpets.util.HamsterPoseUtil;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HamsterSpawnCommandUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        State Machine Inner Class & Ticker
     * ────────────────────────────────────────────────────────────────────────────*/

    private static class PermutationState {
        boolean active = false;
        boolean ignoreSafetyLimits = false;
        ServerPlayerEntity player;
        ServerWorld world;
        Vec3d startPos;

        List<HamsterGenome> genomesToSpawn;
        int currentIndex = 0;
        int delayTicks = 0;
        String batchId = "";
        int maxCols = 0;

        double spacingMultiplier = 1.0;
        boolean randomizeSitting = true;
        boolean randomizeSleeping = true;
        boolean matchPlayerYaw = false;
        boolean randomizeYaw = false;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private static int delayedPromptTicks = 0;
    private static ServerPlayerEntity promptPlayer = null;

    private static final PermutationState permState = new PermutationState();
    public static String lastSpawnBatchId = null;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void onServerTick(MinecraftServer server) {
        // --- Delayed Genetics Visualization Prompt ---
        if (delayedPromptTicks > 0 && promptPlayer != null) {
            delayedPromptTicks--;
            if (delayedPromptTicks == 0) {
                promptPlayer.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.instructions.line1").formatted(Formatting.GOLD), false);
                promptPlayer.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.instructions.line2").formatted(Formatting.WHITE), false);
                promptPlayer.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.instructions.line3").formatted(Formatting.WHITE), false);
                promptPlayer.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.instructions.line4").formatted(Formatting.AQUA), false);
                promptPlayer.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.instructions.line5").formatted(Formatting.AQUA), false);
                promptPlayer = null;
            }
        }

        // --- Asynchronous Permutation Spawning ---
        // Prevent locking up main server thread by processing spawns in batches
        if (!permState.active || permState.genomesToSpawn == null) return;

        if (permState.delayTicks > 0) {
            permState.delayTicks--;
            return;
        }

        // Safety Limit Check (MSPT)
        if (!permState.ignoreSafetyLimits) {
            // Calculate average tick time in milliseconds
            long[] times = server.getTickTimes();
            long sum = 0;
            for (long time : times) {
                sum += time;
            }
            float mspt = (float) (sum / times.length) / 1000000.0f;

            if (mspt > 70.0f) {
                permState.active = false;
                long remaining = permState.genomesToSpawn.size() - permState.currentIndex;
                permState.player.sendMessage(Text.literal("[Hamster Genetics] Safety limit reached. Server milliseconds per tick is " + String.format("%.1f", mspt) + " (max 70.0). Stopping spawn sequence.").formatted(Formatting.RED), false);
                permState.player.sendMessage(Text.literal("[Hamster Genetics] Your server was able to successfully spawn: " + permState.currentIndex + " permutations before dying.").formatted(Formatting.YELLOW), false);
                permState.player.sendMessage(Text.literal("[Hamster Genetics] Total permutations still un-spawned: " + remaining).formatted(Formatting.YELLOW), false);
                return;
            }
        }

        int spawnedThisTick = 0;

        // Spawn batch of 5,000 per interval
        while (spawnedThisTick < 5000 && permState.active) {
            if (permState.currentIndex >= permState.genomesToSpawn.size()) {
                permState.active = false;
                permState.player.sendMessage(Text.literal("[Hamster Genetics] Successfully spawned all " + permState.currentIndex + " permutations. RIP your PC.").formatted(Formatting.GREEN), false);
                return;
            }

            HamsterGenome genome = permState.genomesToSpawn.get(permState.currentIndex);

            int row = permState.currentIndex / permState.maxCols;
            int col = permState.currentIndex % permState.maxCols;

            double zOffset = row * 1.0 * permState.spacingMultiplier;
            double xOffset = col * 1.0 * permState.spacingMultiplier;

            float yaw = 0;
            if (permState.randomizeYaw) {
                yaw = permState.world.random.nextFloat() * 360.0f;
            } else if (permState.matchPlayerYaw) {
                yaw = permState.player.getYaw();
            }

            // Use cached start pos to ensure the grid doesn't drift if player moves
            HamsterEntity hamster = spawnFrozenHamster(permState.world, permState.startPos.add(xOffset, 0, zOffset), yaw, genome, permState.randomizeSitting, permState.randomizeSleeping);
            if (hamster != null) {
                hamster.addCommandTag("ahp_batch_" + permState.batchId);
            }

            permState.currentIndex++;
            spawnedThisTick++;
        }

        // Setup delay for next batch and log progress
        if (permState.active) {
            permState.delayTicks = 10;
            if (permState.currentIndex % 10000 == 0) {
                AdorableHamsterPets.LOGGER.info("[Hamster Genetics] Spawned {} / {} permutations...", permState.currentIndex, permState.genomesToSpawn.size());
                permState.player.sendMessage(Text.literal(String.format("[Hamster Genetics] Spawned %d / %d permutations...", permState.currentIndex, permState.genomesToSpawn.size())).formatted(Formatting.WHITE), false);
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Command Execution Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Undoes the very last executed spawn command by safely discarding all hamsters
     * marked with the latest batch ID.
     */
    public static int executeUndoLastSpawn(ServerCommandSource source) {
        if (lastSpawnBatchId == null) {
            source.sendFeedback(() -> Text.literal("[Hamster Genetics] No recent spawn command found to undo.").formatted(Formatting.RED), false);
            return 0;
        }

        // Abort active permutation sequence if that's what is being undone
        if (permState.active && lastSpawnBatchId.equals(permState.batchId)) {
            permState.active = false;
            source.sendFeedback(() -> Text.literal("[Hamster Genetics] Halted ongoing permutation spawn sequence.").formatted(Formatting.YELLOW), false);
        }

        String targetTag = "ahp_batch_" + lastSpawnBatchId;
        int count = 0;

        List<HamsterEntity> toRemove = new ArrayList<>();

        // Search all loaded entities for the targeted batch ID
        for (ServerWorld world : source.getServer().getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof HamsterEntity hamster) {
                    if (hamster.getCommandTags().contains(targetTag)) {
                        toRemove.add(hamster);
                    }
                }
            }
        }

        // Safely discard the collected entities
        for (HamsterEntity hamster : toRemove) {
            hamster.discard();
            count++;
        }

        final int finalCount = count;
        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Undid last spawn. Removed " + finalCount + " hamster(s).").formatted(Formatting.GREEN), false);

        lastSpawnBatchId = null;
        return 1;
    }

    /**
     * Executes the command to spawn a highly specific hamster based on exact genetic traits.
     */
    public static int executeSpawnSpecific(ServerCommandSource source, String base, String wildPat, String wildPal, String breedPat, String breedPal, String eyes) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();

        // Parse human-readable strings to integers
        int wPatInt = Math.max(0, HamsterPaletteManager.OVERLAY_PATTERN_NAMES.indexOf(wildPat));
        int bPatInt = Math.max(0, HamsterPaletteManager.OVERLAY_PATTERN_NAMES.indexOf(breedPat));
        int eyeInt = Math.max(0, HamsterPaletteManager.EYE_GENOTYPE_NAMES.indexOf(eyes));
        String wPalStr = wildPal.equals("none") ? null : wildPal;
        String bPalStr = breedPal.equals("none") ? null : breedPal;

        if (!HamsterPaletteManager.PALETTE_REGISTRY.containsKey(base)) {
            source.sendFeedback(() -> Text.literal("[Hamster Genetics] Invalid base palette: " + base), false);
            return 0;
        }

        lastSpawnBatchId = UUID.randomUUID().toString();
        HamsterEntity hamster = spawnFrozenHamster(world, player.getPos(), player.getYaw(), new HamsterGenome(base, wPatInt, wPalStr, bPatInt, bPalStr, eyeInt), true, true);
        if (hamster != null) {
            hamster.addCommandTag("ahp_batch_" + lastSpawnBatchId);
        }

        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Spawned requested hamster."), false);
        return 1;
    }

    /**
     * Executes the command to spawn all base variants mapped to their 3D color space coordinates.
     */
    public static int executeSpawnAllBases3D(ServerCommandSource source, boolean withOverlays, boolean withSampleBreeding, String author, double spacingMultiplier, boolean randomizeSitting, boolean randomizeSleeping, boolean matchPlayerYaw, boolean randomizeYaw) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        List<HamsterGenome> genomes = getGenomesToSpawn(player.getServerWorld().getRandom(), withOverlays, withSampleBreeding, author); // Number being spawned

        // --- Spacing ---
        // Dynamically scale 3D cylinder based on total number
        double scale;
        if (withSampleBreeding) {
            scale = 25.0; // Tier 3: ~9,600 hamsters
        } else if (withOverlays) {
            scale = 15.0; // Tier 2: ~3,200 hamsters
        } else {
            scale = Math.max(2.0, Math.cbrt(genomes.size()) * 2.0); // Tier 1: ~45 base coats
        }

        scale *= spacingMultiplier;

        boolean centerTagged = false;
        lastSpawnBatchId = UUID.randomUUID().toString();

        for (HamsterGenome genome : genomes) {
            PaletteDefinition def = HamsterPaletteManager.PALETTE_REGISTRY.get(genome.basePaletteId());
            if (def == null) continue;
            Vec3d hsbPos = def.colorSpacePos();

            // Tiered micro-scattering to prevent suffocation/overlap
            double offsetAmount = (withSampleBreeding ? 4.0 : (withOverlays ? 2.5 : 0.0)) * spacingMultiplier;

            double dx = (hsbPos.x * scale) + (player.getServerWorld().random.nextDouble() - 0.5) * offsetAmount;
            double dy = (hsbPos.z * scale) + (player.getServerWorld().random.nextDouble() - 0.5) * offsetAmount;
            double dz = (hsbPos.y * scale) + (player.getServerWorld().random.nextDouble() - 0.5) * offsetAmount;

            // Force them to look at center of cylinder or match player yaw
            float yaw;
            if (randomizeYaw) {
                yaw = player.getServerWorld().random.nextFloat() * 360.0f;
            } else if (matchPlayerYaw) {
                yaw = player.getYaw();
            } else {
                yaw = (float) Math.toDegrees(Math.atan2(-dz, -dx)) - 90.0f;
            }

            HamsterEntity hamster = spawnFrozenHamster(player.getServerWorld(), player.getPos().add(dx, dy, dz), yaw, genome, randomizeSitting, randomizeSleeping);

            if (hamster != null) {
                hamster.addCommandTag("ahp_batch_" + lastSpawnBatchId);
                hamster.addCommandTag("3d_layout_member");
                hamster.setGeneticsVisualizerMember(true);

                // Tag center-most hamster (Pure White) so it can spawn the 3D cylinder particle visuals
                if (def.zone() == HamsterColorZone.WHITE && !centerTagged) {
                    hamster.addCommandTag("3d_layout_center");
                    hamster.addCommandTag("3d_scale_" + scale);
                    hamster.addCommandTag("3d_base_y_" + player.getBlockPos().getY());
                    centerTagged = true;
                }
            }
        }
        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Spawned " + genomes.size() + " base variants in a 3D HSB layout."), false);

        // Start delay for control instructions
        delayedPromptTicks = 80;
        promptPlayer = player;

        return 1;
    }

    /**
     * Executes the command to spawn all base variants mapped onto a flat 2D grid.
     */
    public static int executeSpawnAllBases2D(ServerCommandSource source, boolean withOverlays, boolean withSampleBreeding, String author, double spacingMultiplier, boolean randomizeSitting, boolean randomizeSleeping, boolean matchPlayerYaw, boolean randomizeYaw) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        List<HamsterGenome> genomes = getGenomesToSpawn(player.getServerWorld().getRandom(), withOverlays, withSampleBreeding, author);

        // Group genomes by their genetic color zone
        Map<HamsterColorZone, List<HamsterGenome>> groupedGenomes = new EnumMap<>(HamsterColorZone.class);
        for (HamsterColorZone zone : HamsterColorZone.values()) {
            groupedGenomes.put(zone, new ArrayList<>());
        }
        for (HamsterGenome genome : genomes) {
            PaletteDefinition def = HamsterPaletteManager.PALETTE_REGISTRY.get(genome.basePaletteId());
            if (def != null) {
                groupedGenomes.get(def.zone()).add(genome);
            }
        }

        // Sort zones descending by number of genomes they contain
        List<HamsterColorZone> sortedZones = new ArrayList<>(Arrays.asList(HamsterColorZone.values()));
        sortedZones.sort((z1, z2) -> Integer.compare(groupedGenomes.get(z2).size(), groupedGenomes.get(z1).size()));

        double spacing = 0.7 * spacingMultiplier;
        double groupSpacing = 1.0 * spacingMultiplier;
        double currentX = 0;
        double currentZ = 0;
        double rowMaxZ = 0;

        // Calculate a dynamic max width to form a rough square layout
        double maxWidth = Math.max(15.0, Math.ceil(Math.sqrt(genomes.size())) * spacing * 1.2);

        lastSpawnBatchId = UUID.randomUUID().toString();

        for (HamsterColorZone zone : sortedZones) {
            List<HamsterGenome> zoneGenomes = groupedGenomes.get(zone);
            if (zoneGenomes.isEmpty()) continue;

            // Sort each group by diluteness for a nice gradient
            zoneGenomes.sort((g1, g2) -> {
                PaletteDefinition d1 = HamsterPaletteManager.PALETTE_REGISTRY.get(g1.basePaletteId());
                PaletteDefinition d2 = HamsterPaletteManager.PALETTE_REGISTRY.get(g2.basePaletteId());
                return Double.compare(d2.colorSpacePos().z, d1.colorSpacePos().z);
            });

            int count = zoneGenomes.size();
            int cols = (int) Math.ceil(Math.sqrt(count));
            int rows = (int) Math.ceil((double) count / cols);

            double groupWidth = cols * spacing;
            double groupDepth = rows * spacing;

            // Wrap to next line if necessary
            if (currentX + groupWidth > maxWidth && currentX > 0) {
                currentX = 0;
                currentZ += rowMaxZ + groupSpacing;
                rowMaxZ = 0;
            }

            for (int i = 0; i < count; i++) {
                int localX = i % cols;
                int localZ = i / cols;

                float yaw = 0;
                if (randomizeYaw) {
                    yaw = player.getServerWorld().random.nextFloat() * 360.0f;
                } else if (matchPlayerYaw) {
                    yaw = player.getYaw();
                }

                HamsterEntity hamster = spawnFrozenHamster(player.getServerWorld(),
                        player.getPos().add(currentX + (localX * spacing), 0, currentZ + (localZ * spacing)),
                        yaw, zoneGenomes.get(i), randomizeSitting, randomizeSleeping);

                if (hamster != null) {
                    hamster.addCommandTag("ahp_batch_" + lastSpawnBatchId);
                }
            }

            currentX += groupWidth + groupSpacing;
            rowMaxZ = Math.max(rowMaxZ, groupDepth);
        }

        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Spawned " + genomes.size() + " variants, organized by color group and diluteness."), false);
        return 1;
    }

    /**
     * Executes the command to spawn a specific number of random hamsters, or every mathematically possible permutation.
     */
    public static int executeSpawnRandomGroup(ServerCommandSource source, String countStr, boolean ignoreSafetyLimits, double spacingMultiplier, boolean randomizeSitting, boolean randomizeSleeping, boolean matchPlayerYaw, boolean randomizeYaw) throws CommandSyntaxException {
        if (permState.active) {
            source.sendFeedback(() -> Text.literal("[Hamster Genetics] A permutation spawn is already in progress.").formatted(Formatting.RED), false);
            return 0;
        }

        int parsedCount = 0;
        boolean spawnAll = false;

        if (countStr.equals("all_THIS_CAN_BREAK_YOUR_WORLD")) {
            spawnAll = true;
        } else {
            try {
                parsedCount = Integer.parseInt(countStr);
                if (parsedCount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                source.sendFeedback(() -> Text.literal("[Hamster Genetics] Invalid count. Must be a positive number or 'all_THIS_CAN_BREAK_YOUR_WORLD'.").formatted(Formatting.RED), false);
                return 0;
            }
        }

        // Fast, synchronous path for counts <= 5,000
        if (!spawnAll && parsedCount <= 5000) {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            Vec3d startPos = player.getPos();

            List<HamsterGenome> genomes = generateRandomPermutations(net.minecraft.util.math.random.Random.create(), parsedCount);

            lastSpawnBatchId = UUID.randomUUID().toString();
            int maxCols = Math.max(1, (int) Math.ceil(Math.sqrt(parsedCount)));

            for (int i = 0; i < parsedCount; i++) {
                int row = i / maxCols;
                int col = i % maxCols;

                float yaw = 0;
                if (randomizeYaw) {
                    yaw = world.random.nextFloat() * 360.0f;
                } else if (matchPlayerYaw) {
                    yaw = player.getYaw();
                }

                double colOffset = col * spacingMultiplier;
                double rowOffset = row * spacingMultiplier;

                HamsterEntity hamster = spawnFrozenHamster(world, startPos.add(colOffset, 0, rowOffset), yaw, genomes.get(i), randomizeSitting, randomizeSleeping);
                if (hamster != null) {
                    hamster.addCommandTag("ahp_batch_" + lastSpawnBatchId);
                }
            }

            final int finalParsedCount = parsedCount;
            source.sendFeedback(() -> Text.literal("[Hamster Genetics] Instantly spawned " + finalParsedCount + " random permutations.").formatted(Formatting.GREEN), true);
            return 1;
        }

        // Async batch path for enormous generation requirements
        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Calculating permutations...").formatted(Formatting.YELLOW), true);

        final boolean isAll = spawnAll;
        final int finalCount = parsedCount;

        CompletableFuture.supplyAsync(() -> {
            if (isAll) {
                List<HamsterGenome> permutations = generateAllPermutations();
                Collections.shuffle(permutations);
                return permutations;
            } else {
                return generateRandomPermutations(net.minecraft.util.math.random.Random.create(), finalCount);
            }
        }).thenAcceptAsync(permutations -> {
            // Apply results back on main server thread
            permState.active = true;
            permState.ignoreSafetyLimits = ignoreSafetyLimits;
            try {
                permState.player = source.getPlayerOrThrow();
            } catch (CommandSyntaxException e) {
                permState.active = false;
                return;
            }
            permState.world = permState.player.getServerWorld();
            permState.startPos = permState.player.getPos();

            permState.batchId = UUID.randomUUID().toString();
            lastSpawnBatchId = permState.batchId;

            permState.genomesToSpawn = permutations;
            permState.currentIndex = 0;
            permState.delayTicks = 0;
            permState.maxCols = Math.max(1, (int) Math.ceil(Math.sqrt(permutations.size())));

            permState.spacingMultiplier = spacingMultiplier;
            permState.randomizeSitting = randomizeSitting;
            permState.randomizeSleeping = randomizeSleeping;
            permState.matchPlayerYaw = matchPlayerYaw;
            permState.randomizeYaw = randomizeYaw;

            source.sendFeedback(() -> Text.literal("[Hamster Genetics] Starting batch-spawn of " + permutations.size() + " permutations in a " + permState.maxCols + " x " + permState.maxCols + " grid (blocks).").formatted(Formatting.GREEN), true);
            if (!ignoreSafetyLimits) {
                source.sendFeedback(() -> Text.literal("Safety limits enabled. Command will abort if server MSPT > 70.0.").formatted(Formatting.YELLOW), false);
            } else {
                source.sendFeedback(() -> Text.literal("Safety limits ignored. YOLO.").formatted(Formatting.RED, Formatting.BOLD), false);
            }
        }, source.getServer());

        return 1;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static List<HamsterGenome> generateRandomPermutations(net.minecraft.util.math.random.Random random, int count) {
        List<HamsterGenome> list = new ArrayList<>(count);
        List<PaletteDefinition> allBases = new ArrayList<>(HamsterPaletteManager.PALETTE_REGISTRY.values());
        List<String> allPaletteIds = new ArrayList<>(HamsterPaletteManager.PALETTE_REGISTRY.keySet());
        int overlayPatterns = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size() - 1;

        for (int i = 0; i < count; i++) {
            PaletteDefinition baseDef = allBases.get(random.nextInt(allBases.size()));

            // 1. Pick Wild Overlay
            List<HamsterColorZone> allowedWildZones = new ArrayList<>(ConfigDataCache.getAllowedWildOverlayZones());
            allowedWildZones.remove(baseDef.zone());
            if (ConfigDataCache.getRestrictedBaseZones().contains(baseDef.zone())) {
                allowedWildZones.removeAll(ConfigDataCache.getClashingOverlayZones());
            }
            List<String> validWildOverlayIds = HamsterPaletteManager.PALETTE_REGISTRY.values().stream()
                    .filter(p -> allowedWildZones.contains(p.zone()))
                    .filter(p -> HamsterGeneticsUtil.isValidWildOverlay(baseDef, p))
                    .map(PaletteDefinition::id)
                    .toList();

            int totalWildChoices = 1 + (validWildOverlayIds.size() * overlayPatterns);
            int wildChoice = random.nextInt(totalWildChoices);
            int wildPat = 0;
            String wildPal = null;

            if (wildChoice > 0) {
                wildChoice -= 1;
                wildPat = (wildChoice % overlayPatterns) + 1;
                wildPal = validWildOverlayIds.get(wildChoice / overlayPatterns);
            }

            // 2. Pick Breeding Overlay
            List<String> validBreedOverlayIds = allPaletteIds.stream()
                    .filter(id -> HamsterGeneticsUtil.isValidBreedingOverlay(baseDef, HamsterPaletteManager.PALETTE_REGISTRY.get(id)))
                    .toList();

            int effectiveOverlayPatterns = wildPat > 0 ? overlayPatterns - 1 : overlayPatterns;
            int totalBreedChoices = 1 + (validBreedOverlayIds.size() * effectiveOverlayPatterns);
            int breedChoice = random.nextInt(totalBreedChoices);
            int breedPat = 0;
            String breedPal = null;

            if (breedChoice > 0) {
                breedChoice -= 1;
                int offset = breedChoice % effectiveOverlayPatterns;
                breedPat = offset + 1;
                if (wildPat > 0 && breedPat >= wildPat) {
                    breedPat++;
                }
                breedPal = validBreedOverlayIds.get(breedChoice / effectiveOverlayPatterns);
            }

            // 3. Pick Eye Genetics
            int eye = random.nextBoolean() ? 2 : 0; // Exclude invisible carrier state (1) for display

            list.add(new HamsterGenome(baseDef.id(), wildPat, wildPal, breedPat, breedPal, eye));
        }
        return list;
    }

    private static List<HamsterGenome> generateAllPermutations() {
        List<HamsterGenome> permutations = new ArrayList<>();
        int overlayPatterns = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size() - 1;
        List<String> allPaletteIds = new ArrayList<>(HamsterPaletteManager.PALETTE_REGISTRY.keySet());

        for (PaletteDefinition baseDef : HamsterPaletteManager.PALETTE_REGISTRY.values()) {
            List<HamsterColorZone> allowedWildZones = new ArrayList<>(ConfigDataCache.getAllowedWildOverlayZones());
            allowedWildZones.remove(baseDef.zone());

            if (ConfigDataCache.getRestrictedBaseZones().contains(baseDef.zone())) {
                allowedWildZones.removeAll(ConfigDataCache.getClashingOverlayZones());
            }

            List<String> validWildOverlayIds = HamsterPaletteManager.PALETTE_REGISTRY.values().stream()
                    .filter(p -> allowedWildZones.contains(p.zone()))
                    .filter(p -> HamsterGeneticsUtil.isValidWildOverlay(baseDef, p))
                    .map(PaletteDefinition::id)
                    .toList();

            List<HamsterGenome> baseWildCombos = new ArrayList<>();
            baseWildCombos.add(new HamsterGenome(baseDef.id(), 0, null, 0, null, 0));

            for (String wPalId : validWildOverlayIds) {
                for (int wPat = 1; wPat <= overlayPatterns; wPat++) {
                    baseWildCombos.add(new HamsterGenome(baseDef.id(), wPat, wPalId, 0, null, 0));
                }
            }

            List<HamsterGenome> breedingCombos = new ArrayList<>();
            breedingCombos.add(new HamsterGenome(null, 0, null, 0, null, 0));

            for (String bPalId : allPaletteIds) {
                for (int bPat = 1; bPat <= overlayPatterns; bPat++) {
                    breedingCombos.add(new HamsterGenome(null, 0, null, bPat, bPalId, 0));
                }
            }

            for (HamsterGenome wCombo : baseWildCombos) {
                for (HamsterGenome bCombo : breedingCombos) {
                    if (bCombo.breedingOverlayPaletteId() != null) {
                        PaletteDefinition breedingDef = HamsterPaletteManager.PALETTE_REGISTRY.get(bCombo.breedingOverlayPaletteId());
                        if (breedingDef != null && !HamsterGeneticsUtil.isValidBreedingOverlay(baseDef, breedingDef)) {
                            continue;
                        }

                        if (wCombo.wildOverlayPattern() > 0 && wCombo.wildOverlayPattern() == bCombo.breedingOverlayPattern()) {
                            continue;
                        }
                    }

                    permutations.add(new HamsterGenome(wCombo.basePaletteId(), wCombo.wildOverlayPattern(), wCombo.wildOverlayPaletteId(), bCombo.breedingOverlayPattern(), bCombo.breedingOverlayPaletteId(), 0));
                    permutations.add(new HamsterGenome(wCombo.basePaletteId(), wCombo.wildOverlayPattern(), wCombo.wildOverlayPaletteId(), bCombo.breedingOverlayPattern(), bCombo.breedingOverlayPaletteId(), 2));
                }
            }
        }
        return permutations;
    }

    private static HamsterEntity spawnFrozenHamster(ServerWorld world, Vec3d pos, float yaw, HamsterGenome genome, boolean randomizeSitting, boolean randomizeSleeping) {
        HamsterEntity hamster = ModEntities.HAMSTER.get().create(world);
        if (hamster != null) {
            hamster.setGenome(genome);
            hamster.setAiDisabled(true);
            hamster.setNoGravity(true);
            hamster.setInvulnerable(true);

            // Assign random personality
            int personality = world.getRandom().nextBetween(1, 3);
            hamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, personality);

            // Pose assignment
            boolean willSleep = false;
            boolean willSit = true;

            if (randomizeSitting && randomizeSleeping) {
                willSleep = world.getRandom().nextBoolean();
                willSit = !willSleep;
            } else if (randomizeSleeping) {
                willSleep = true;
                willSit = false;
            } else if (randomizeSitting) {
                willSit = true;
                willSleep = false;
            }

            if (willSleep) {
                hamster.setSleeping(true);
                hamster.setInSittingPose(true);
                hamster.getDataTracker().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, HamsterPoseUtil.getDeepSleepAnimId(personality));
            } else if (willSit) {
                hamster.setSitting(true, true);
            }

            hamster.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0);
            hamster.setYaw(yaw);
            hamster.setBodyYaw(yaw);
            hamster.setHeadYaw(yaw);
            world.spawnEntity(hamster);
        }
        return hamster;
    }

    private static List<HamsterGenome> getGenomesToSpawn(Random random, boolean withOverlays, boolean withSampleBreeding, String targetAuthor) {
        List<HamsterGenome> genomes = new ArrayList<>();
        for (PaletteDefinition def : HamsterPaletteManager.PALETTE_REGISTRY.values()) {
            if (!targetAuthor.equals("all") && !def.author().equals(targetAuthor)) continue; // Filter base colors by author

            int baseEye = HamsterGeneticsUtil.generateWildEyeGenotype(def.id(), random);
            genomes.add(new HamsterGenome(def.id(), 0, null, 0, null, baseEye));

            if (withOverlays) {
                List<HamsterColorZone> allowedZones = new ArrayList<>(ConfigDataCache.getAllowedWildOverlayZones());
                allowedZones.remove(def.zone()); // Exclude base color group so overlays don't match

                if (ConfigDataCache.getRestrictedBaseZones().contains(def.zone())) {
                    allowedZones.removeAll(ConfigDataCache.getClashingOverlayZones());
                }

                for (HamsterColorZone overlayZone : allowedZones) {
                    // Grab all palettes from this color group that meet criteria
                    List<PaletteDefinition> validOverlays = HamsterPaletteManager.PALETTE_REGISTRY.values().stream()
                            .filter(p -> p.zone() == overlayZone)
                            .filter(p -> targetAuthor.equals("all") || p.author().equals(targetAuthor)) // Filter overlays by author
                            .filter(p -> HamsterGeneticsUtil.isValidWildOverlay(def, p)) // Filter by brightness & saturation (if enabled)
                            .toList();

                    for (PaletteDefinition overlayPalette : validOverlays) {
                        int maxPattern = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size() - 1;
                        for (int pattern = 1; pattern <= maxPattern; pattern++) {
                            int overlayEye = HamsterGeneticsUtil.generateWildEyeGenotype(def.id(), random);
                            genomes.add(new HamsterGenome(def.id(), pattern, overlayPalette.id(), 0, null, overlayEye));
                        }
                    }
                }
            }
        }

        if (withSampleBreeding) {
            List<HamsterGenome> breedingGenomes = new ArrayList<>();
            String[] samplePalettes = {"tortoise_shell", "silver", "rust"};
            int maxPattern = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size() - 1;

            for (HamsterGenome baseGenome : genomes) {
                for (String bPalId : samplePalettes) {
                    int bPat;
                    do {
                        bPat = random.nextBetween(1, maxPattern);
                    } while (bPat == baseGenome.wildOverlayPattern());

                    breedingGenomes.add(new HamsterGenome(
                            baseGenome.basePaletteId(),
                            baseGenome.wildOverlayPattern(),
                            baseGenome.wildOverlayPaletteId(),
                            bPat,
                            bPalId,
                            baseGenome.eyeGenotype()
                    ));
                }
            }
            return breedingGenomes;
        }

        return genomes;
    }
}