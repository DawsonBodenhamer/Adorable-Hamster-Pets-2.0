package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

@SuppressWarnings({"removal"})
public class HamsterBedModel extends GeoModel<HamsterBedBlockEntity> {

    @Override
    public ResourceLocation getModelResource(HamsterBedBlockEntity animatable, @Nullable GeoRenderer<HamsterBedBlockEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "geo/hamster_bed.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HamsterBedBlockEntity animatable, @Nullable GeoRenderer<HamsterBedBlockEntity> renderer) {
        WoodVariant variant = animatable.getBlockState().getValue(HamsterBedBlock.WOOD_VARIANT);
        return ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_" + variant.getSerializedName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(HamsterBedBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "animations/anim_hamster_bed.animation.json");
    }

    // Deprecated, Abstract Method Implementations (Required by Compiler)
    @Deprecated(forRemoval = true)
    @Override
    public ResourceLocation getModelResource(HamsterBedBlockEntity animatable) {
        // Delegate to the new, non-deprecated method.
        return this.getModelResource(animatable, null);
    }

    @Deprecated(forRemoval = true)
    @Override
    public ResourceLocation getTextureResource(HamsterBedBlockEntity animatable) {
        // Delegate to the new, non-deprecated method.
        return this.getTextureResource(animatable, null);
    }
}