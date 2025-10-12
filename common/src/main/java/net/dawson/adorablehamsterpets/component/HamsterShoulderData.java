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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public record HamsterShoulderData(
        int variantId,
        float health,
        NbtCompound inventoryNbt,
        int breedingAge,
        long throwCooldownEndTick,
        long greenBeanBuffEndTick,
        long greenBeanBuffDuration,
        NbtList activeEffectsNbt, // Changed from NbtCompound to NbtList in 1.20.1
        int autoEatCooldownTicks,
        Optional<String> customName,
        int pinkPetalType,
        int animationPersonalityId,
        SeekingBehaviorData seekingBehaviorData,
        int hamsterFlags // Replaces all boolean flags
) {

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

    public static final Codec<HamsterShoulderData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("variantId").forGetter(HamsterShoulderData::variantId),
                    Codec.FLOAT.fieldOf("health").forGetter(HamsterShoulderData::health),
                    NBT_COMPOUND_CODEC.fieldOf("inventoryNbt").forGetter(HamsterShoulderData::inventoryNbt),
                    Codec.INT.fieldOf("breedingAge").forGetter(HamsterShoulderData::breedingAge),
                    Codec.LONG.fieldOf("throwCooldownEndTick").forGetter(HamsterShoulderData::throwCooldownEndTick),
                    Codec.LONG.fieldOf("greenBeanBuffEndTick").forGetter(HamsterShoulderData::greenBeanBuffEndTick),
                    Codec.LONG.fieldOf("greenBeanBuffDuration").orElse(0L).forGetter(HamsterShoulderData::greenBeanBuffDuration),
                    NBT_LIST_CODEC.fieldOf("activeEffectsNbt").forGetter(HamsterShoulderData::activeEffectsNbt), // Now expects a list in 1.20.1
                    Codec.INT.fieldOf("autoEatCooldownTicks").forGetter(HamsterShoulderData::autoEatCooldownTicks),
                    Codec.STRING.optionalFieldOf("customName").forGetter(HamsterShoulderData::customName),
                    Codec.INT.fieldOf("pinkPetalType").orElse(0).forGetter(HamsterShoulderData::pinkPetalType),
                    Codec.INT.fieldOf("animationPersonalityId").orElse(1).forGetter(HamsterShoulderData::animationPersonalityId),
                    SeekingBehaviorData.CODEC.fieldOf("seekingBehaviorData").orElse(SeekingBehaviorData.empty()).forGetter(HamsterShoulderData::seekingBehaviorData),
                    Codec.INT.fieldOf("hamsterFlags").orElse(0).forGetter(HamsterShoulderData::hamsterFlags)).apply(instance, HamsterShoulderData::new)
    );

    /**
     * Deserializes an NbtCompound into a HamsterShoulderData record.
     * @param nbt The NbtCompound to deserialize.
     * @return An Optional containing the HamsterShoulderData, or empty if deserialization fails.
     */
    public static Optional<HamsterShoulderData> fromNbt(NbtCompound nbt) {
        return CODEC.parse(NbtOps.INSTANCE, nbt).result();
    }

    /**
     * Serializes this record into an NbtCompound.
     * @return The NbtCompound representation of this data.
     */
    public NbtCompound toNbt() {
        // Use the 1.20.1 getOrThrow signature which takes a Consumer<String>
        return (NbtCompound) CODEC.encodeStart(NbtOps.INSTANCE, this)
                .getOrThrow(false, error -> {
                    throw new IllegalStateException("Could not encode HamsterShoulderData: " + error);
                });
    }

    @Override
    public String toString() {
        return "HamsterShoulderData[variantId=" + variantId +
                ", health=" + health +
                ", inventoryNbt=" + inventoryNbt.toString().substring(0, Math.min(inventoryNbt.toString().length(), 50)) + "..." +
                ", age=" + breedingAge +
                ", throwCooldownEnd=" + throwCooldownEndTick +
                ", beansCooldownEnd=" + greenBeanBuffEndTick +
                ", effectsNbtCount=" + activeEffectsNbt.size() +
                ", autoEatCooldown=" + autoEatCooldownTicks +
                ", customName=" + customName.orElse("None") +
                ", pinkPetalType=" + pinkPetalType +
                ", animationPersonalityId=" + animationPersonalityId +
                ", seekingBehaviorData=" + seekingBehaviorData.toString() +
                ", animationPersonalityId=" + hamsterFlags +
                "]";
    }

    public static HamsterShoulderData empty() {
        return new HamsterShoulderData(0, 8.0f, new NbtCompound(), 0, 0L, 0L, 0L, new NbtList(), 0, Optional.empty(), 0, 1, SeekingBehaviorData.empty(), 0
        );
    }
}