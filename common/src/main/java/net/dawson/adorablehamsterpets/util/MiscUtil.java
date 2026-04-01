package net.dawson.adorablehamsterpets.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A centralized utility for miscellaneous things that don't fit in other utilities.
 */
public final class MiscUtil {

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