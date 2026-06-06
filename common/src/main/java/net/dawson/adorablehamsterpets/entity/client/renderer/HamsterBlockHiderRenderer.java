package net.dawson.adorablehamsterpets.entity.client.renderer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterBlockHiderEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

/**
 * A renderer for the invisible Block Hider proxy entity.
 * It overrides shouldRender to always return false, ensuring the entity is never drawn.
 */
public class HamsterBlockHiderRenderer extends EntityRenderer<HamsterBlockHiderEntity> {
    public HamsterBlockHiderRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(HamsterBlockHiderEntity entity) {
        return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/fur_pattern.png");
    }

    @Override
    public boolean shouldRender(HamsterBlockHiderEntity entity, Frustum frustum, double x, double y, double z) {
        // Always return false to skip rendering entirely
        return false;
    }
}