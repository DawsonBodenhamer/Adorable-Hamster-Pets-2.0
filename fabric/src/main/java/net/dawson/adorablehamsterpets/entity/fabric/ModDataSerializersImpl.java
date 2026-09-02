package net.dawson.adorablehamsterpets.entity.fabric;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.Identifier;

public final class ModDataSerializersImpl {
    private ModDataSerializersImpl() {}

    public static void registerPlatform(String name, EntityDataSerializer<?> serializer) {
        FabricEntityDataRegistry.register(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, name), serializer);
    }
}
