package net.dawson.adorablehamsterpets.component;

/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.registry.registries.DeferredRegister;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;
import java.util.function.UnaryOperator;

public class ModDataComponentTypes {

    public static final DeferredRegister<ComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.DATA_COMPONENT_TYPE);

    public static final ComponentType<UUID> LINKED_HAMSTER_UUID =
            register("linked_hamster_uuid", builder -> builder.codec(Uuids.CODEC).cache());

    public static final ComponentType<Text> LINKED_HAMSTER_NAME =
            register("linked_hamster_name", builder -> builder.codec(TextCodecs.CODEC).cache());

    public static final ComponentType<WanderDistance> WANDER_DISTANCE =
            register("wander_distance", builder -> builder.codec(WanderDistance.CODEC).cache());

    public static final ComponentType<WoodVariant> WOOD_VARIANT =
            register("wood_variant", builder -> builder.codec(WoodVariant.CODEC).cache());

    private static <T> ComponentType<T> register(String name, UnaryOperator<ComponentType.Builder<T>> builderUnaryOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(AdorableHamsterPets.MOD_ID, name),
                builderUnaryOperator.apply(ComponentType.<T>builder()).build());
    }

    public static void registerDataComponentTypes() {
        DATA_COMPONENT_TYPES.register();
        AdorableHamsterPets.LOGGER.info("Registering Data Component Types for " + AdorableHamsterPets.MOD_ID);
    }
}