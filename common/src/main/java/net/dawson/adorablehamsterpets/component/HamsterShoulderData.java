package net.dawson.adorablehamsterpets.component;

/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.Optional;
import java.util.UUID;

// Data-holder record
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
        int hamsterFlags // Replaces all boolean flags
) {

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
            NbtCompound activeEffectsNbt
    ) {
        public static final Codec<GreenBeanBuffData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("greenBeanBuffEndTick").orElse(0L).forGetter(GreenBeanBuffData::greenBeanBuffEndTick),
                        Codec.LONG.fieldOf("greenBeanBuffDuration").orElse(0L).forGetter(GreenBeanBuffData::greenBeanBuffDuration),
                        NBT_COMPOUND_CODEC.fieldOf("activeEffectsNbt").forGetter(GreenBeanBuffData::activeEffectsNbt)
                ).apply(instance, GreenBeanBuffData::new)
        );

        public static GreenBeanBuffData empty() {
            return new GreenBeanBuffData(0L, 0L, new NbtCompound());
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
     * Serializes this record into an NbtCompound.
     * @return The NbtCompound representation of this data.
     */
    public NbtCompound toNbt() {
        return (NbtCompound) getCodec().encodeStart(NbtOps.INSTANCE, this)
                .getOrThrow(error -> new IllegalStateException("Could not encode HamsterShoulderData: " + error));
    }

    /**
     * Deserializes an NbtCompound into a HamsterShoulderData record.
     * @param nbt The NbtCompound to read from.
     * @return An Optional containing the deserialized data, or empty if deserialization fails.
     */
    public static Optional<HamsterShoulderData> fromNbt(NbtCompound nbt) {
        return getCodec().parse(NbtOps.INSTANCE, nbt)
                .resultOrPartial(AdorableHamsterPets.LOGGER::error);
    }
}