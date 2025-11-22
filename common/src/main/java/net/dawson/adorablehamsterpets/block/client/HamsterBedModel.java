package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings({"removal"})
public class HamsterBedModel extends GeoModel<HamsterBedBlockEntity> {

    @Override
    public Identifier getModelResource(HamsterBedBlockEntity animatable) {
        return new Identifier(AdorableHamsterPets.MOD_ID, "geo/hamster_bed.geo.json");
    }

    @Override
    public Identifier getTextureResource(HamsterBedBlockEntity animatable) {
        // In 1.20.1, access the state directly from the block entity
        WoodVariant variant = animatable.getCachedState().get(HamsterBedBlock.WOOD_VARIANT);
        return new Identifier(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_" + variant.asString() + ".png");
    }

    @Override
    public Identifier getAnimationResource(HamsterBedBlockEntity animatable) {
        return Identifier.of(AdorableHamsterPets.MOD_ID, "animations/anim_hamster_bed.animation.json");
    }
}