package net.dawson.adorablehamsterpets.entity.client.renderer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterBlockHiderEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * A renderer for the invisible Block Hider proxy entity.
 * It overrides shouldRender to always return false, ensuring the entity is never drawn.
 */
public class HamsterBlockHiderRenderer extends EntityRenderer<HamsterBlockHiderEntity, EntityRenderState> {
    public HamsterBlockHiderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public boolean shouldRender(HamsterBlockHiderEntity entity, Frustum frustum, double x, double y, double z) {
        // Always return false to skip rendering entirely
        return false;
    }
}