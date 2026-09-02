package net.dawson.adorablehamsterpets.item.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public final class HamsterBedItemModel extends GeoModel<HamsterBedItem> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "geo/hamster_bed");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        // This is handled by the renderer, so return a default fallback.
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_oak.png");
    }

    @Override
    public Identifier getAnimationResource(HamsterBedItem animatable) {
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "animations/anim_hamster_bed");
    }
}