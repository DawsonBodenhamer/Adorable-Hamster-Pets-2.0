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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

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

        int baseIndex = 0;
        int maxRow = 0;
        double globalZOffset = 0.0;
        String currentBaseId = "";

        int delayTicks = 0;
    }

    private static final PermutationState permState = new PermutationState();

    /**
     * Ticks an asynchronous permutation spawning process.
     * Prevents locking up the main server thread by processing spawns in batches.
     */
    public static void onServerTick(MinecraftServer server) {
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
            String base = genome.basePaletteId();

            if (!base.equals(permState.currentBaseId)) {
                // Extra offset for new base color
                if (!permState.currentBaseId.isEmpty()) {
                    permState.globalZOffset += (permState.maxRow * 1.0) + 2.0;
                }
                permState.currentBaseId = base;
                permState.maxRow = 0;
                permState.baseIndex = 0;
            }

            int row = permState.baseIndex / 500;
            if (row > permState.maxRow) permState.maxRow = row;

            double zOffset = permState.globalZOffset + row * 1.0;
            double xOffset = (permState.baseIndex % 500) * 1.0;

            // Use cached start pos to ensure the grid doesn't drift if player moves
            spawnFrozenHamster(permState.world, permState.startPos.add(xOffset, 0, zOffset), 0, genome);

            permState.currentIndex++;
            permState.baseIndex++;
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

        spawnFrozenHamster(world, player.getPos(), player.getYaw(), new HamsterGenome(base, wPatInt, wPalStr, bPatInt, bPalStr, eyeInt));
        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Spawned requested hamster."), false);
        return 1;
    }

    /**
     * Executes the command to spawn all base variants mapped to their 3D color space coordinates.
     */
    public static int executeSpawnAllBases3D(ServerCommandSource source, boolean withOverlays, String author) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        List<HamsterGenome> genomes = getGenomesToSpawn(withOverlays, author); // Number being spawned

        // Dynamic scale based on total number
        double scale = Math.max(2.0,
                Math.min(15.0, // Cap at 15
                        Math.cbrt(genomes.size()) // Number being spawned
                                * 2.0)); // Spacing multiplier
        boolean centerTagged = false;

        for (HamsterGenome genome : genomes) {
            PaletteDefinition def = HamsterPaletteManager.PALETTE_REGISTRY.get(genome.basePaletteId());
            if (def == null) continue;
            Vec3d hsbPos = def.colorSpacePos();
            double offsetAmount = withOverlays ? 2.5 : 0.0;
            double dx = (hsbPos.x * scale) + (player.getServerWorld().random.nextDouble() - 0.5) * offsetAmount;
            double dy = (hsbPos.z * scale) + (player.getServerWorld().random.nextDouble() - 0.5) * offsetAmount;
            double dz = (hsbPos.y * scale) + (player.getServerWorld().random.nextDouble() - 0.5) * offsetAmount;

            // Force them to look at the center of the cylinder
            float yaw = (float) Math.toDegrees(Math.atan2(-dz, -dx)) - 90.0f;
            HamsterEntity hamster = spawnFrozenHamster(player.getServerWorld(), player.getPos().add(dx, dy, dz), yaw, genome);

            // Tag the center-most hamster (Pure White) so it can spawn the 3D cylinder particle visuals
            if (hamster != null && def.zone() == HamsterColorZone.WHITE && !centerTagged) {
                hamster.addCommandTag("3d_layout_center");
                hamster.addCommandTag("3d_scale_" + scale);
                hamster.addCommandTag("3d_base_y_" + player.getBlockPos().getY());
                centerTagged = true;
            }
        }
        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Spawned " + genomes.size() + " base variants in a 3D HSB layout."), false);
        return 1;
    }

    /**
     * Executes the command to spawn all base variants mapped onto a flat 2D grid.
     */
    public static int executeSpawnAllBases2D(ServerCommandSource source, boolean withOverlays, String author) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        List<HamsterGenome> genomes = getGenomesToSpawn(withOverlays, author);

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

        double spacing = 0.7;
        double groupSpacing = 1.0;
        double currentX = 0;
        double currentZ = 0;
        double rowMaxZ = 0;

        // Calculate a dynamic max width to form a rough square layout
        double maxWidth = Math.max(15.0, Math.ceil(Math.sqrt(genomes.size())) * spacing * 1.2);

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

                spawnFrozenHamster(player.getServerWorld(),
                        player.getPos().add(currentX + (localX * spacing), 0, currentZ + (localZ * spacing)),
                        0, zoneGenomes.get(i));
            }

            currentX += groupWidth + groupSpacing;
            rowMaxZ = Math.max(rowMaxZ, groupDepth);
        }

        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Spawned " + genomes.size() + " variants, organized by color group and diluteness."), false);
        return 1;
    }

    /**
     * Executes the massive command to spawn every single mathematically possible permutation.
     */
    public static int executeSpawnAllPermutations(ServerCommandSource source, boolean ignoreSafetyLimits) throws CommandSyntaxException {
        if (permState.active) {
            source.sendFeedback(() -> Text.literal("[Hamster Genetics] A permutation spawn is already in progress.").formatted(Formatting.RED), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("[Hamster Genetics] Calculating valid permutations...").formatted(Formatting.YELLOW), true);

        // Separate thread for performance
        CompletableFuture.supplyAsync(() -> {
            List<HamsterGenome> permutations = new ArrayList<>();
            int overlayPatterns = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size() - 1;
            List<String> allPaletteIds = new ArrayList<>(HamsterPaletteManager.PALETTE_REGISTRY.keySet());

            for (PaletteDefinition baseDef : HamsterPaletteManager.PALETTE_REGISTRY.values()) {
                List<HamsterColorZone> allowedWildZones = new ArrayList<>(ConfigDataCache.getAllowedWildOverlayZones());
                allowedWildZones.remove(baseDef.zone());

                if (ConfigDataCache.getRestrictedBaseZones().contains(baseDef.zone())) {
                    allowedWildZones.removeAll(ConfigDataCache.getClashingOverlayZones());
                }

                // Default config: must be brighter and less saturated than base coat
                List<String> validWildOverlayIds = HamsterPaletteManager.PALETTE_REGISTRY.values().stream()
                        .filter(p -> allowedWildZones.contains(p.zone()))
                        .filter(p -> HamsterGeneticsUtil.isValidWildOverlay(baseDef, p))
                        .map(PaletteDefinition::id)
                        .toList();

                // Build valid wild combos for this base
                List<HamsterGenome> baseWildCombos = new ArrayList<>();
                baseWildCombos.add(new HamsterGenome(baseDef.id(), 0, null, 0, null, 0));

                for (String wPalId : validWildOverlayIds) {
                    for (int wPat = 1; wPat <= overlayPatterns; wPat++) {
                        baseWildCombos.add(new HamsterGenome(baseDef.id(), wPat, wPalId, 0, null, 0));
                    }
                }

                // Build breeding overlays
                List<HamsterGenome> breedingCombos = new ArrayList<>();
                breedingCombos.add(new HamsterGenome(null, 0, null, 0, null, 0));

                for (String bPalId : allPaletteIds) {
                    for (int bPat = 1; bPat <= overlayPatterns; bPat++) {
                        breedingCombos.add(new HamsterGenome(null, 0, null, bPat, bPalId, 0));
                    }
                }

                // Cross product
                for (HamsterGenome wCombo : baseWildCombos) {
                    for (HamsterGenome bCombo : breedingCombos) {
                        // Skip invalid breeding overlays
                        if (bCombo.breedingOverlayPaletteId() != null) {
                            PaletteDefinition breedingDef = HamsterPaletteManager.PALETTE_REGISTRY.get(bCombo.breedingOverlayPaletteId());
                            if (breedingDef != null && !HamsterGeneticsUtil.isValidBreedingOverlay(baseDef, breedingDef)) {
                                continue;
                            }
                        }

                        // Carrier looks identical to Black so skip for visual distinction
                        permutations.add(new HamsterGenome(wCombo.basePaletteId(), wCombo.wildOverlayPattern(), wCombo.wildOverlayPaletteId(), bCombo.breedingOverlayPattern(), bCombo.breedingOverlayPaletteId(), 0));
                        permutations.add(new HamsterGenome(wCombo.basePaletteId(), wCombo.wildOverlayPattern(), wCombo.wildOverlayPaletteId(), bCombo.breedingOverlayPattern(), bCombo.breedingOverlayPaletteId(), 2));
                    }
                }
            }
            return permutations;
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

            permState.genomesToSpawn = permutations;
            permState.currentIndex = 0;
            permState.baseIndex = 0;
            permState.maxRow = 0;
            permState.globalZOffset = 0.0;
            permState.currentBaseId = "";
            permState.delayTicks = 0;

            source.sendFeedback(() -> Text.literal("[Hamster Genetics] Starting batch-spawn of " + permutations.size() + " permutations in a 500 x 5,370 grid (blocks).").formatted(Formatting.GREEN), true);
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

    private static HamsterEntity spawnFrozenHamster(ServerWorld world, Vec3d pos, float yaw, HamsterGenome genome) {
        HamsterEntity hamster = ModEntities.HAMSTER.get().create(world);
        if (hamster != null) {
            hamster.setGenome(genome);
            hamster.setAiDisabled(true);
            hamster.setNoGravity(true);
            hamster.setSitting(true, true);
            hamster.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0);
            hamster.setYaw(yaw);
            hamster.setBodyYaw(yaw);
            hamster.setHeadYaw(yaw);
            world.spawnEntity(hamster);
        }
        return hamster;
    }

    private static List<HamsterGenome> getGenomesToSpawn(boolean withOverlays, String targetAuthor) {
        List<HamsterGenome> genomes = new ArrayList<>();
        for (PaletteDefinition def : HamsterPaletteManager.PALETTE_REGISTRY.values()) {
            if (!targetAuthor.equals("all") && !def.author().equals(targetAuthor)) continue; // Filter base colors by author

            genomes.add(new HamsterGenome(def.id(), 0, null, 0, null, 0)); // Base color group

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
                            genomes.add(new HamsterGenome(def.id(), pattern, overlayPalette.id(), 0, null, 0));
                        }
                    }
                }
            }
        }
        return genomes;
    }
}