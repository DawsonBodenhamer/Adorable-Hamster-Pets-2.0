package net.dawson.adorablehamsterpets.entity.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;

/**
 * Persistent condition state for Redstone Fever. Runtime behavior accesses this state through {@link HamsterEntity}.
 */
public final class RedstoneFeverState {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ─────────────────────────────────────────────────────────────────────────────*/

    // --- Persistent Condition State ---
    private boolean fevered;
    private int scarVariant = -1;
    private long sunlightTicks;
    private boolean commissionedRollResolved;
    @Nullable private UUID firstLeadRescuerUuid;
    @Nullable private UUID firstSunlightTargetUuid;

    // --- Runtime Presentation State ---
    private boolean shiverPeakArmed = true;
    @Nullable private ShiverSchedule scheduledShiver;

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ─────────────────────────────────────────────────────────────────────────────*/

    public boolean isFevered() {
        return this.fevered;
    }

    public void setFevered(boolean fevered) {
        this.fevered = fevered;
        if (!fevered) {
            this.scarVariant = -1;
            this.sunlightTicks = 0L;
            this.firstLeadRescuerUuid = null;
            this.firstSunlightTargetUuid = null;
            this.shiverPeakArmed = true;
            this.scheduledShiver = null;
        }
    }

    public int getScarVariant() {
        return this.scarVariant;
    }

    public void setScarVariant(int scarVariant) {
        this.scarVariant = Math.clamp(scarVariant, -1, 2);
    }

    public long getSunlightTicks() {
        return this.sunlightTicks;
    }

    public void setSunlightTicks(long sunlightTicks) {
        this.sunlightTicks = Math.max(0L, sunlightTicks);
    }

    public boolean isCommissionedRollResolved() {
        return this.commissionedRollResolved;
    }

    public void setCommissionedRollResolved(boolean commissionedRollResolved) {
        this.commissionedRollResolved = commissionedRollResolved;
    }

    @Nullable
    public UUID getFirstLeadRescuerUuid() {
        return this.firstLeadRescuerUuid;
    }

    public void setFirstLeadRescuerUuid(@Nullable UUID firstLeadRescuerUuid) {
        this.firstLeadRescuerUuid = firstLeadRescuerUuid;
    }

    @Nullable
    public UUID getFirstSunlightTargetUuid() {
        return this.firstSunlightTargetUuid;
    }

    public void setFirstSunlightTargetUuid(@Nullable UUID firstSunlightTargetUuid) {
        this.firstSunlightTargetUuid = firstSunlightTargetUuid;
    }

    public boolean isShiverPeakArmed() {
        return this.shiverPeakArmed;
    }

    public void setShiverPeakArmed(boolean shiverPeakArmed) {
        this.shiverPeakArmed = shiverPeakArmed;
    }

    @Nullable
    public ShiverSchedule getScheduledShiver() {
        return this.scheduledShiver;
    }

    public void scheduleShiver(
            SoundEvent sound,
            long durationTicks,
            long triggerTick,
            long peakTick,
            double clipPeakOffsetTicks,
            double pitchMultiplier) {
        this.scheduledShiver = new ShiverSchedule(
                sound, durationTicks, triggerTick, peakTick, clipPeakOffsetTicks, pitchMultiplier);
    }

    public void clearScheduledShiver() {
        this.scheduledShiver = null;
    }

    // --- Typed Transfer Boundary ---
    public TransferData createTransferData() {
        return new TransferData(
                this.fevered,
                this.scarVariant,
                this.sunlightTicks,
                this.commissionedRollResolved,
                Optional.ofNullable(this.firstLeadRescuerUuid),
                Optional.ofNullable(this.firstSunlightTargetUuid)
        );
    }

    public void applyTransferData(TransferData data) {
        this.fevered = data.fevered();
        this.scarVariant = data.scarVariant();
        this.sunlightTicks = data.sunlightTicks();
        this.commissionedRollResolved = data.commissionedRollResolved();
        this.firstLeadRescuerUuid = data.firstLeadRescuerUuid().orElse(null);
        this.firstSunlightTargetUuid = data.firstSunlightTargetUuid().orElse(null);

        this.shiverPeakArmed = true;
        this.scheduledShiver = null;
    }

    // --- NBT Persistence ---
    public void writeNbt(CompoundTag nbt) {
        this.createTransferData().writeNbt(nbt);
    }

    public void readNbt(CompoundTag nbt) {
        this.applyTransferData(TransferData.fromNbt(nbt));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ──────────────────────────────────────────────────────────────────────────────*/

    /**
     * Immutable persistent snapshot used by shoulder, projectile, and other entity transfers.
     */
    public record TransferData(
            boolean fevered,
            int scarVariant,
            long sunlightTicks,
            boolean commissionedRollResolved,
            Optional<UUID> firstLeadRescuerUuid,
            Optional<UUID> firstSunlightTargetUuid
    ) {

        private static final String FEVERED_KEY = "RedstoneFevered";
        private static final String SCAR_KEY = "RedstoneFeverScar";
        private static final String SUNLIGHT_TICKS_KEY = "RedstoneFeverSunlightTicks";
        private static final String COMMISSIONED_ROLL_RESOLVED_KEY = "RedstoneFeverCommissionedRollResolved";
        private static final String FIRST_LEAD_RESCUER_KEY = "RedstoneFeverFirstLeadRescuer";
        private static final String FIRST_SUNLIGHT_TARGET_KEY = "RedstoneFeverFirstSunlightTarget";

        public static final Codec<TransferData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.optionalFieldOf(FEVERED_KEY, false)
                                .forGetter(TransferData::fevered),
                        Codec.INT.optionalFieldOf(SCAR_KEY, -1)
                                .forGetter(TransferData::scarVariant),
                        Codec.LONG.optionalFieldOf(SUNLIGHT_TICKS_KEY, 0L)
                                .forGetter(TransferData::sunlightTicks),
                        Codec.BOOL.optionalFieldOf(COMMISSIONED_ROLL_RESOLVED_KEY, false)
                                .forGetter(TransferData::commissionedRollResolved),
                        UUIDUtil.AUTHLIB_CODEC.optionalFieldOf(FIRST_LEAD_RESCUER_KEY)
                                .forGetter(TransferData::firstLeadRescuerUuid),
                        UUIDUtil.AUTHLIB_CODEC.optionalFieldOf(FIRST_SUNLIGHT_TARGET_KEY)
                                .forGetter(TransferData::firstSunlightTargetUuid)
                ).apply(instance, TransferData::new)
        );

        public TransferData {
            scarVariant = fevered ? Math.clamp(scarVariant, 0, 2) : -1;
            sunlightTicks = Math.max(0L, sunlightTicks);
            firstLeadRescuerUuid = firstLeadRescuerUuid == null ? Optional.empty() : firstLeadRescuerUuid;
            firstSunlightTargetUuid = firstSunlightTargetUuid == null ? Optional.empty() : firstSunlightTargetUuid;
        }

        public static TransferData healthy() {
            return new TransferData(false, -1, 0L, false, Optional.empty(), Optional.empty());
        }

        public static TransferData fromNbt(CompoundTag nbt) {
            boolean fevered = nbt.getBooleanOr(FEVERED_KEY, false);
            return new TransferData(
                    fevered,
                    fevered ? nbt.getIntOr(SCAR_KEY, 0) : -1,
                    nbt.getLongOr(SUNLIGHT_TICKS_KEY, 0L),
                    nbt.getBooleanOr(COMMISSIONED_ROLL_RESOLVED_KEY, false),
                    nbt.read(FIRST_LEAD_RESCUER_KEY, UUIDUtil.CODEC),
                    nbt.read(FIRST_SUNLIGHT_TARGET_KEY, UUIDUtil.CODEC)
            );
        }

        public void writeNbt(CompoundTag nbt) {
            nbt.putBoolean(FEVERED_KEY, this.fevered);
            nbt.putInt(SCAR_KEY, this.scarVariant);
            nbt.putLong(SUNLIGHT_TICKS_KEY, this.sunlightTicks);
            nbt.putBoolean(COMMISSIONED_ROLL_RESOLVED_KEY, this.commissionedRollResolved);
            this.firstLeadRescuerUuid.ifPresent(uuid -> nbt.store(FIRST_LEAD_RESCUER_KEY, UUIDUtil.CODEC, uuid));
            this.firstSunlightTargetUuid.ifPresent(uuid -> nbt.store(FIRST_SUNLIGHT_TARGET_KEY, UUIDUtil.CODEC, uuid));
        }
    }

    public record ShiverSchedule(
            SoundEvent sound,
            long durationTicks,
            long triggerTick,
            long peakTick,
            double clipPeakOffsetTicks,
            double pitchMultiplier) {}
}
