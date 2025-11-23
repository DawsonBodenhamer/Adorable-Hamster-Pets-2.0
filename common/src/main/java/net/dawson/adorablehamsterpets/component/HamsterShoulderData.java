package net.dawson.adorablehamsterpets.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.Optional;
import java.util.UUID;

public record HamsterShoulderData(
        UUID entityUuid,
        int variantId,
        float health,
        NbtCompound inventoryNbt,
        int breedingAge,
        long throwCooldownEndTick,
        GreenBeanBuffData greenBeanBuffData,
        int autoEatCooldownTicks,
        Optional<String> customName,
        int pinkPetalType,
        int animationPersonalityId,
        SeekingBehaviorData seekingBehaviorData,
        WanderModeData wanderModeData,
        int hamsterFlags
) {

    // --- Static Codecs Definitions ---
    public static final Codec<NbtCompound> NBT_COMPOUND_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            (dynamic) -> {
                NbtElement element = dynamic.convert(NbtOps.INSTANCE).getValue();
                if (element instanceof NbtCompound compound) {
                    return DataResult.success(compound);
                }
                return DataResult.error(() -> "Not a compound NBT: " + element);
            },
            (nbt) -> new Dynamic<>(NbtOps.INSTANCE, nbt)
    );

    public static final Codec<NbtList> NBT_LIST_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            (dynamic) -> {
                NbtElement element = dynamic.convert(NbtOps.INSTANCE).getValue();
                if (element instanceof NbtList list) {
                    return DataResult.success(list);
                }
                return DataResult.error(() -> "Not a list NBT: " + element);
            },
            (nbt) -> new Dynamic<>(NbtOps.INSTANCE, nbt)
    );

    // --- Inner Record for Seeking/Sulking Data ---
    public record SeekingBehaviorData(
            boolean isPrimedToSeekDiamonds,
            long foundOreCooldownEndTick,
            Optional<BlockPos> currentOreTarget
    ) {
        public static final Codec<SeekingBehaviorData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("isPrimedToSeekDiamonds").orElse(false).forGetter(SeekingBehaviorData::isPrimedToSeekDiamonds),
                        Codec.LONG.fieldOf("foundOreCooldownEndTick").orElse(0L).forGetter(SeekingBehaviorData::foundOreCooldownEndTick),
                        BlockPos.CODEC.optionalFieldOf("currentOreTarget").forGetter(SeekingBehaviorData::currentOreTarget)
                ).apply(instance, SeekingBehaviorData::new)
        );

        public static SeekingBehaviorData empty() {
            return new SeekingBehaviorData(false, 0L, Optional.empty());
        }
    }

    // --- Inner Record for Green Bean Buff Data ---
    public record GreenBeanBuffData(
            long greenBeanBuffEndTick,
            long greenBeanBuffDuration,
            NbtList activeEffectsNbt // NbtList for 1.20.1 compatibility
    ) {
        public static final Codec<GreenBeanBuffData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("greenBeanBuffEndTick").orElse(0L).forGetter(GreenBeanBuffData::greenBeanBuffEndTick),
                        Codec.LONG.fieldOf("greenBeanBuffDuration").orElse(0L).forGetter(GreenBeanBuffData::greenBeanBuffDuration),
                        NBT_LIST_CODEC.fieldOf("activeEffectsNbt").forGetter(GreenBeanBuffData::activeEffectsNbt)
                ).apply(instance, GreenBeanBuffData::new)
        );

        public static GreenBeanBuffData empty() {
            return new GreenBeanBuffData(0L, 0L, new NbtList());
        }
    }

    // --- Inner Record for Wander Mode/Hamster Bed Data ---
    public record WanderModeData(
            Optional<GlobalPos> linkedBedPos,
            boolean bypassNextSleepDelay
    ) {
        public static final Codec<WanderModeData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        GlobalPos.CODEC.optionalFieldOf("linkedBedPos").forGetter(WanderModeData::linkedBedPos),
                        Codec.BOOL.fieldOf("bypassNextSleepDelay").orElse(false).forGetter(WanderModeData::bypassNextSleepDelay)
                ).apply(instance, WanderModeData::new)
        );

        public static WanderModeData empty() {
            return new WanderModeData(Optional.empty(), false);
        }
    }

    // --- Lazy Initialized Main Codec ---
    private static Codec<HamsterShoulderData> CODEC;

    public static Codec<HamsterShoulderData> getCodec() {
        if (CODEC == null) {
            CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Uuids.CODEC.fieldOf("entityUuid").forGetter(HamsterShoulderData::entityUuid),
                            Codec.INT.fieldOf("variantId").forGetter(HamsterShoulderData::variantId),
                            Codec.FLOAT.fieldOf("health").forGetter(HamsterShoulderData::health),
                            NBT_COMPOUND_CODEC.fieldOf("inventoryNbt").forGetter(HamsterShoulderData::inventoryNbt),
                            Codec.INT.fieldOf("breedingAge").forGetter(HamsterShoulderData::breedingAge),
                            Codec.LONG.fieldOf("throwCooldownEndTick").forGetter(HamsterShoulderData::throwCooldownEndTick),
                            GreenBeanBuffData.CODEC.fieldOf("greenBeanBuffData").orElse(GreenBeanBuffData.empty()).forGetter(HamsterShoulderData::greenBeanBuffData),
                            Codec.INT.fieldOf("autoEatCooldownTicks").forGetter(HamsterShoulderData::autoEatCooldownTicks),
                            Codec.STRING.optionalFieldOf("customName").forGetter(HamsterShoulderData::customName),
                            Codec.INT.fieldOf("pinkPetalType").orElse(0).forGetter(HamsterShoulderData::pinkPetalType),
                            Codec.INT.fieldOf("animationPersonalityId").orElse(1).forGetter(HamsterShoulderData::animationPersonalityId),
                            SeekingBehaviorData.CODEC.fieldOf("seekingBehaviorData").orElse(SeekingBehaviorData.empty()).forGetter(HamsterShoulderData::seekingBehaviorData),
                            WanderModeData.CODEC.fieldOf("wanderModeData").orElse(WanderModeData.empty()).forGetter(HamsterShoulderData::wanderModeData),
                            Codec.INT.fieldOf("hamsterFlags").orElse(0).forGetter(HamsterShoulderData::hamsterFlags)
                    ).apply(instance, HamsterShoulderData::new)
            );
        }
        return CODEC;
    }

    /**
     * Deserializes an NbtCompound into a HamsterShoulderData record.
     * @param nbt The NbtCompound to deserialize.
     * @return An Optional containing the HamsterShoulderData, or empty if deserialization fails.
     */
    public static Optional<HamsterShoulderData> fromNbt(NbtCompound nbt) {
        return getCodec().parse(NbtOps.INSTANCE, nbt).result();
    }

    /**
     * Serializes this record into an NbtCompound.
     * @return The NbtCompound representation of this data.
     */
    public NbtCompound toNbt() {
        // Use the 1.20.1 getOrThrow signature
        return (NbtCompound) getCodec().encodeStart(NbtOps.INSTANCE, this)
                .getOrThrow(false, error -> {
                    throw new IllegalStateException("Could not encode HamsterShoulderData: " + error);
                });
    }

    @Override
    public String toString() {
        return "HamsterShoulderData[uuid=" + entityUuid +
                ", variantId=" + variantId +
                ", health=" + health +
                ", inventoryNbt=" + inventoryNbt.toString().substring(0, Math.min(inventoryNbt.toString().length(), 50)) + "..." +
                ", age=" + breedingAge +
                ", throwCooldownEnd=" + throwCooldownEndTick +
                ", buffData=" + greenBeanBuffData +
                ", autoEatCooldown=" + autoEatCooldownTicks +
                ", customName=" + customName.orElse("None") +
                ", pinkPetalType=" + pinkPetalType +
                ", animationPersonalityId=" + animationPersonalityId +
                ", seekingBehaviorData=" + seekingBehaviorData +
                ", wanderModeData=" + wanderModeData +
                ", hamsterFlags=" + hamsterFlags +
                "]";
    }

    public static HamsterShoulderData empty() {
        return new HamsterShoulderData(UUID.randomUUID(), 0, 8.0f, new NbtCompound(), 0, 0L,
                GreenBeanBuffData.empty(), 0, Optional.empty(), 0, 1,
                SeekingBehaviorData.empty(), WanderModeData.empty(), 0
        );
    }
}