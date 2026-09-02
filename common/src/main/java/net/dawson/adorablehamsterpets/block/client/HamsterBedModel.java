package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoRenderer;

@SuppressWarnings({"removal"})
public class HamsterBedModel extends GeoModel<HamsterBedBlockEntity> {

    @Override
    public Identifier getModelResource(HamsterBedBlockEntity animatable, @Nullable GeoRenderer<HamsterBedBlockEntity> renderer) {
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "geo/hamster_bed.geo.json");
    }

    @Override
    public Identifier getTextureResource(HamsterBedBlockEntity animatable, @Nullable GeoRenderer<HamsterBedBlockEntity> renderer) {
        WoodVariant variant = animatable.getBlockState().getValue(HamsterBedBlock.WOOD_VARIANT);
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_" + variant.getSerializedName() + ".png");
    }

    @Override
    public Identifier getAnimationResource(HamsterBedBlockEntity animatable) {
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "animations/anim_hamster_bed.animation.json");
    }

    // Deprecated, Abstract Method Implementations (Required by Compiler)
    @Deprecated(forRemoval = true)
    @Override
    public Identifier getModelResource(HamsterBedBlockEntity animatable) {
        // Delegate to the new, non-deprecated method.
        return this.getModelResource(animatable, null);
    }

    @Deprecated(forRemoval = true)
    @Override
    public Identifier getTextureResource(HamsterBedBlockEntity animatable) {
        // Delegate to the new, non-deprecated method.
        return this.getTextureResource(animatable, null);
    }
}