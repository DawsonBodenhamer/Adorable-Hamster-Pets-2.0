package net.dawson.adorablehamsterpets.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import java.util.concurrent.CompletableFuture;

public class ModRegistryDataGenerator extends FabricDynamicRegistryProvider {
    public ModRegistryDataGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, Entries entries) {
       entries.addAll(registries.lookupOrThrow(Registries.CONFIGURED_FEATURE));
       entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));

    }

    @Override
    public String getName() {
        return "";
    }
}
