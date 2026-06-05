package net.dawson.adorablehamsterpets.util;

public final class HamsterPoseUtil {

    private HamsterPoseUtil() {}

    /**
     * Ensures the personality ID is within valid bounds (1-3), and selects animations based on said ID.
     */
    public static int getValidPersonality(int personalityId) {
        return (personalityId >= 1 && personalityId <= 3) ? personalityId : 1;
    }

    public static String getDeepSleepAnimId(int personalityId) {
        return "anim_hamster_sleep_pose" + getValidPersonality(personalityId);
    }

    public static String getSettleSleepAnimId(int personalityId, boolean fromSitting) {
        return (fromSitting ? "anim_hamster_sit_settle_sleep" : "anim_hamster_stand_settle_sleep") + getValidPersonality(personalityId);
    }

    public static String getSitAnimId(int personalityId) {
        return "sit" + getValidPersonality(personalityId);
    }

    public static String getStandUpAnimId(int personalityId) {
        return "standup" + getValidPersonality(personalityId);
    }

    public static String getWakeUpAnimId(int personalityId) {
        return "wakeup" + getValidPersonality(personalityId);
    }
}