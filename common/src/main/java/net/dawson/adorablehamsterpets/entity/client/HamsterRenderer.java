package net.dawson.adorablehamsterpets.entity.client;

import java.lang.Math;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.payload.HamsterAnimationSoundPayload;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMouthItemOffsets;
import net.dawson.adorablehamsterpets.util.HamsterRenderUtil;
import net.dawson.adorablehamsterpets.util.HamsterRidingUtil;
import net.dawson.adorablehamsterpets.util.HamsterTextureUtil;
import net.dawson.adorablehamsterpets.util.RedstoneFeverUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimatableManager;
import com.geckolib.cache.object.BakedGeoModel;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.renderer.GeoEntityRenderer;

public class HamsterRenderer extends GeoEntityRenderer<HamsterEntity> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final String SEAT_BONE = "body_child";

    // Suppresses vanilla passenger rendering
    public static final ThreadLocal<Boolean> IS_RENDERING_PASSENGER = ThreadLocal.withInitial(() -> false);

    // Suppresses nameplate inside inventory GUI
    public static final ThreadLocal<Boolean> IS_RENDERING_IN_GUI = ThreadLocal.withInitial(() -> false);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final float adultShadowRadius = 0.2F;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HamsterModel());
        this.addRenderLayer(new RedstoneFeverEyesRenderLayer(this));
        this.shadowRadius = this.adultShadowRadius;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public Identifier getTextureLocation(HamsterEntity entity) {
        // Defer to caching utility
        return HamsterTextureUtil.getHamsterTexture(entity);
    }

    @Override
    public boolean shouldShowName(HamsterEntity entity) {
        if (IS_RENDERING_IN_GUI.get()) {
            return false;
        }
        return super.shouldShowName(entity);
    }

    @Override
    public void preRender(PoseStack poseStack, HamsterEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        // Track matrices for post render access
        model.getBone("left_foot").ifPresent(bone -> bone.setTrackingMatrices(true));
        model.getBone("nose").ifPresent(bone -> bone.setTrackingMatrices(true));
        model.getBone(SEAT_BONE).ifPresent(bone -> bone.setTrackingMatrices(true));
    }

    @Override
    public void render(HamsterEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // --- 1. Set Shadow Radius ---
        if (entity.isBaby()) {
            this.shadowRadius = this.adultShadowRadius * 0.5f;
        } else {
            this.shadowRadius = this.adultShadowRadius;
        }

        // --- 2. Report to Client-Side Tracker ---
        // Add ID to set to determine entities no longer rendered
        AdorableHamsterPetsClient.onHamsterRendered(entity.getId());

        // --- 3. Redstone Fever Tremor ---
        poseStack.pushPose();
        if (entity.hasRedstoneFever() && !entity.isRedstoneFeverBurstActive() && !IS_RENDERING_IN_GUI.get()) {
            // --- Tuning ---
            double baseAmplitude =  0.000D;
            double spikeAmplitude = 0.015D;
            double horizontalXFrequency = 4.73D;
            double horizontalZFrequency = 4.39D;

            // --- Inputs ---
            // Severity fades from 1.0 toward 0.0
            double severity = entity.getSynchronizedRedstoneFeverSeverity();
            double renderTime = entity.level().getGameTime() + partialTick;

            // --- Amplitude Pulse ---
            double spike = RedstoneFeverUtil.getTremorSpike(renderTime, entity.getUUID()) * spikeAmplitude;
            double finalAmplitude = (baseAmplitude + spike) * severity;
            double amplitudePulse = (baseAmplitude + spike) / (baseAmplitude + spikeAmplitude);

            // --- Horizontal Offsets ---
            // Mismatched X/Z frequencies to prevent diagonal rocking
            double entityPhase = entity.getUUID().hashCode() * 0.61803398875D;
            double xOffset = Math.sin(renderTime * horizontalXFrequency + entityPhase) * finalAmplitude;
            double zOffset = Math.cos(renderTime * horizontalZFrequency + entityPhase) * finalAmplitude;
            poseStack.translate(xOffset, 0.0D, zOffset);
            double roll = Math.toRadians(2.5D)
                    * amplitudePulse
                    * severity
                    * Math.sin(renderTime * 4.17D + entityPhase);
            poseStack.mulPose(Axis.ZP.rotation((float) roll));
        }

        // --- 4. Smooth Ground Surface Height Adjustment ---
        if (IS_RENDERING_IN_GUI.get() || entity.isShoulderPet() || entity.isProjectileDummy) {
            entity.renderedGroundYOffset = 0.0F;
        } else {
            float targetYOffset = HamsterRenderUtil.getGroundSurfaceOffset(entity);
            entity.renderedGroundYOffset += (targetYOffset - entity.renderedGroundYOffset) * 0.15F;
            poseStack.translate(0.0, entity.renderedGroundYOffset, 0.0);
        }

        // --- 5. Iris/Shader Compatibility Hack ---
        // Force GeckoLib to rebuild bone poses for this entity. Prevents animations
        // from "bleeding" between different hamsters during multi-pass rendering.
        // I'm detecting these multi-passes (and game pauses/server lag, because those
        // also cause this) by checking if the render time hasn't changed.
        double currentTick = entity.tickCount + partialTick;
        AnimatableInstanceCache cache = entity.getAnimatableInstanceCache();
        if (cache != null) {
            AnimatableManager<?> manager = cache.getManagerForId(entity.getId());
            if (manager != null) {
                if (currentTick == entity.lastRenderTime) {
                    // Use microscopic delta to prevent transition math from stretching model
                    manager.updatedAt(currentTick - 0.0000001);
                }
            }
        }
        entity.lastRenderTime = currentTick;

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    public void renderFinal(PoseStack poseStack, HamsterEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);

        // --- 1. Handle Passengers ---
        if (!animatable.getPassengers().isEmpty()) {
            model.getBone(SEAT_BONE).ifPresent(bone -> {
                renderPassengersForBone(poseStack, animatable, bone, bufferSource, packedLight, partialTick);
            });
        }

        // --- 2. Handle Mouth Item ---
        if (animatable.isHoldingMouthItem()) {
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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Renders hamster passengers "bone-locked" to a GeckoLib bone.
     *
     * <p>Core trick: Manually apply the bone's tracked matrix transform to the {@link PoseStack}.
     * <p>
     * Then:
     * <ul>
     *   <li>Move to the bone pivot (stable attachment point)</li>
     *   <li>Cancel inherited bone scaling (keep bounce on hamster, not rider)</li>
     *   <li>Apply seat offsets (dynamic by passenger scale)</li>
     *   <li>Pre-cancel vanilla LivingEntityRenderer yaw rotation (avoid "double yaw")</li>
     * </ul>
     */
    private void renderPassengersForBone(PoseStack matrices,
                                         HamsterEntity hamster,
                                         GeoBone bone,
                                         MultiBufferSource bufferSource,
                                         int packedLight,
                                         float partialTick) {

        final Minecraft client = Minecraft.getInstance();
        final EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        // --- Base Pose: Hamster Render Space ---
        // Already includes camera-relative translation + GeoEntityRenderer.applyRotations (180 - bodyYaw) + entity scale
        final Matrix4f modelBase = new Matrix4f(this.modelRenderTranslations);
        final Matrix3f modelBaseNormal = new Matrix3f(modelBase).invert().transpose();

        // Temporarily disable shadow rendering for rider to prevent double shadows or artifacts
        dispatcher.setRenderShadow(false);
        try {
            for (Entity passenger : hamster.getPassengers()) {
                if (!(passenger instanceof LivingEntity living)) {
                    continue;
                }

                // Skip fake rider in first person so it doesn't obstruct view
                if (passenger == client.player && client.options.getCameraType().isFirstPerson()) {
                    continue;
                }

                matrices.pushPose();
                try {
                    // --- 1. Jump Into Hamster's Model Render-Space ---
                    matrices.last().pose().set(modelBase);
                    matrices.last().normal().set(modelBaseNormal);

                    // --- 2. Apply Bone Translation and Rotation ---
                    Matrix4f bonePose = new Matrix4f(bone.getModelSpaceMatrix()); // tracked during recursive render

                    // Convert TRS -> TR only
                    // The rider should maintain their own size, not squash with the hamster's animation
                    var t = bonePose.getTranslation(new Vector3f());
                    var r = bonePose.getUnnormalizedRotation(new Quaternionf());
                    Matrix4f boneTR = new Matrix4f().identity().translate(t).rotate(r);

                    // Apply to current pose
                    matrices.last().pose().mul(boneTR);

                    // Keep normals correct (translation ignored automatically by Matrix3f ctor)
                    matrices.last().normal().set(new Matrix3f(matrices.last().pose()).invert().transpose());


                    // --- 4. Cancel Vehicle Global Scale ---
                    // If the hamster itself is scaled, undo that scale for the rider
                    float mountScale = hamster.getScale();
                    if (mountScale != 1.0f) {
                        float inv = 1.0f / mountScale;
                        matrices.scale(inv, inv, inv);
                    }

                    // --- 5. Apply Seat Offsets ---
                    // Use centralized logic to position rider correctly on hamster's back
                    Vec3 seat = HamsterRidingUtil.HamsterSeatOffsets.visualSeatOffset(living, hamster.getScale());
                    matrices.translate(seat.x, seat.y, seat.z);

                    // --- 6. Neutralize Vanilla Yaw ---
                    // Vanilla LivingEntityRenderer applies rotY(180 - yaw), so pre-apply the inverse
                    // to keep rider locked to the bone's rotation instead of the entity's global rotation.
                    float passengerYaw = Mth.rotLerp(partialTick, passenger.yRotO, passenger.getYRot());
                    matrices.mulPose(Axis.YP.rotationDegrees(passengerYaw - 180.0f));

                    // --- 7. Render the Passenger ---
                    // Set thread-local flag to bypass the Mixin that cancels vanilla rendering
                    IS_RENDERING_PASSENGER.set(true);
                    try {
                        dispatcher.render(passenger, 0.0, 0.0, 0.0, passengerYaw, partialTick, matrices, bufferSource, packedLight);
                    } finally {
                        IS_RENDERING_PASSENGER.set(false);
                    }
                } finally {
                    matrices.popPose();
                }
            }
        } finally {
            // Re-enable shadows for subsequent renders
            dispatcher.setRenderShadow(true);
        }
    }

    /**
     * Renders an item held in the hamster's mouth, locked to the "nose" bone.
     * Mimics passenger rendering, but keeps the bone's scale logic intact.
     */
    private void renderItemForBone(PoseStack matrices,
                                   HamsterEntity hamster,
                                   GeoBone bone,
                                   MultiBufferSource bufferSource,
                                   int packedLight,
                                   int packedOverlay) {

        ItemStack itemStack = hamster.getMouthItemStack();
        if (itemStack.isEmpty()) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        // --- Base Pose: Hamster Render Space ---
        final Matrix4f modelBase = new Matrix4f(this.modelRenderTranslations);
        final Matrix3f modelBaseNormal = new Matrix3f(modelBase).invert().transpose();

        matrices.pushPose();
        try {
            // 1. Jump Into Hamster's Model Render-Space
            matrices.last().pose().set(modelBase);
            matrices.last().normal().set(modelBaseNormal);

            // 2. Apply Full Bone Transform (T, R, S)
            // Using getModelSpaceMatrix() applies T, R, and S relative to the entity root.
            Matrix4f bonePose = new Matrix4f(bone.getModelSpaceMatrix());
            matrices.last().pose().mul(bonePose);

            // 3. Keep normals correct
            matrices.last().normal().set(new Matrix3f(matrices.last().pose()).invert().transpose());

            // 4. Apply Manual Adjustments
            HamsterMouthItemOffsets.applyMouthItemTransforms(matrices);

            // 5. Render Item
            itemRenderer.renderStatic(itemStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, packedOverlay, matrices, bufferSource, hamster.level(), hamster.getId());

        } finally {
            matrices.popPose();
        }
    }

    private void handleParticleKeyframes(HamsterEntity animatable, BakedGeoModel model) {
        RandomSource random = animatable.getRandom();
        switch (animatable.particleEffectId) {
            case "attack_poof":
                model.getBone("left_foot").ifPresent(bone -> {
                    Vector3d pos = bone.getWorldPosition();
                    for (int i = 0; i < 8; ++i) {
                        double d = random.nextGaussian() * 0.1;
                        double e = random.nextGaussian() * 0.2;
                        double f = random.nextGaussian() * 0.1;
                        animatable.level().addParticle(ParticleTypes.WHITE_SMOKE,
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
                    BlockPos blockBelow = BlockPos.containing(pos.x, pos.y - 0.1, pos.z).below();
                    BlockState state = animatable.level().getBlockState(blockBelow);
                    if (state.isAir()) state = Blocks.DIRT.defaultBlockState();
                    for (int i = 0; i < 12; ++i) {
                        double d = random.nextGaussian() * 0.2;
                        double e = random.nextGaussian() * 0.03;
                        double f = random.nextGaussian() * 0.2;
                        animatable.level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state),
                                pos.x + d, pos.y + e, pos.z + f,
                                0.0, 0.0, 0.0);
                    }
                });
                break;
            case "hamster_spit_particles":
                // Spawn items and spit them out
                model.getBone("nose").ifPresent(bone -> {
                    Vector3d pos = bone.getWorldPosition();
                    // 1. Item Particles (if holding item)
                    ItemStack mouthStack = animatable.getMouthItemStack();
                    if (!mouthStack.isEmpty()) {
                        for (int i = 0; i < 5; i++) {
                            animatable.level().addParticle(
                                    new ItemParticleOption(ParticleTypes.ITEM, mouthStack),
                                    pos.x, pos.y, pos.z,
                                    (random.nextDouble() - 0.5) * 0.3,
                                    random.nextDouble() * 0.2,
                                    (random.nextDouble() - 0.5) * 0.3
                            );
                        }
                    }
                    // 2. Llama Spit Particles
                    for (int i = 0; i < 8; i++) {
                        animatable.level().addParticle(
                                ParticleTypes.SPIT,
                                pos.x, pos.y, pos.z,
                                (random.nextDouble() - 0.5) * 0.1,
                                random.nextDouble() * 0.1,
                                (random.nextDouble() - 0.5) * 0.1
                        );
                    }
                });
                break;
        }
        animatable.particleEffectId = null;
    }

    private void handleSoundKeyframes(HamsterEntity animatable) {
        Minecraft client = Minecraft.getInstance();
        switch (animatable.soundEffectId) {
            case "dynamic_item_sound":
                ItemStack mouthStack = animatable.getMouthItemStack();
                if (!mouthStack.isEmpty()) {
                    SoundEvent dynamicSound = ModSounds.getDynamicItemSound(mouthStack);
                    float baseVol = ModSounds.getDynamicSoundVolume(dynamicSound);

                    client.getSoundManager().play(new SimpleSoundInstance(
                            dynamicSound, SoundSource.NEUTRAL, baseVol * 0.6f, 1.0f + (animatable.getRandom().nextFloat() - 0.5f) * 0.2f,
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_scratch_sound":
                SoundEvent scratchSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SCRATCH_SOUNDS, animatable.getRandom());
                if (scratchSound != null) {
                    client.getSoundManager().play(new SimpleSoundInstance(
                            scratchSound, SoundSource.NEUTRAL, 0.2f, 0.8f,
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_roll_back_sound":
                SoundEvent rollBackSound = Configs.AHP_MAIN.enableRollingSlideWhistle
                        ? ModSounds.HAMSTER_ROLL_BACK.get()
                        : ModSounds.HAMSTER_ROLL_BACK_NO_SLIDE_WHISTLE.get();

                client.getSoundManager().play(new SimpleSoundInstance(
                        rollBackSound, SoundSource.NEUTRAL, 0.3f, 1.4f,
                        animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                ));
                break;
            case "hamster_roll_forward_sound":
                client.getSoundManager().play(new SimpleSoundInstance(
                        ModSounds.HAMSTER_ROLL_FORWARD.get(), SoundSource.NEUTRAL, 0.2f, 1.4f,
                        animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                ));
                break;
            case "hamster_step_sound":
                BlockPos pos = animatable.blockPosition();
                BlockState blockState = animatable.level().getBlockState(pos.below());
                if (blockState.isAir()) blockState = animatable.level().getBlockState(pos.below(2));
                if (!blockState.isAir()) {
                    SoundType group = blockState.getSoundType();
                    float volume = blockState.is(Blocks.GRAVEL) ? (0.10F * 0.60F) : 0.10F;
                    client.getSoundManager().play(new SimpleSoundInstance(
                            group.getStepSound(), SoundSource.NEUTRAL, volume,
                            group.getPitch() * 1.5F, animatable.getRandom(),
                            animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_bounce_sound":
                if (animatable.isDancing()) break; // Mute bounce sound when dancing to music disc
                SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, animatable.getRandom());
                if (bounceSound != null) {
                    float basePitch = animatable.getVoicePitch();
                    float randomPitchAddition = animatable.getRandom().nextFloat() * 0.2f;
                    float finalPitch = (basePitch * 1.2f) + randomPitchAddition;
                    client.getSoundManager().play(new SimpleSoundInstance(
                            bounceSound, SoundSource.NEUTRAL, 0.6f, finalPitch,
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_thump_sound":
                float thumpPitch = 1.0F + animatable.getRandom().nextFloat() * 0.4F;
                client.getSoundManager().play(new SimpleSoundInstance(
                        ModSounds.HAMSTER_THUMP.get(), SoundSource.NEUTRAL, 0.3f, thumpPitch,
                        animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                ));

                // Broadcast via server if >16.0 blocks from player to bypass vanilla distance attenuation
                if (client.player != null) {
                    if (animatable.distanceToSqr(client.player) > 16.0 * 16.0) {
                        NetworkManager.sendToServer(
                                new HamsterAnimationSoundPayload(animatable.getId(), "hamster_thump_sound")
                        );
                    }
                }
                break;
            case "hamster_spit_sound":
                client.getSoundManager().play(new SimpleSoundInstance(
                        SoundEvents.LLAMA_SPIT, SoundSource.NEUTRAL, 0.4f, 2.0f,
                        animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                ));
                break;
            case "hamster_sniff_sound":
                SoundEvent sniffSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, animatable.getRandom());
                if (sniffSound != null) {
                    client.getSoundManager().play(new SimpleSoundInstance(
                            sniffSound, SoundSource.NEUTRAL, 1.0f, animatable.getVoicePitch(),
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_head_shake_fast_sound":
                client.getSoundManager().play(new SimpleSoundInstance(
                        ModSounds.HAMSTER_HEAD_SHAKE_FAST.get(), SoundSource.NEUTRAL, 0.35f, 1.0f + (animatable.getRandom().nextFloat() - 0.5f) * 0.2f,
                        animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                ));
                break;
            case "hamster_swish_sound":
                client.getSoundManager().play(new SimpleSoundInstance(
                        ModSounds.HAMSTER_SWISH.get(), SoundSource.NEUTRAL, 0.1f, 1.0f + (animatable.getRandom().nextFloat() * 0.5f),
                        animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                ));
                break;
            case "hamster_water_swish_sound":
                SoundEvent waterSwishSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_WATER_SWISH_SOUNDS, animatable.getRandom());
                if (waterSwishSound != null) {
                    client.getSoundManager().play(new SimpleSoundInstance(
                            waterSwishSound, SoundSource.NEUTRAL, 0.25f, 1.0f,
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_affection_sound":
                SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, animatable.getRandom());
                if (affectionSound != null) {
                    client.getSoundManager().play(new SimpleSoundInstance(
                            affectionSound, SoundSource.NEUTRAL, 1.0f, 1.0f,
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_celebrate_sound":
                SoundEvent celebrateSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, animatable.getRandom());
                if (celebrateSound != null) {
                    client.getSoundManager().play(new SimpleSoundInstance(
                            celebrateSound, SoundSource.NEUTRAL, 1.0f, 1.0f,
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
            case "hamster_sneeze_sound":
                SoundEvent sneezeSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SNEEZE_SOUNDS, animatable.getRandom());
                if (sneezeSound != null) {
                    client.getSoundManager().play(new SimpleSoundInstance(
                            sneezeSound, SoundSource.NEUTRAL, 0.6f, 1.0f,
                            animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                    ));
                }
                break;
        }
        animatable.soundEffectId = null;
    }
}
