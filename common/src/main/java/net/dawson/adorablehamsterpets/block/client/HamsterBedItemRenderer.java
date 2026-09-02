package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.item.client.HamsterBedItemModel;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.minecraft.resources.Identifier;

public class HamsterBedItemRenderer extends GeoItemRenderer<HamsterBedItem> {

    private static final DataTicket<Identifier> BED_TEXTURE =
            DataTicket.create("adorablehamsterpets:bed_item_texture", Identifier.class);
    private static final Identifier FALLBACK =
            Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_oak.png");

    public HamsterBedItemRenderer() {
        super(new HamsterBedItemModel());
    }


    /** The wood variant lives on the item, so resolve the texture while the item is in reach. */
    @Override
    public void addRenderData(HamsterBedItem item, RenderData renderData, GeoRenderState state, float partialTick) {
        super.addRenderData(item, renderData, state, partialTick);
        WoodVariant variant = item.getVariant();
        state.addGeckolibData(BED_TEXTURE, Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID,
                "textures/block/hamster_bed_" + variant.getSerializedName() + ".png"));
    }

    @Override
    public Identifier getTextureLocation(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(BED_TEXTURE, FALLBACK);
    }
}
