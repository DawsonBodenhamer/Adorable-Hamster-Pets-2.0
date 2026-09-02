package net.dawson.adorablehamsterpets.entity.client.renderer;

import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import com.geckolib.cache.model.BakedGeoModel;

/**
 * A specialized renderer for the shoulder-mounted hamster.
 * It extends the base HamsterRenderer but overrides methods to suppress
 * sounds, particles, and other world-interactive effects that are not
 * needed for a purely cosmetic render.
 */
public class ShoulderHamsterRenderer extends HamsterRenderer {

    public ShoulderHamsterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    // 26.2 port: the render() override only set shadowRadius by age, and
    // HamsterRenderer.extractRenderState already does exactly that (0.2 adult,
    // 0.1 baby), so nothing is overridden here any more.
}