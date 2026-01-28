package net.dawson.adorablehamsterpets.entity.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.sound.HamsterCleaningSoundInstance;
import net.dawson.adorablehamsterpets.entity.client.layer.HamsterAcornHatLayer;
import net.dawson.adorablehamsterpets.entity.client.layer.HamsterArmorLayer;
import net.dawson.adorablehamsterpets.entity.client.layer.HamsterOverlayLayer;
import net.dawson.adorablehamsterpets.entity.client.layer.HamsterPinkPetalOverlayLayer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterVariant;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMouthItemOffsets;
import net.dawson.adorablehamsterpets.util.HamsterSeatOffsets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

public class HamsterRenderer extends GeoEntityRenderer<HamsterEntity> {

    private final float adultShadowRadius;
    private static final Map<Integer, HamsterCleaningSoundInstance> activeCleaningSounds = new HashMap<>();

    /**
     * Used by a LivingEntityRenderer mixin to suppress vanilla passenger rendering and avoid double-draw.
     * True only while intentionally rendering the rider here.
     */
    public static final ThreadLocal<Boolean> IS_RENDERING_PASSENGER = ThreadLocal.withInitial(() -> false);

    private static final String SEAT_BONE = "body_child";

    public HamsterRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new HamsterModel());
        this.adultShadowRadius = 0.2F;
        this.shadowRadius = this.adultShadowRadius;

        // --- Physical Attributes ---
        addRenderLayer(new HamsterOverlayLayer(this));

        // --- Armor & Accessories ---
        addRenderLayer(new HamsterArmorLayer(this));
        addRenderLayer(new HamsterPinkPetalOverlayLayer(this));
        addRenderLayer(new HamsterAcornHatLayer(this));
    }

    @Override
    public Identifier getTextureLocation(HamsterEntity entity) {
        HamsterVariant variant = HamsterVariant.byId(entity.getVariant());
        String baseTextureName = variant.getBaseTextureName();
        return Identifier.of(
                AdorableHamsterPets.MOD_ID,
                "textures/entity/hamster/" + baseTextureName + ".png"
        );
    }

    @Override
    public void preRender(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model, @Nullable VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        // Track matrices for bones to which I need post-render access
        model.getBone("left_foot").ifPresent(bone -> bone.setTrackingMatrices(true));
        model.getBone("nose").ifPresent(bone -> bone.setTrackingMatrices(true));
        model.getBone(SEAT_BONE).ifPresent(bone -> bone.setTrackingMatrices(true));
    }

    @Override
    public void render(HamsterEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        // --- 1. Manage Cleaning Sound ---
        boolean isCleaning = entity.isCleaning();
        HamsterCleaningSoundInstance sound = activeCleaningSounds.get(entity.getId());

        if (isCleaning && (sound == null || sound.isDone())) {
            sound = new HamsterCleaningSoundInstance(entity);
            activeCleaningSounds.put(entity.getId(), sound);
            MinecraftClient.getInstance().getSoundManager().play(sound);
        } else if (!isCleaning && sound != null) {
            sound.stop();
            activeCleaningSounds.remove(entity.getId());
        }

        // --- 2. Set Shadow Radius ---
        if (entity.isBaby()) {
            this.shadowRadius = this.adultShadowRadius * 0.5f;
        } else {
            this.shadowRadius = this.adultShadowRadius;
        }

        // --- 3. Report to Client-Side Tracker ---
        // Add ID to a set to determine which entities are no longer being rendered
        AdorableHamsterPetsClient.onHamsterRendered(entity.getId());

        // --- 4. Smooth Snow Layer Height Adjustment ---
        poseStack.push();
        float targetYOffset = 0.0f;
        BlockPos pos = entity.getBlockPos();
        BlockState blockState = entity.getWorld().getBlockState(pos);

        // If block is a snow layer, apply a fixed offset equal to one layer's height
        if (blockState.isOf(Blocks.SNOW)) {
            targetYOffset = 1.0f / 8.0f;
        }

        // Smoothly interpolate the current offset towards the target offset
        entity.renderedSnowYOffset += (targetYOffset - entity.renderedSnowYOffset) * 0.15f; // controls transition speed
        poseStack.translate(0.0, entity.renderedSnowYOffset, 0.0);

        // --- 5. Force Animation Update for In-World Entities ---
        // Prevents animations from shoulder-pet dummies (FeatureRenderer)
        // from "bleeding" onto in-world hamsters during Flashback replays
        if (!entity.isShoulderPet()) {
            AnimatableInstanceCache cache = entity.getAnimatableInstanceCache();
            if (cache != null) {
                AnimatableManager<?> manager = cache.getManagerForId(entity.getId());
                if (manager != null) {
                    manager.updatedAt(0);
                }
            }
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.pop();
    }

    /**
     * Performs the final rendering steps, including particle effects, sounds, and passenger rendering.
     * <p>
     * This method polls a transient {@code particleEffectId} flag on the animatable entity each frame.
     * If the flag is set (by a particle keyframe event), it spawns the corresponding particle effect
     * at the animated bone's calculated world position and then immediately resets the flag to {@code null}
     * to prevent re-triggering.
     */
    @Override
    public void renderFinal(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);

        // --- 1. Handle Passengers (Bone-Locked) ---
        if (!animatable.getPassengerList().isEmpty()) {
            model.getBone(SEAT_BONE).ifPresent(bone -> {
                renderPassengersForBone(poseStack, animatable, bone, bufferSource, packedLight, partialTick);
            });
        }

        // --- 2. Handle Item in Mouth ---
        if (animatable.isHoldingInterestItem()) {
            model.getBone("nose").ifPresent(bone -> {
                renderItemForBone(poseStack, animatable, bone, bufferSource, packedLight, packedOverlay);
            });
        }

        // --- 3. Handle Keyframe Particles ---
        if (animatable.particleEffectId != null) {
            handleParticleKeyframes(animatable, model);
        }

        // --- 4. Handle Keyframe Sounds ---
        if (animatable.soundEffectId != null) {
            handleSoundKeyframes(animatable);
        }
    }

    /**
     * Renders hamster passengers "bone-locked" to a GeckoLib bone.
     *
     * <p>Core trick: Manually apply the bone's tracked matrix transform to the {@link MatrixStack}.
     * <p>
     * Then:
     * <ul>
     *   <li>Move to the bone pivot (stable attachment point)</li>
     *   <li>Cancel inherited bone scaling (keep bounce on hamster, not rider)</li>
     *   <li>Apply seat offsets (dynamic by passenger scale)</li>
     *   <li>Pre-cancel vanilla LivingEntityRenderer yaw rotation (avoid "double yaw")</li>
     * </ul>
     */
    private void renderPassengersForBone(MatrixStack matrices,
                                         HamsterEntity hamster,
                                         GeoBone bone,
                                         VertexConsumerProvider bufferSource,
                                         int packedLight,
                                         float partialTick) {

        final MinecraftClient client = MinecraftClient.getInstance();
        final EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        // --- Base Pose: Hamster Render Space ---
        // Already includes camera-relative translation + GeoEntityRenderer.applyRotations (180 - bodyYaw) + entity scale
        final Matrix4f modelBase = new Matrix4f(this.modelRenderTranslations);
        final Matrix3f modelBaseNormal = new Matrix3f(modelBase).invert().transpose();

        // Temporarily disable shadow rendering for the rider to prevent double shadows or artifacts
        dispatcher.setRenderShadows(false);
        try {
            for (Entity passenger : hamster.getPassengerList()) {
                if (!(passenger instanceof LivingEntity living)) {
                    continue;
                }

                // Local first-person: don't render "fake" rider so it doesn't obstruct view
                if (passenger == client.player && client.options.getPerspective().isFirstPerson()) {
                    continue;
                }

                matrices.push();
                try {
                    // --- 1. Jump Into Hamster's Model Render-Space ---
                    matrices.peek().getPositionMatrix().set(modelBase);
                    matrices.peek().getNormalMatrix().set(modelBaseNormal);

                    // --- 2. Apply Bone Translation and Rotation ---
                    Matrix4f bonePose = new Matrix4f(bone.getModelSpaceMatrix()); // tracked during recursive render

                    // Convert TRS -> TR only
                    // The rider should maintain their own size, not squash with the hamster's animation
                    var t = bonePose.getTranslation(new Vector3f());
                    var r = bonePose.getUnnormalizedRotation(new Quaternionf());
                    Matrix4f boneTR = new Matrix4f().identity().translate(t).rotate(r);

                    // Apply to current pose
                    matrices.peek().getPositionMatrix().mul(boneTR);

                    // Keep normals correct (translation ignored automatically by Matrix3f ctor)
                    matrices.peek().getNormalMatrix().set(new Matrix3f(matrices.peek().getPositionMatrix()).invert().transpose());


                    // --- 4. Cancel Vehicle Global Scale ---
                    // If the hamster itself is scaled, undo that scale for the rider
                    float mountScale = hamster.getScale();
                    if (mountScale != 1.0f) {
                        float inv = 1.0f / mountScale;
                        matrices.scale(inv, inv, inv);
                    }

                    // --- 5. Apply Seat Offsets ---
                    // Use centralized logic to position rider correctly on hamster's back
                    Vec3d seat = HamsterSeatOffsets.visualSeatOffset(living, hamster.getScale());
                    matrices.translate(seat.x, seat.y, seat.z);

                    // --- 6. Neutralize Vanilla Yaw ---
                    // Vanilla LivingEntityRenderer applies rotY(180 - yaw), so pre-apply the inverse
                    // to keep rider locked to the bone's rotation instead of the entity's global rotation.
                    float passengerYaw = MathHelper.lerpAngleDegrees(partialTick, passenger.prevYaw, passenger.getYaw());
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(passengerYaw - 180.0f));

                    // --- 7. Render the Passenger ---
                    // Set thread-local flag to bypass the Mixin that cancels vanilla rendering
                    IS_RENDERING_PASSENGER.set(true);
                    try {
                        dispatcher.render(passenger, 0.0, 0.0, 0.0, passengerYaw, partialTick, matrices, bufferSource, packedLight);
                    } finally {
                        IS_RENDERING_PASSENGER.set(false);
                    }
                } finally {
                    matrices.pop();
                }
            }
        } finally {
            // Re-enable shadows for subsequent renders
            dispatcher.setRenderShadows(true);
        }
    }

    /**
     * Renders an item held in the hamster's mouth, locked to the "nose" bone.
     * Mimics passenger rendering, but keeps the bone's scale logic intact.
     */
    private void renderItemForBone(MatrixStack matrices,
                                   HamsterEntity hamster,
                                   GeoBone bone,
                                   VertexConsumerProvider bufferSource,
                                   int packedLight,
                                   int packedOverlay) {

        ItemStack itemStack = hamster.getInterestItemStack();
        if (itemStack.isEmpty()) return;

        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

        // --- Base Pose: Hamster Render Space ---
        final Matrix4f modelBase = new Matrix4f(this.modelRenderTranslations);
        final Matrix3f modelBaseNormal = new Matrix3f(modelBase).invert().transpose();

        matrices.push();
        try {
            // 1. Jump Into Hamster's Model Render-Space
            matrices.peek().getPositionMatrix().set(modelBase);
            matrices.peek().getNormalMatrix().set(modelBaseNormal);

            // 2. Apply Full Bone Transform (T, R, S)
            // Using getModelSpaceMatrix() applies T, R, and S relative to the entity root.
            Matrix4f bonePose = new Matrix4f(bone.getModelSpaceMatrix());
            matrices.peek().getPositionMatrix().mul(bonePose);

            // 3. Keep normals correct
            matrices.peek().getNormalMatrix().set(new Matrix3f(matrices.peek().getPositionMatrix()).invert().transpose());

            // 4. Apply Manual Adjustments
            HamsterMouthItemOffsets.applyMouthItemTransforms(matrices);

            // 5. Render Item
            itemRenderer.renderItem(itemStack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, packedLight, packedOverlay, matrices, bufferSource, hamster.getWorld(), hamster.getId());

        } finally {
            matrices.pop();
        }
    }


    private void handleParticleKeyframes(HamsterEntity animatable, BakedGeoModel model) {
        Random random = animatable.getRandom();
        switch (animatable.particleEffectId) {
            case "attack_poof":
                model.getBone("left_foot").ifPresent(bone -> {
                    Vector3d pos = bone.getWorldPosition();
                    for (int i = 0; i < 8; ++i) {
                        double d = random.nextGaussian() * 0.1;
                        double e = random.nextGaussian() * 0.2;
                        double f = random.nextGaussian() * 0.1;
                        animatable.getWorld().addParticle(ParticleTypes.WHITE_SMOKE,
                                pos.x + d, pos.y + e, pos.z + f,
                                random.nextGaussian() * 0.05,
                                random.nextGaussian() * 0.05,
                                random.nextGaussian() * 0.05);
                    }
                });
                break;
            case "seeking_dust":
                model.getBone("nose").ifPresent(bone -> {
                    Vector3d pos = bone.getWorldPosition();
                    BlockPos blockBelow = BlockPos.ofFloored(pos.x, pos.y - 0.1, pos.z).down();
                    BlockState state = animatable.getWorld().getBlockState(blockBelow);
                    if (state.isAir()) state = Blocks.DIRT.getDefaultState();
                    for (int i = 0; i < 12; ++i) {
                        double d = random.nextGaussian() * 0.2;
                        double e = random.nextGaussian() * 0.03;
                        double f = random.nextGaussian() * 0.2;
                        animatable.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state),
                                pos.x + d, pos.y + e, pos.z + f,
                                0.0, 0.0, 0.0);
                    }
                });
                break;
        }
        animatable.particleEffectId = null;
    }

    private void handleSoundKeyframes(HamsterEntity animatable) {
        MinecraftClient client = MinecraftClient.getInstance();
        switch (animatable.soundEffectId) {
            case "hamster_step_sound":
                BlockPos pos = animatable.getBlockPos();
                BlockState blockState = animatable.getWorld().getBlockState(pos.down());
                if (blockState.isAir()) blockState = animatable.getWorld().getBlockState(pos.down(2));
                if (!blockState.isAir()) {
                    BlockSoundGroup group = blockState.getSoundGroup();
                    float volume = blockState.isOf(Blocks.GRAVEL) ? (0.10F * 0.60F) : 0.10F;
                    client.getSoundManager().play(new PositionedSoundInstance(
                            group.getStepSound(), SoundCategory.NEUTRAL, volume,
                            group.getPitch() * 1.5F, animatable.getRandom(),
                            animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_beg_bounce":
                SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, animatable.getRandom());
                if (bounceSound != null) {
                    float basePitch = animatable.getSoundPitch();
                    float randomPitchAddition = animatable.getRandom().nextFloat() * 0.2f;
                    float finalPitch = (basePitch * 1.2f) + randomPitchAddition;
                    client.getSoundManager().play(new PositionedSoundInstance(
                            bounceSound, SoundCategory.NEUTRAL, 0.6f, finalPitch,
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_thump_sound":
                float thumpPitch = 1.0F + animatable.getRandom().nextFloat() * 0.4F;
                client.getSoundManager().play(new PositionedSoundInstance(
                        ModSounds.HAMSTER_THUMP.get(), SoundCategory.NEUTRAL, 0.3f, thumpPitch,
                        animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                ));
                break;
        }
        animatable.soundEffectId = null;
    }
}