package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.item.client.HamsterBedItemModel;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HamsterBedItemRenderer extends GeoItemRenderer<HamsterBedItem> {
    public HamsterBedItemRenderer() {
        super(new HamsterBedItemModel());
    }

    @Override
    public RenderLayer getRenderType(HamsterBedItem animatable, Identifier texture, @org.jetbrains.annotations.Nullable VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityCutout(getTextureLocation(animatable));
    }

    @Override
    public Identifier getTextureLocation(HamsterBedItem animatable) {
        WoodVariant variant = animatable.getVariant();
        return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_" + variant.asString() + ".png");
    }
}