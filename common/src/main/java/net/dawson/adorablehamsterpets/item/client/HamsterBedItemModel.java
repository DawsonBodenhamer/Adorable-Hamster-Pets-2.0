package net.dawson.adorablehamsterpets.item.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings({"removal"})
public final class HamsterBedItemModel extends GeoModel<HamsterBedItem> {

    @Override
    public Identifier getModelResource(HamsterBedItem animatable) {
        return new Identifier(AdorableHamsterPets.MOD_ID, "geo/hamster_bed.geo.json");
    }

    @Override
    public Identifier getTextureResource(HamsterBedItem animatable) {
        // Fallback texture; the actual texture is determined by the Renderer
        // In 1.20.1, access the state directly from the block entity
        return new Identifier(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_oak.png");
    }

    @Override
    public Identifier getAnimationResource(HamsterBedItem animatable) {
        return Identifier.of(AdorableHamsterPets.MOD_ID, "animations/anim_hamster_bed.animation.json");
    }
}