package net.dawson.adorablehamsterpets.entity.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the Redstone Fever eye mask at full brightness.
 *
 * <p>26.2 port: rendering no longer touches the entity. Minecraft builds a render
 * state first and draws from that, so whether the hamster currently has the fever
 * is captured in {@link #addRenderData} and read back through a data ticket at
 * draw time. GeckoLib 5 also ships {@link AutoGlowingGeoLayer}, which already does
 * the emissive pass this class used to hand-roll with {@code RenderType.eyes}.
 */
public final class RedstoneFeverEyesRenderLayer<R extends EntityRenderState & GeoRenderState>
        extends AutoGlowingGeoLayer<HamsterEntity, Void, R> {

    /** Set per-frame in {@link #addRenderData}; read at draw time when the entity is out of reach. */
    public static final DataTicket<Boolean> SHOW_FEVER_EYES =
            DataTicket.create("adorablehamsterpets:show_fever_eyes", Boolean.class);

    private static final Identifier FEVER_EYES_TEXTURE = Identifier.fromNamespaceAndPath(
            "adorablehamsterpets",
            "textures/entity/hamster/appearance/conditions/redstone_fever/eyes.png");

    public RedstoneFeverEyesRenderLayer(GeoRenderer<HamsterEntity, Void, R> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(HamsterEntity animatable, Void relatedObject, R renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);

        renderState.addGeckolibData(SHOW_FEVER_EYES,
                animatable.hasRedstoneFever() && !animatable.isInvisible());
    }

    /** Fixed mask texture, rather than the emissive-variant lookup the base class does. */
    @Override
    protected Identifier getTextureResource(R renderState) {
        return FEVER_EYES_TEXTURE;
    }

    @Override
    public void submitRenderTask(RenderPassInfo<R> renderPass, SubmitNodeCollector collector) {
        if (!renderPass.renderState().getOrDefaultGeckolibData(SHOW_FEVER_EYES, false)) {
            return;
        }

        super.submitRenderTask(renderPass, collector);
    }
}
