package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.model.GeoModel;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.world.level.block.state.BlockState;

public class HamsterBedModel extends GeoModel<HamsterBedBlockEntity> {

    /** 26.2 port: the block state is out of reach at draw time, so the variant is captured here. */
    private static final DataTicket<WoodVariant> WOOD_VARIANT =
            DataTicket.create("adorablehamsterpets:bed_wood_variant", WoodVariant.class);

    @Override
    public void addAdditionalStateData(HamsterBedBlockEntity animatable, Object relatedObject, GeoRenderState renderState) {
        super.addAdditionalStateData(animatable, relatedObject, renderState);
        BlockState state = animatable.getBlockState();
        if (state.hasProperty(HamsterBedBlock.WOOD_VARIANT)) {
            renderState.addGeckolibData(WOOD_VARIANT, state.getValue(HamsterBedBlock.WOOD_VARIANT));
        }
    }


    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_bed");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        WoodVariant variant = renderState.getOrDefaultGeckolibData(WOOD_VARIANT, WoodVariant.OAK);
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/block/hamster_bed_" + variant.getSerializedName() + ".png");
    }

    @Override
    public Identifier getAnimationResource(HamsterBedBlockEntity animatable) {
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "anim_hamster_bed");
    }


}