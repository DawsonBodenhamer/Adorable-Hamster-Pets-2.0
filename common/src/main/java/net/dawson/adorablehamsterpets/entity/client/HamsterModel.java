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
import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.AnimationState;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoRenderer;

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
    public Identifier getModelResource(HamsterEntity animatable, @Nullable GeoRenderer<HamsterEntity> renderer) {
        return MODEL_RESOURCE;
    }

    @Override
    public Identifier getTextureResource(HamsterEntity animatable, @Nullable GeoRenderer<HamsterEntity> renderer) {
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
        if (AdorableHamsterPetsClient.isPerformanceModeEnabled) {
            // Hide everything except absolute essentials
            for (GeoBone bone : processor.getRegisteredBones()) {
                String name = bone.getName();
                boolean keepVisible = name.equals("root")
                        || name.equals("body_parent")
                        || name.equals("body_child");
                bone.setHidden(!keepVisible);
            }

            return; // Skip all other visual calculations
        } else {
            // Restore visibility to all bones when performance mode off
            for (GeoBone bone : processor.getRegisteredBones()) {
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
        var flowerHeadNoArmorBone = processor.getBone("flower_head_no_armor");
        var flowerSideNoArmorBone = processor.getBone("flower_side_no_armor");
        var flowerBackNoArmorBone = processor.getBone("flower_lower_back_no_armor");
        var flowerHeadWithArmorBone = processor.getBone("flower_head_with_armor");
        var flowerSideWithArmorBone = processor.getBone("flower_side_with_armor");
        var flowerBackWithArmorBone = processor.getBone("flower_lower_back_with_armor");

        // --- Statue / AI Disabled Logic ---
        var closedEyesBone = processor.getBone("closed_eyes");
        if (closedEyesBone != null) {
            closedEyesBone.setHidden(entity.isNoAi()); // Ensure eyes remain open in t-pose
        }

        // --- Easter Egg Logic ---
        boolean isMoonwalking = entity.isMoonwalking();

        // --- Equipment State ---
        ItemStack armorStack = entity.getArmorStack();
        boolean isArmorVisible = Configs.AHP_MAIN.enableArmorVisuals
                && entity.isArmorVisible()
                && !armorStack.isEmpty()
                && armorStack.getItem() instanceof HamsterArmorItem;

        // --- Pink Petal Logic ---
        int flowerType = entity.getEntityData().get(HamsterEntity.FLOWER_POS);
        boolean useArmorFlowers = isArmorVisible && Configs.AHP_MAIN.renderFlowersWithArmor.get();

        if (flowerHeadNoArmorBone != null) flowerHeadNoArmorBone.setHidden(flowerType != 1 || useArmorFlowers);
        if (flowerSideNoArmorBone != null) flowerSideNoArmorBone.setHidden(flowerType != 2 || useArmorFlowers);
        if (flowerBackNoArmorBone != null) flowerBackNoArmorBone.setHidden(flowerType != 3 || useArmorFlowers);

        if (flowerHeadWithArmorBone != null) flowerHeadWithArmorBone.setHidden(flowerType != 1 || !useArmorFlowers);
        if (flowerSideWithArmorBone != null) flowerSideWithArmorBone.setHidden(flowerType != 2 || !useArmorFlowers);
        if (flowerBackWithArmorBone != null) flowerBackWithArmorBone.setHidden(flowerType != 3 || !useArmorFlowers);

        // --- Cheek Pouch Logic ---
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

        // --- Armor/Accessory Logic ---
        if (rightEarBone != null) {
            boolean shouldHideEar = false;
            boolean shouldShowHat = false;

            // Check bling slot 6 for highest priority
            ItemStack blingStack = entity.getAccessoryStack();
            if (blingStack.is(ModItems.ACORN_HAT.get())) {
                shouldHideEar = true; // Prevent clipping through hat
                shouldShowHat = true;
            }

            // Check armor slot 7 and config if not already showing hat
            if (!shouldShowHat
                    && isArmorVisible
                    && armorStack.is(ModItems.HAMSTER_ARMOR_ACORN.get())
                    && Configs.AHP_MAIN.renderAcornHat.get()) {
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
                int exactAge = entity.getEntityData().get(HamsterEntity.EXACT_AGE);
                ageProgress = 1.0f - (Math.abs(exactAge) / 24000.0f);
            }

            // Smoothly lerp between baby and adult scales
            float currentBaseScale = Mth.lerp(ageProgress, BABY_SCALE, ADULT_SCALE);
            float currentHeadScale = Mth.lerp(ageProgress, BABY_HEAD_SCALE, ADULT_HEAD_SCALE);

            rootBone.setScaleX(currentBaseScale);
            rootBone.setScaleZ(currentBaseScale);

            // Override y scale for dynamic squash and stretch if shoulder pet
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
            if (entity.isProjectileDummy) {
                // --- Projectile Mode ---
                // Align with velocity vector (follow flight arc)
                Vec3 velocity = entity.getDeltaMovement();
                double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

                // Calculate pitch: Positive RotX = Nose Up, Negative RotX = Nose Down
                pitchOffset = (float) Math.atan2(velocity.y, horizontalSpeed);
            } else if (entity.isInWater() || entity.isInLava()) {
                // --- Swim Mode ---
                // Use pre-smoothed pitch to eliminate buoyancy RNG flickering
                float partialTick = animationState.getPartialTick();
                pitchOffset = Mth.lerp(partialTick, entity.prevClientSwimPitch, entity.clientSwimPitch);
            } else if (entity.clientFallPitchProgress > 0.0f || entity.prevClientFallPitchProgress > 0.0f) {
                float partialTick = animationState.getPartialTick();
                float lerpedProgress = Mth.lerp(partialTick, entity.prevClientFallPitchProgress, entity.clientFallPitchProgress);

                // Natural Fall Mode: Procedural Nose Dive (Cosine Interpolation)
                float interpolated = (1.0f - Mth.cos(lerpedProgress * (float) Math.PI)) * 0.5f;

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
