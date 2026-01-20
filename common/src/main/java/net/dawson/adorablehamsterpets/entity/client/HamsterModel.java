package net.dawson.adorablehamsterpets.entity.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

@SuppressWarnings("removal") // Suppress deprecation warnings for the old abstract methods
public class HamsterModel extends GeoModel<HamsterEntity> {

    // --- 1. Constants for Scaling and Positioning ---
    private static final float ADULT_SCALE = 0.8f;
    private static final float ADULT_HEAD_SCALE = 1.0f;
    private static final float BABY_SCALE = 0.5f;
    private static final float BABY_HEAD_SCALE = 1.2f;
    // --- End 1. Constants ---

    @Override
    public Identifier getModelResource(HamsterEntity animatable) {
        return Identifier.of(AdorableHamsterPets.MOD_ID, "geo/hamster.geo.json");
    }

    @Override
    public Identifier getTextureResource(HamsterEntity animatable) {
        // This is just a fallback; the actual texture is set in the renderer
        return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/orange.png");
    }

    @Override
    public Identifier getAnimationResource(HamsterEntity animatable) {
        return Identifier.of(AdorableHamsterPets.MOD_ID, "animations/anim_hamster.animation.json");
    }

    @Override
    public void setCustomAnimations(HamsterEntity entity, long instanceId, AnimationState<HamsterEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        // --- Bone References ---
        CoreGeoBone rootBone = this.getAnimationProcessor().getBone("root");
        CoreGeoBone headParentBone = this.getAnimationProcessor().getBone("head_parent");
        CoreGeoBone leftCheekDefBone = this.getAnimationProcessor().getBone("left_cheek_deflated");
        CoreGeoBone rightCheekDefBone = this.getAnimationProcessor().getBone("right_cheek_deflated");
        CoreGeoBone leftCheekInfBone = this.getAnimationProcessor().getBone("left_cheek_inflated");
        CoreGeoBone rightCheekInfBone = this.getAnimationProcessor().getBone("right_cheek_inflated");
        CoreGeoBone rightEarBone = this.getAnimationProcessor().getBone("right_ear");
        CoreGeoBone acornHatBone = this.getAnimationProcessor().getBone("acorn_hat");
        CoreGeoBone petalHeadBone = this.getAnimationProcessor().getBone("pink_petal_head");
        CoreGeoBone petalSideBone = this.getAnimationProcessor().getBone("pink_petal_side");
        CoreGeoBone petalBackBone = this.getAnimationProcessor().getBone("pink_petal_lower_back");


        // --- Cheek Pouch Visibility Logic ---
        if (leftCheekDefBone != null && leftCheekInfBone != null) {
            boolean leftFull = entity.isLeftCheekFull();
            leftCheekDefBone.setHidden(leftFull);
            leftCheekInfBone.setHidden(!leftFull);
        }
        if (rightCheekDefBone != null && rightCheekInfBone != null) {
            boolean rightFull = entity.isRightCheekFull();
            rightCheekDefBone.setHidden(rightFull);
            rightCheekInfBone.setHidden(!rightFull);
        }

        // --- Armor/Accessory Visual Logic ---
        if (rightEarBone != null) {
            boolean shouldHideEar = false;
            boolean shouldShowHat = false;

            // 1. Check Bling Slot (Slot 6) - Highest Priority
            ItemStack blingStack = entity.getAccessoryStack();
            if (blingStack.isOf(ModItems.ACORN_HAT.get())) {
                shouldHideEar = true; // Prevent clipping through hat
                shouldShowHat = true;
            }

            // 2. Check Armor Slot (Slot 7) + Config
            // Only check if we haven't already decided to show the hat (bling overrides armor)
            ItemStack armorStack = entity.getArmorStack();
            if (armorStack.isOf(ModItems.HAMSTER_ARMOR_ACORN.get()) && Configs.AHP.renderAcornHat.get()) {
                // If wearing Acorn Armor AND config enables hat, hide ear and show hat.
                shouldHideEar = true;
                shouldShowHat = true;
            }

            // Apply visibility states
            rightEarBone.setHidden(shouldHideEar);

            if (acornHatBone != null) {
                // Default the hat bone to hidden unless specifically enabled.
                acornHatBone.setHidden(!shouldShowHat);
            }
        }

        // --- Scaling & Rotation Logic ---
        // bodyParentBone scale is intentionally not set here, allowing JSON breathing anims to work proportionally.
        if (rootBone != null && headParentBone != null) {
            // 1. Determine the base scale for the entire model and the head.
            float baseScale = entity.isBaby() ? BABY_SCALE : ADULT_SCALE;
            float headScale = entity.isBaby() ? BABY_HEAD_SCALE : ADULT_HEAD_SCALE;

            // 2. Start with the base scale for all axes.
            rootBone.setScaleX(baseScale);
            rootBone.setScaleY(baseScale);
            rootBone.setScaleZ(baseScale);

            // 3. If it's a shoulder pet, apply the dynamic squash/stretch by overriding just the Y-axis scale.
            if (entity.isShoulderPet()) {
                rootBone.setScaleY(baseScale * entity.dynamicScaleY);
            }

            // 4. Set the head scale independently.
            headParentBone.setScaleX(headScale);
            headParentBone.setScaleY(headScale);
            headParentBone.setScaleZ(headScale);

            // --- Dynamic Pitch Rotation ---
            // Rotates the root bone to match the trajectory
            if (entity.clientFallPitchProgress > 0.0f) {
                // 1. Calculate Cosine Interpolation (0.0 to 1.0)
                // Formula: (1 - cos(t * π)) / 2
                float t = entity.clientFallPitchProgress;
                float interpolated = (1.0f - MathHelper.cos(t * (float) Math.PI)) * 0.5f;

                // 2. Map to Target Angle (-90 degrees / -PI/2 radians)
                // Rotates the model to face downward
                float targetPitch = (float) (-Math.PI / 2.0);

                rootBone.setRotX(targetPitch * interpolated);
            } else {
                // Not falling -> Reset to neutral
                rootBone.setRotX(0);
            }
        }
    }
}