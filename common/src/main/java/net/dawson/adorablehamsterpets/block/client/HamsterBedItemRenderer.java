package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.item.client.HamsterBedItemModel;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HamsterBedItemRenderer extends GeoItemRenderer<HamsterBedItem> {
    public HamsterBedItemRenderer() {
        super(new HamsterBedItemModel());
    }

    @Override
    public RenderType getRenderType(HamsterBedItem animatable, ResourceLocation texture, @org.jetbrains.annotations.Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(getTextureLocation(animatable));
    }

    @Override
    public ResourceLocation getTextureLocation(HamsterBedItem animatable) {
        WoodVariant variant = animatable.getVariant();
        return ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_" + variant.getSerializedName() + ".png");
    }
}