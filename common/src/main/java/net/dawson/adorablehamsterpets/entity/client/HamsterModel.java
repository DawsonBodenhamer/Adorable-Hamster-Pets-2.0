package net.dawson.adorablehamsterpets.entity.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import java.util.Locale;

@SuppressWarnings("removal")
public class HamsterModel extends GeoModel<HamsterEntity> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final float ADULT_SCALE = 0.8f;
    private static final float ADULT_HEAD_SCALE = 1.0f;
    private static final float BABY_SCALE = 0.5f;
    private static final float BABY_HEAD_SCALE = 1.2f;

    private static final Identifier MODEL_RESOURCE = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "geo/hamster.geo.json");
    private static final Identifier ANIMATION_RESOURCE = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "animations/anim_hamster.animation.json");
    private static final Identifier FALLBACK_TEXTURE = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/fur_pattern.png");

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL_RESOURCE;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        // Fallback texture; the renderer resolves the real one per hamster
        return FALLBACK_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(HamsterEntity animatable) {
        return ANIMATION_RESOURCE;
    }

    // Deprecated methods required by superclass
    @Deprecated(forRemoval = true)
    @Override
    public Identifier getModelResource(HamsterEntity animatable) {
        return this.getModelResource(animatable, null);
    }

    @Deprecated(forRemoval = true)
    @Override
    public Identifier getTextureResource(HamsterEntity animatable) {
        return this.getTextureResource(animatable, null);
    }
}
