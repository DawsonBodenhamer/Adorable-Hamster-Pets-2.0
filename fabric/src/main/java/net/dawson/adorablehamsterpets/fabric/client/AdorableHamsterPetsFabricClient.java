package net.dawson.adorablehamsterpets.fabric.client;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.fabricmc.api.ClientModInitializer;

public final class AdorableHamsterPetsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AdorableHamsterPetsClient.init();
        AdorableHamsterPetsClient.initScreenHandlers();
        AdorableHamsterPetsClient.initEntityRenderers();

        // --- Register keybindings for Fabric ---
        ModKeyBindings.init();
        KeyMappingRegistry.register(ModKeyBindings.THROW_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.DISMOUNT_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY);
    }
}