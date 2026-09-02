package net.dawson.adorablehamsterpets.entity.client.renderer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * A renderer for the invisible Tree Searcher proxy entity.
 * It overrides shouldRender to always return false, ensuring the entity is never drawn.
 */
public class HamsterTreeSearcherRenderer extends EntityRenderer<HamsterTreeSearcherEntity> {
    public HamsterTreeSearcherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTextureLocation(HamsterTreeSearcherEntity entity) {
        // Fallback texture, though it will never be used.
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/fur_pattern.png");
    }

    @Override
    public boolean shouldRender(HamsterTreeSearcherEntity entity, Frustum frustum, double x, double y, double z) {
        // Always return false to skip rendering entirely
        return false;
    }
}