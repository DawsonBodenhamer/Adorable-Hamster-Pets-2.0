package net.dawson.adorablehamsterpets.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.RedstoneFeverState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Immutable transfer snapshot for moving one hamster through shoulder, projectile, and other NBT-backed paths.
 */
public record HamsterState(
        IdentityData identityData,
        float health,
        NbtCompound inventoryNbt,
        LifeHistoryData lifeHistoryData,
        StatusData statusData,
        AppearanceData appearanceData,
        BehaviorData behaviorData,
        HamsterConditionData conditionData
) {

    /* ───────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ──────────────────────────────────────────────────────────────────────────────*/

    public static final Codec<NbtCompound> NBT_COMPOUND_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                NbtElement element = dynamic.convert(NbtOps.INSTANCE).getValue();
                if (element instanceof NbtCompound compound) {
                    return DataResult.success(compound);
                }
                return DataResult.error(() -> "Not a compound NBT: " + element);
            },
            nbt -> new Dynamic<>(NbtOps.INSTANCE, nbt)
    );

    public static final Codec<NbtList> NBT_LIST_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                NbtElement element = dynamic.convert(NbtOps.INSTANCE).getValue();
                if (element instanceof NbtList list) {
                    return DataResult.success(list);
                }
                return DataResult.error(() -> "Not a list NBT: " + element);
            },
            nbt -> new Dynamic<>(NbtOps.INSTANCE, nbt)
    );

    private static Codec<HamsterState> createCodec() {
        return RecordCodecBuilder.create(instance ->
                instance.group(
                        IdentityData.CODEC.forGetter(HamsterState::identityData),
                        Codec.FLOAT.fieldOf("health").forGetter(HamsterState::health),
                        NBT_COMPOUND_CODEC.fieldOf("inventoryNbt").forGetter(HamsterState::inventoryNbt),
                        LifeHistoryData.CODEC.forGetter(HamsterState::lifeHistoryData),
                        StatusData.CODEC.forGetter(HamsterState::statusData),
                        AppearanceData.CODEC.forGetter(HamsterState::appearanceData),
                        BehaviorData.CODEC.forGetter(HamsterState::behaviorData),
                        HamsterConditionData.CODEC.forGetter(HamsterState::conditionData)
                ).apply(instance, HamsterState::new)
        );
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ───────────────────────────────────────────────────────────────────────────────*/

    // --- Compatibility Accessors ---
    // Backwards compat for existing call sites while storage remains grouped by domain
    public UUID entityUuid() {
        return this.identityData.entityUuid();
    }

    public NbtCompound genomeNbt() {
        return this.identityData.genomeNbt();
    }

    public Optional<String> customName() {
        return this.identityData.customName();
    }

    public int breedingAge() {
        return this.lifeHistoryData.breedingAge();
    }

    public long totalAgeTicks() {
        return this.lifeHistoryData.totalAgeTicks();
    }

    public int timesBred() {
        return this.lifeHistoryData.timesBred();
    }

    public long throwCooldownEndTick() {
        return this.statusData.throwCooldownEndTick();
    }

    public GreenBeanBuffData greenBeanBuffData() {
        return this.statusData.greenBeanBuffData();
    }

    public int autoEatCooldownTicks() {
        return this.statusData.autoEatCooldownTicks();
    }

    public int flowerPosition() {
        return this.appearanceData.flowerPosition();
    }

    public int animationPersonalityId() {
        return this.appearanceData.animationPersonalityId();
    }

    public boolean armorVisible() {
        return this.appearanceData.armorVisible();
    }

    public MiniGameBehaviorData seekingBehaviorData() {
        return this.behaviorData.seekingBehaviorData();
    }

    public WanderModeData wanderModeData() {
        return this.behaviorData.wanderModeData();
    }

    public int hamsterFlags() {
        return this.behaviorData.hamsterFlags();
    }

    public static Codec<HamsterState> getCodec() {
        return CodecHolder.CODEC;
    }

    public HamsterState withFlags(int newFlags) {
        return new HamsterState(
                this.identityData,
                this.health,
                this.inventoryNbt,
                this.lifeHistoryData,
                this.statusData,
                this.appearanceData,
                this.behaviorData.withHamsterFlags(newFlags),
                this.conditionData
        );
    }

    public NbtCompound toNbt() {
        return (NbtCompound) getCodec().encodeStart(NbtOps.INSTANCE, this)
                .getOrThrow(false, error -> {
                    throw new IllegalStateException("Could not encode HamsterState: " + error);
                });
    }

    public static Optional<HamsterState> fromNbt(NbtCompound nbt) {
        // --- Legacy Migration Shim ---
        // Convert v3.5.0 variants to v3.6.0 genome NBT to prevent shoulder hamsters being deleted
        if (!nbt.contains("genomeNbt", NbtElement.COMPOUND_TYPE)) {
            int legacyId = nbt.contains("variantId", NbtElement.INT_TYPE)
                    ? nbt.getInt("variantId")
                    : 0;
            nbt.put("genomeNbt", HamsterGeneticsUtil.getGenomeForLegacyId(legacyId).saveToNbt());
        }

        return getCodec().parse(NbtOps.INSTANCE, nbt)
                .resultOrPartial(AdorableHamsterPets.LOGGER::error);
    }

    /* ────────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ───────────────────────────────────────────────────────────────────────────────*/

    public record IdentityData(UUID entityUuid, NbtCompound genomeNbt, Optional<String> customName) {

        private static final MapCodec<IdentityData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Uuids.CODEC.fieldOf("entityUuid").forGetter(IdentityData::entityUuid),
                        NBT_COMPOUND_CODEC.fieldOf("genomeNbt").forGetter(IdentityData::genomeNbt),
                        Codec.STRING.optionalFieldOf("customName").forGetter(IdentityData::customName)
                ).apply(instance, IdentityData::new)
        );
    }

    public record LifeHistoryData(int breedingAge, long totalAgeTicks, int timesBred) {

        private static final MapCodec<LifeHistoryData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.INT.fieldOf("breedingAge").forGetter(LifeHistoryData::breedingAge),
                        Codec.LONG.optionalFieldOf("totalAgeTicks", 0L)
                                .forGetter(LifeHistoryData::totalAgeTicks),
                        Codec.INT.optionalFieldOf("timesBred", 0)
                                .forGetter(LifeHistoryData::timesBred)
                ).apply(instance, LifeHistoryData::new)
        );
    }

    public record StatusData(
            long throwCooldownEndTick,
            GreenBeanBuffData greenBeanBuffData,
            int autoEatCooldownTicks
    ) {

        private static final MapCodec<StatusData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.LONG.fieldOf("throwCooldownEndTick").forGetter(StatusData::throwCooldownEndTick),
                        GreenBeanBuffData.CODEC.fieldOf("greenBeanBuffData")
                                .orElse(GreenBeanBuffData.empty())
                                .forGetter(StatusData::greenBeanBuffData),
                        Codec.INT.fieldOf("autoEatCooldownTicks").forGetter(StatusData::autoEatCooldownTicks)
                ).apply(instance, StatusData::new)
        );
    }

    public record AppearanceData(int flowerPosition, int animationPersonalityId, boolean armorVisible) {

        private static final MapCodec<AppearanceData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf("flowerPosition", 0)
                                .forGetter(AppearanceData::flowerPosition),
                        Codec.INT.optionalFieldOf("animationPersonalityId", 1)
                                .forGetter(AppearanceData::animationPersonalityId),
                        Codec.BOOL.optionalFieldOf("armorVisible", true)
                                .forGetter(AppearanceData::armorVisible)
                ).apply(instance, AppearanceData::new)
        );
    }

    public record BehaviorData(
            MiniGameBehaviorData seekingBehaviorData,
            WanderModeData wanderModeData,
            int hamsterFlags
    ) {

        private static final MapCodec<BehaviorData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        MiniGameBehaviorData.CODEC.fieldOf("seekingBehaviorData")
                                .orElse(MiniGameBehaviorData.empty())
                                .forGetter(BehaviorData::seekingBehaviorData),
                        WanderModeData.CODEC.fieldOf("wanderModeData")
                                .orElse(WanderModeData.empty())
                                .forGetter(BehaviorData::wanderModeData),
                        Codec.INT.optionalFieldOf("hamsterFlags", 0)
                                .forGetter(BehaviorData::hamsterFlags)
                ).apply(instance, BehaviorData::new)
        );

        public BehaviorData withHamsterFlags(int newFlags) {
            return new BehaviorData(this.seekingBehaviorData, this.wanderModeData, newFlags);
        }
    }

    /**
     * Typed transfer boundary for mutually exclusive hamster conditions.
     */
    public record HamsterConditionData(RedstoneFeverState.TransferData redstoneFeverData) {

        private static final MapCodec<HamsterConditionData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        RedstoneFeverState.TransferData.CODEC
                                .optionalFieldOf("redstoneFeverNbt", RedstoneFeverState.TransferData.healthy())
                                .forGetter(HamsterConditionData::redstoneFeverData)
                ).apply(instance, HamsterConditionData::new)
        );

        /*
         * TODO Ancient/Void Hamsters:
         * Replace derived ActiveCondition with a sealed condition variant once both data contracts exist.
         * Give Ancient and Void conditions typed transfer records and codecs beside their runtime state.
         * Keep Healthy, Redstone Fever, Ancient, and Void mutually exclusive.
         */
        public ActiveCondition activeCondition() {
            return this.redstoneFeverData.fevered()
                    ? ActiveCondition.REDSTONE_FEVER
                    : ActiveCondition.HEALTHY;
        }

        public static HamsterConditionData capture(RedstoneFeverState state) {
            return new HamsterConditionData(state.createTransferData());
        }

        public void applyTo(RedstoneFeverState state) {
            state.applyTransferData(this.redstoneFeverData);
        }
    }

    public enum ActiveCondition {
        HEALTHY,
        REDSTONE_FEVER
    }

    public record MiniGameBehaviorData(
            boolean isPrimedToSeekDiamonds,
            long foundOreCooldownEndTick,
            long cropSnackCooldownEndTick,
            long hideAndSeekCooldownEndTick,
            Optional<BlockPos> currentOreTarget
    ) {

        public static final Codec<MiniGameBehaviorData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.optionalFieldOf("isPrimedToSeekDiamonds", false)
                                .forGetter(MiniGameBehaviorData::isPrimedToSeekDiamonds),
                        Codec.LONG.optionalFieldOf("foundOreCooldownEndTick", 0L)
                                .forGetter(MiniGameBehaviorData::foundOreCooldownEndTick),
                        Codec.LONG.optionalFieldOf("cropSnackCooldownEndTick", 0L)
                                .forGetter(MiniGameBehaviorData::cropSnackCooldownEndTick),
                        Codec.LONG.optionalFieldOf("hideAndSeekCooldownEndTick", 0L)
                                .forGetter(MiniGameBehaviorData::hideAndSeekCooldownEndTick),
                        BlockPos.CODEC.optionalFieldOf("currentOreTarget")
                                .forGetter(MiniGameBehaviorData::currentOreTarget)
                ).apply(instance, MiniGameBehaviorData::new)
        );

        public static MiniGameBehaviorData empty() {
            return new MiniGameBehaviorData(false, 0L, 0L, 0L, Optional.empty());
        }
    }

    public record GreenBeanBuffData(
            long greenBeanBuffEndTick,
            long greenBeanBuffDuration,
            NbtList activeEffectsNbt
    ) {

        public static final Codec<GreenBeanBuffData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.optionalFieldOf("greenBeanBuffEndTick", 0L)
                                .forGetter(GreenBeanBuffData::greenBeanBuffEndTick),
                        Codec.LONG.optionalFieldOf("greenBeanBuffDuration", 0L)
                                .forGetter(GreenBeanBuffData::greenBeanBuffDuration),
                        NBT_LIST_CODEC.fieldOf("activeEffectsNbt")
                                .forGetter(GreenBeanBuffData::activeEffectsNbt)
                ).apply(instance, GreenBeanBuffData::new)
        );

        public static GreenBeanBuffData empty() {
            return new GreenBeanBuffData(0L, 0L, new NbtList());
        }
    }

    public record WanderModeData(Optional<GlobalPos> linkedBedPos, boolean bypassNextSleepDelay) {

        public static final Codec<WanderModeData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        GlobalPos.CODEC.optionalFieldOf("linkedBedPos")
                                .forGetter(WanderModeData::linkedBedPos),
                        Codec.BOOL.optionalFieldOf("bypassNextSleepDelay", false)
                                .forGetter(WanderModeData::bypassNextSleepDelay)
                ).apply(instance, WanderModeData::new)
        );

        public static WanderModeData empty() {
            return new WanderModeData(Optional.empty(), false);
        }
    }

    private static final class CodecHolder {

        private static final Codec<HamsterState> CODEC = createCodec();
    }
}
