package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.item.client.HamsterBedItemModel;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.GeoItemRenderer;

public class HamsterBedItemRenderer extends GeoItemRenderer<HamsterBedItem> {
    public HamsterBedItemRenderer() {
        super(new HamsterBedItemModel());
    }

    @Override
    public RenderType getRenderType(HamsterBedItem animatable, Identifier texture, @org.jetbrains.annotations.Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(getTextureLocation(animatable));
    }

    @Override
    public Identifier getTextureLocation(HamsterBedItem animatable) {
        WoodVariant variant = animatable.getVariant();
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_" + variant.getSerializedName() + ".png");
    }
}