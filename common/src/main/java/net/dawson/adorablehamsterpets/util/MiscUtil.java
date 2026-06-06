package net.dawson.adorablehamsterpets.util;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.tag.ModBlockTags;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A centralized utility for miscellaneous things that don't fit in other utilities.
 */
public final class MiscUtil {

    /**
     * Utility for determining if a block state represents a bush and selecting particles accordingly.
     */
    public static final class BlockStateUtil {

        public static boolean isBushBlock(BlockState state) {
            return state.isIn(ModBlockTags.BUSHES) || Registries.BLOCK.getId(state.getBlock()).getPath().toLowerCase(Locale.ROOT).contains("bush");
        }

        public static ParticleEffect getHidingSpotParticle(BlockState state) {
            if (isBushBlock(state)) {
                return ModParticles.getForVariant(WoodVariant.BAMBOO); // Green leaves for bushes
            } else {
                return new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
            }
        }
    }

    /**
     * Utility for handling player-specific physics interactions and forced client synchronization.
     */
    public static final class PlayerPhysicsUtil {

        public static final double KNOCKBACK_HORIZONTAL = 0.3;
        public static final double KNOCKBACK_VERTICAL = 0.4;

        private PlayerPhysicsUtil() {}

        /**
         * Applies reliable knockback to a player and forces the server to sync the velocity
         * to the client, overriding client-side movement prediction.
         *
         * @param player    The player to knock back.
         * @param sourcePos The origin position of the knockback.
         */
        public static void applyKnockback(ServerPlayerEntity player, Vec3d sourcePos) {
            // 1. Calculate direction away from source
            Vec3d knockbackDir = player.getPos().subtract(sourcePos).normalize();

            // 2. Apply velocity
            player.setVelocity(knockbackDir.x * KNOCKBACK_HORIZONTAL, KNOCKBACK_VERTICAL, knockbackDir.z * KNOCKBACK_HORIZONTAL);

            // 3. Mark velocity as dirty
            player.velocityDirty = true;

            // 4. Force sync with client using vanilla velocity packet
            player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        }
    }

    /**
     * Utility for managing dynamic or randomized messages sent to players.
     */
    public final class MessagingUtil {

        private MessagingUtil() {}

        /**
         * Selects and sends a randomized sequential message to the player, ensuring it doesn't repeat the
         * last message shown. Optionally handles "first-time" experience logic via a specific advancement.
         *
         * @param player           The player receiving the message.
         * @param advancementId    The ID of the "first time" advancement. If null, first-time logic is skipped.
         * @param messageBaseKey   The translation base key (e.g., "message.mymod.some_event"). Will append ".1", ".2", etc.
         * @param messageCount     The total number of available localized messages in the pool.
         * @param memoryContextKey The NBT dictionary key used to remember the last message shown for this specific event.
         */
        public static void sendRandomizedSequentialMessage(ServerPlayerEntity player, @Nullable Identifier advancementId, String messageBaseKey, int messageCount, String memoryContextKey) {
            PlayerAdvancementTracker tracker = player.getAdvancementTracker();
            int messageIndex;

            if (advancementId != null) {
                AdvancementEntry advancement = player.server.getAdvancementLoader().get(advancementId);

                if (advancement == null) {
                    AdorableHamsterPets.LOGGER.error("[MessagingUtil] CRITICAL: Could not find advancement '{}'. Message will not be sent.", advancementId);
                    return;
                }

                AdvancementProgress progress = tracker.getProgress(advancement);

                if (!progress.isDone()) {
                    // First time ever for this player
                    messageIndex = 0;
                    // Grant advancement so this block doesn't run again
                    for (String criterion : advancement.value().criteria().keySet()) {
                        tracker.grantCriterion(advancement, criterion);
                    }
                } else {
                    messageIndex = getNextRandomMessageIndex(player, messageCount, memoryContextKey);
                }
            } else {
                // No first-time logic required, grab random message
                messageIndex = getNextRandomMessageIndex(player, messageCount, memoryContextKey);
            }

            // Save new index and send message
            ((PlayerEntityAccessor) player).ahp$setLastRandomMessageIndex(memoryContextKey, messageIndex);
            String messageKey = messageBaseKey + "." + (messageIndex + 1);
            player.sendMessage(Text.translatable(messageKey).formatted(Formatting.WHITE), true);
        }

        private static int getNextRandomMessageIndex(ServerPlayerEntity player, int messageCount, String memoryContextKey) {
            PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
            int lastIndex = accessor.ahp$getLastRandomMessageIndex(memoryContextKey);

            List<Integer> possibleIndices = IntStream.range(0, messageCount).boxed().collect(Collectors.toList());
            if (lastIndex >= 0 && lastIndex < messageCount) {
                possibleIndices.remove(Integer.valueOf(lastIndex));
            }

            return possibleIndices.get(player.getWorld().random.nextInt(possibleIndices.size()));
        }
    }

    private MiscUtil() {}

    /**
     * Converts a raw ID into a human-readable format.
     * Example: "cheesecake_mocha" -> "Cheesecake Mocha"
     *
     * @param id The raw string ID to format.
     * @return A capitalized, space-separated human-readable string.
     */
    public static String formatHumanReadableName(String id) {
        if (id == null || id.isEmpty()) return "";
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase(Locale.ROOT));
                }
            }
            if (i < words.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Utility for checking mod compatibility and specific versions.
     */
    public static final class ModCompatUtil {

        // Cache result
        private static final boolean HAS_PUNCHY_VERSION = checkPunchyVersion();

        private ModCompatUtil() {}

        /**
         * Checks if the Punchy mod is installed and is at least version 2.6.0.
         */
        public static boolean hasRequiredPunchyVersion() {
            return HAS_PUNCHY_VERSION;
        }

        private static boolean checkPunchyVersion() {
            if (!Platform.isModLoaded("punchy")) {
                return false;
            }
            try {
                String versionStr = Platform.getMod("punchy").getVersion();
                // Split version string by standard delimiters to extract semantic digits
                String[] parts = versionStr.split("[.\\-+]");
                int major = Integer.parseInt(parts[0]);
                int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

                if (major > 2) return true;
                if (major == 2 && minor > 6) return true;
                return major == 2 && minor == 6 && patch >= 0;
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.warn("Failed to parse Punchy version", e);
                return false;
            }
        }
    }

    /**
     * Utility for converting game ticks into human-readable time formats
     * (Days, Months, Years) for either in-game time or real-life time.
     */
    public final class TimeConversionUtil {

        // Real-Life Time: 1 second = 20 ticks. 1 Day = 86400 seconds = 1,728,000 ticks.
        private static final long TICKS_PER_IRL_DAY = 1_728_000L;

        // Minecraft Time: 1 Day = 20 minutes = 24,000 ticks.
        private static final long TICKS_PER_MC_DAY = 24_000L;

        private static final long DAYS_PER_MONTH = 30L;
        private static final long DAYS_PER_YEAR = 365L;

        private TimeConversionUtil() {
        }

        /**
         * Converts a tick duration into a localized "X Years, Y Months, Z Days" format.
         * Zero-values are hidden to keep the string concise.
         */
        public static Text formatAge(long ageInTicks) {
            long ticksPerMonth = TICKS_PER_MC_DAY * DAYS_PER_MONTH;
            long ticksPerYear = TICKS_PER_MC_DAY * DAYS_PER_YEAR;

            long years = ageInTicks / ticksPerYear;
            long remainder = ageInTicks % ticksPerYear;

            long months = remainder / ticksPerMonth;
            remainder = remainder % ticksPerMonth;

            long days = remainder / TICKS_PER_MC_DAY;

            List<Text> parts = new ArrayList<>();

            if (years > 0) {
                String key = years == 1 ? "time.adorablehamsterpets.year" : "time.adorablehamsterpets.years";
                parts.add(Text.translatable(key, years));
            }

            if (months > 0) {
                String key = months == 1 ? "time.adorablehamsterpets.month" : "time.adorablehamsterpets.months";
                parts.add(Text.translatable(key, months));
            }

            // Always display days if under a month old, or if there is a remainder of days.
            if (days > 0 || (years == 0 && months == 0)) {
                String key = days == 1 ? "time.adorablehamsterpets.day" : "time.adorablehamsterpets.days";
                parts.add(Text.translatable(key, days));
            }

            MutableText result = Text.empty();
            for (int i = 0; i < parts.size(); i++) {
                result.append(parts.get(i));
                if (i < parts.size() - 1) {
                    result.append(Text.literal(", "));
                }
            }

            return result;
        }
    }
}