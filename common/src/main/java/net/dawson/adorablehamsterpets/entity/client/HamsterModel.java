package net.dawson.adorablehamsterpets.entity.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("removal")
public class HamsterModel extends GeoModel<HamsterEntity> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final float ADULT_SCALE = 0.8f;
    private static final float ADULT_HEAD_SCALE = 1.0f;
    private static final float BABY_SCALE = 0.5f;
    private static final float BABY_HEAD_SCALE = 1.2f;

    private static final Identifier MODEL_RESOURCE = Identifier.of(AdorableHamsterPets.MOD_ID, "geo/hamster.geo.json");
    private static final Identifier ANIMATION_RESOURCE = Identifier.of(AdorableHamsterPets.MOD_ID, "animations/anim_hamster.animation.json");
    private static final Identifier FALLBACK_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/fur_pattern.png");

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public Identifier getModelResource(HamsterEntity animatable) {
        return MODEL_RESOURCE;
    }

    @Override
    public Identifier getTextureResource(HamsterEntity animatable) {
        // Fallback texture; actual texture handled by renderer
        return FALLBACK_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(HamsterEntity animatable) {
        return ANIMATION_RESOURCE;
    }

    @Override
    public void setCustomAnimations(HamsterEntity entity, long instanceId, AnimationState<HamsterEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        var processor = this.getAnimationProcessor();

        // --- Performance Mode ---
        if (Configs.AHP.performanceMode) {
            // Hide everything except absolute essentials
            for (CoreGeoBone bone : processor.getRegisteredBones()) {
                String name = bone.getName();
                boolean keepVisible = name.equals("root")
                        || name.equals("body_parent")
                        || name.equals("body_child");
                bone.setHidden(!keepVisible);
            }

//            // Apply basic scaling
//            var rootBone = processor.getBone("root");
//            var headParentBone = processor.getBone("head_parent");
//            if (rootBone != null && headParentBone != null) {
//                float ageProgress = 1.0f;
//                if (entity.isBaby()) {
//                    int exactAge = entity.getDataTracker().get(HamsterEntity.EXACT_AGE);
//                    ageProgress = 1.0f - (Math.abs(exactAge) / 24000.0f);
//                }
//                float currentBaseScale = MathHelper.lerp(ageProgress, BABY_SCALE, ADULT_SCALE);
//                float currentHeadScale = MathHelper.lerp(ageProgress, BABY_HEAD_SCALE, ADULT_HEAD_SCALE);
//
//                rootBone.setScaleX(currentBaseScale);
//                rootBone.setScaleZ(currentBaseScale);
//                rootBone.setScaleY(entity.isShoulderPet() ? currentBaseScale * entity.dynamicScaleY : currentBaseScale);
//
//                headParentBone.setScaleX(currentHeadScale);
//                headParentBone.setScaleY(currentHeadScale);
//                headParentBone.setScaleZ(currentHeadScale);
//            }
            return; // Skip all other visual calculations
        } else {
            // Restore visibility to all bones when performance mode off
            for (CoreGeoBone bone : processor.getRegisteredBones()) {
                bone.setHidden(false);
            }
        }

        // --- Normal Mode ---
        // --- Bone References ---
        var rootBone = processor.getBone("root");
        var headParentBone = processor.getBone("head_parent");
        var leftCheekDefBone = processor.getBone("left_cheek_deflated");
        var rightCheekDefBone = processor.getBone("right_cheek_deflated");
        var leftCheekInfBone = processor.getBone("left_cheek_inflated");
        var rightCheekInfBone = processor.getBone("right_cheek_inflated");
        var rightEarBone = processor.getBone("right_ear");
        var acornHatBone = processor.getBone("acorn_hat");
        var petalHeadBone = processor.getBone("pink_petal_head");
        var petalSideBone = processor.getBone("pink_petal_side");
        var petalBackBone = processor.getBone("pink_petal_lower_back");

        // --- Statue / AI Disabled Logic ---
        var closedEyesBone = processor.getBone("closed_eyes");
        if (closedEyesBone != null) {
            closedEyesBone.setHidden(entity.isAiDisabled()); // Ensure eyes remain open in t-pose
        }

        // --- Easter Egg Logic ---
        boolean isMoonwalking = entity.isMoonwalking();

        // --- Pink Petal Visibility Defaults ---
        // 1.20.1: Hide petal bones by default to prevent rendering state leaks
        if (petalHeadBone != null) petalHeadBone.setHidden(true);
        if (petalSideBone != null) petalSideBone.setHidden(true);
        if (petalBackBone != null) petalBackBone.setHidden(true);

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

            // Check bling slot 6 for highest priority
            ItemStack blingStack = entity.getAccessoryStack();
            if (blingStack.isOf(ModItems.ACORN_HAT.get())) {
                shouldHideEar = true; // Prevent clipping through hat
                shouldShowHat = true;
            }

            // Check armor slot 7 and config if not already showing hat
            ItemStack armorStack = entity.getArmorStack();
            if (!shouldShowHat && armorStack.isOf(ModItems.HAMSTER_ARMOR_ACORN.get()) && Configs.AHP.renderAcornHat.get()) {
                shouldHideEar = true;
                shouldShowHat = true;
            }

            rightEarBone.setHidden(shouldHideEar);

            if (acornHatBone != null) {
                acornHatBone.setHidden(!shouldShowHat);
            }
        }

        // --- Scaling & Rotation Logic ---
        // bodyParentBone scale intentionally not set here so json breathing anims scale proportionally
        if (rootBone != null && headParentBone != null) {
            // Calculate continuous growth progress (0.0 = newborn, 1.0 = adult)
            float ageProgress = 1.0f;
            if (entity.isBaby()) {
                int exactAge = entity.getDataTracker().get(HamsterEntity.EXACT_AGE);
                ageProgress = 1.0f - (Math.abs(exactAge) / 24000.0f);
            }

            // Smoothly lerp between baby and adult scales
            float currentBaseScale = MathHelper.lerp(ageProgress, BABY_SCALE, ADULT_SCALE);
            float currentHeadScale = MathHelper.lerp(ageProgress, BABY_HEAD_SCALE, ADULT_HEAD_SCALE);

            rootBone.setScaleX(currentBaseScale);
            rootBone.setScaleZ(currentBaseScale);

            // override y scale for dynamic squash and stretch if shoulder pet
            if (entity.isShoulderPet()) {
                rootBone.setScaleY(currentBaseScale * entity.dynamicScaleY);
            } else {
                rootBone.setScaleY(currentBaseScale);
            }

            headParentBone.setScaleX(currentHeadScale);
            headParentBone.setScaleY(currentHeadScale);
            headParentBone.setScaleZ(currentHeadScale);

            float pitchOffset = 0.0f;

            // --- Dynamic Pitch Rotation ---
            if (entity.isThrown()) {
                // Projectile Mode: Align with velocity vector (follow flight arc)
                Vec3d velocity = entity.getVelocity();
                double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

                // Calculate pitch: Positive RotX = Nose Up, Negative RotX = Nose Down
                pitchOffset = (float) Math.atan2(velocity.y, horizontalSpeed);
            } else if (entity.clientFallPitchProgress > 0.0f || entity.prevClientFallPitchProgress > 0.0f) {
                float partialTick = animationState.getPartialTick();
                float lerpedProgress = MathHelper.lerp(partialTick, entity.prevClientFallPitchProgress, entity.clientFallPitchProgress);

                // Natural Fall Mode: Procedural Nose Dive (Cosine Interpolation)
                float interpolated = (1.0f - MathHelper.cos(lerpedProgress * (float) Math.PI)) * 0.5f;

                // Rotate to face downward
                pitchOffset = (float) (-Math.PI / 2.0) * interpolated;
            }

            // Absolute assignment
            rootBone.setRotX(pitchOffset);

            // Easter Egg rotation if applicable
            if (isMoonwalking) {
                rootBone.setRotY((float) Math.PI);
            } else {
                rootBone.setRotY(0.0f);
            }
        }
    }
}