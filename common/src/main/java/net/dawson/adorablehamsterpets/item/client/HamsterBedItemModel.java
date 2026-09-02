package net.dawson.adorablehamsterpets.item.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

@SuppressWarnings({"removal"})
public final class HamsterBedItemModel extends GeoModel<HamsterBedItem> {

    @Override
    public ResourceLocation getModelResource(HamsterBedItem animatable, @Nullable GeoRenderer<HamsterBedItem> renderer) {
        return ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "geo/hamster_bed.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HamsterBedItem animatable, @Nullable GeoRenderer<HamsterBedItem> renderer) {
        // This is handled by the renderer, so return a default fallback.
        return ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_oak.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HamsterBedItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "animations/anim_hamster_bed.animation.json");
    }

    // Deprecated, Abstract Method Implementations (Required by Compiler)
    @Deprecated(forRemoval = true)
    @Override
    public ResourceLocation getModelResource(HamsterBedItem animatable) {
        // Delegate to the new, non-deprecated method.
        return this.getModelResource(animatable, null);
    }

    @Deprecated(forRemoval = true)
    @Override
    public ResourceLocation getTextureResource(HamsterBedItem animatable) {
        // Delegate to the new, non-deprecated method.
        return this.getTextureResource(animatable, null);
    }
}