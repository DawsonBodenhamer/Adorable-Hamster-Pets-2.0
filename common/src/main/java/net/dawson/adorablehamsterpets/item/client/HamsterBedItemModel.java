package net.dawson.adorablehamsterpets.item.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

@SuppressWarnings({"removal"})
public final class HamsterBedItemModel extends GeoModel<HamsterBedItem> {

    @Override
    public Identifier getModelResource(HamsterBedItem animatable, @Nullable GeoRenderer<HamsterBedItem> renderer) {
        return Identifier.of(AdorableHamsterPets.MOD_ID, "geo/hamster_bed.geo.json");
    }

    @Override
    public Identifier getTextureResource(HamsterBedItem animatable, @Nullable GeoRenderer<HamsterBedItem> renderer) {
        // This is handled by the renderer, so return a default fallback.
        return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_oak.png");
    }

    @Override
    public Identifier getAnimationResource(HamsterBedItem animatable) {
        return Identifier.of(AdorableHamsterPets.MOD_ID, "animations/anim_hamster_bed.animation.json");
    }

    // Deprecated, Abstract Method Implementations (Required by Compiler)
    @Deprecated(forRemoval = true)
    @Override
    public Identifier getModelResource(HamsterBedItem animatable) {
        // Delegate to the new, non-deprecated method.
        return this.getModelResource(animatable, null);
    }

    @Deprecated(forRemoval = true)
    @Override
    public Identifier getTextureResource(HamsterBedItem animatable) {
        // Delegate to the new, non-deprecated method.
        return this.getTextureResource(animatable, null);
    }
}