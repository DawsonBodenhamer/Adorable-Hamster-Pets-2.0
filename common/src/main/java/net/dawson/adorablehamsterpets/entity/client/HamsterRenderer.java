package net.dawson.adorablehamsterpets.entity.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.UUIDUtil;
import java.lang.Math;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.networking.payload.HamsterAnimationSoundPayload;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMouthItemOffsets;
import net.dawson.adorablehamsterpets.util.HamsterRenderUtil;
import net.dawson.adorablehamsterpets.util.HamsterRidingUtil;
import net.dawson.adorablehamsterpets.util.HamsterTextureUtil;
import net.dawson.adorablehamsterpets.util.RedstoneFeverUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class HamsterRenderer extends GeoEntityRenderer<HamsterEntity, HamsterRenderState> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final String SEAT_BONE = "body_child";

    // Moved from HamsterModel with the bone logic (GeckoLib 5)
    private static final float ADULT_SCALE = 0.8f;
    private static final float ADULT_HEAD_SCALE = 1.0f;
    private static final float BABY_SCALE = 0.5f;
    private static final float BABY_HEAD_SCALE = 1.2f;

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
        this.withRenderLayer(new RedstoneFeverEyesRenderLayer<>(this));
        this.shadowRadius = this.adultShadowRadius;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public HamsterRenderState createRenderState(HamsterEntity entity, Void relatedObject) {
        return new HamsterRenderState();
    }

    @Override
    public Identifier getTextureLocation(HamsterRenderState renderState) {
        // Resolved during extraction, where the entity is still reachable.
        return renderState.textureLocation;
    }

    @Override
    public boolean shouldShowName(HamsterEntity entity, double distanceSq) {
        if (IS_RENDERING_IN_GUI.get()) {
            return false;
        }
        return super.shouldShowName(entity, distanceSq);
    }

    @Override
    public void preRenderPass(RenderPassInfo<HamsterRenderState> renderPass, SubmitNodeCollector collector) {
        super.preRenderPass(renderPass, collector);

        // Track matrices so the post pass can position passengers, the mouth item
        // and particle emitters against live bone transforms.
        // 26.2 port (GeckoLib 5): bones no longer track matrices; register listeners
        // that hand the post pass each bone's world position instead.
        HamsterRenderState state = renderPass.renderState();
        state.bonePositions.clear();
        for (String boneName : new String[] {"left_foot", "nose", SEAT_BONE}) {
            renderPass.addBonePositionListener(boneName, (position, rotation, scale) -> state.bonePositions.put(boneName, position));
        }

        // Mouth item and riders are drawn as per-bone render tasks: GeckoLib runs them
        // with the pose stack already at the bone's transform.
        if (state.mouthItemRenderState != null) {
            renderPass.model().getBone("nose").ifPresent(bone -> renderPass.addPerBoneRender(bone, this::submitMouthItem));
        }
        if (!state.riders.isEmpty()) {
            renderPass.model().getBone(SEAT_BONE).ifPresent(bone -> renderPass.addPerBoneRender(bone, this::submitRiders));
        }
    }

    /** Resolves the item in the hamster's mouth into a render state (needs the entity, so runs at extraction). */
    private static void extractMouthItem(HamsterEntity entity, HamsterRenderState state) {
        state.mouthItemRenderState = null;
        if (!entity.isHoldingMouthItem()) return;
        ItemStack stack = entity.getMouthItemStack();
        if (stack.isEmpty()) return;
        ItemStackRenderState itemState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(itemState, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
        if (!itemState.isEmpty()) state.mouthItemRenderState = itemState;
    }

    /** Snapshots every rider with its own renderer's state so the post pass can draw it on the seat bone. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void extractRiders(HamsterEntity hamster, HamsterRenderState state, float partialTick) {
        state.riders.clear(); // adorablehamsterpets$extractRiders
        if (hamster.getPassengers().isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        for (Entity passenger : hamster.getPassengers()) {
            if (!(passenger instanceof LivingEntity living)) continue;
            if (passenger == client.player && client.options.getCameraType().isFirstPerson()) continue;
            EntityRenderer renderer = dispatcher.getRenderer(passenger);
            if (renderer == null) continue;
            EntityRenderState riderState = renderer.createRenderState();
            renderer.extractRenderState(passenger, riderState, partialTick);
            riderState.shadowPieces.clear(); // the hamster already casts the shadow
            Vec3 seat = HamsterRidingUtil.HamsterSeatOffsets.visualSeatOffset(living, hamster.getScale());
            float yaw = Mth.rotLerp(partialTick, passenger.yRotO, passenger.getYRot());
            state.riders.add(new HamsterRenderState.Rider(riderState, seat, yaw));
        }
    }

    /** Per-bone task on the nose: draws the carried item with the mod's mouth offsets. */
    private void submitMouthItem(RenderPassInfo<HamsterRenderState> renderPass, GeoBone bone, SubmitNodeCollector collector) {
        HamsterRenderState state = renderPass.renderState();
        if (state.mouthItemRenderState == null) return;
        PoseStack poseStack = renderPass.poseStack();
        poseStack.pushPose();
        HamsterMouthItemOffsets.applyMouthItemTransforms(poseStack);
        state.mouthItemRenderState.submit(poseStack, collector, renderPass.packedLight(), renderPass.packedOverlay(), 0);
        poseStack.popPose();
    }

    /** Per-bone task on the seat bone: draws riders locked to the animated seat. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void submitRiders(RenderPassInfo<HamsterRenderState> renderPass, GeoBone bone, SubmitNodeCollector collector) {
        HamsterRenderState state = renderPass.renderState();
        HamsterEntity hamster = state.entity;
        if (hamster == null || state.riders.isEmpty()) return;
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        PoseStack poseStack = renderPass.poseStack();
        float mountScale = hamster.getScale();
        for (HamsterRenderState.Rider rider : state.riders) {
            EntityRenderer renderer = dispatcher.getRenderer(rider.state());
            if (renderer == null) continue;
            poseStack.pushPose();
            if (mountScale != 1.0f) {
                float inv = 1.0f / mountScale;
                poseStack.scale(inv, inv, inv);
            }
            poseStack.translate(rider.seatOffset().x, rider.seatOffset().y, rider.seatOffset().z);
            poseStack.mulPose(Axis.YP.rotationDegrees(rider.yaw() - 180.0f));
            IS_RENDERING_PASSENGER.set(true);
            try {
                renderer.submit(rider.state(), poseStack, collector, renderPass.cameraState());
            } finally {
                IS_RENDERING_PASSENGER.set(false);
            }
            poseStack.popPose();
        }
    }

    /**
     * 26.2 port: everything the old {@code render} read off the entity happens here.
     * Drawing runs later against {@link HamsterRenderState} alone, so any value the
     * pose or post pass needs has to be resolved and stored now.
     */
    @Override
    public void extractRenderState(HamsterEntity entity, HamsterRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        final boolean inGui = IS_RENDERING_IN_GUI.get();
        state.renderingInGui = inGui;
        state.textureLocation = HamsterTextureUtil.getHamsterTexture(entity);

        // --- 1. Shadow Size ---
        this.shadowRadius = entity.isBaby() ? this.adultShadowRadius * 0.5f : this.adultShadowRadius;

        // --- 2. Report to Client-Side Tracker ---
        AdorableHamsterPetsClient.onHamsterRendered(entity.getId());

        // --- 3. Redstone Fever Tremor ---
        // Resolved to finished offsets here; the pose pass only applies them.
        state.tremorOffsetX = 0.0D;
        state.tremorOffsetZ = 0.0D;
        state.tremorRoll = 0.0F;

        if (entity.hasRedstoneFever() && !entity.isRedstoneFeverBurstActive() && !inGui) {
            final double baseAmplitude = 0.000D;
            final double spikeAmplitude = 0.015D;
            final double horizontalXFrequency = 4.73D;
            final double horizontalZFrequency = 4.39D;

            // Severity fades from 1.0 toward 0.0
            double severity = entity.getSynchronizedRedstoneFeverSeverity();
            double renderTime = entity.level().getGameTime() + partialTick;

            double spike = RedstoneFeverUtil.getTremorSpike(renderTime, entity.getUUID()) * spikeAmplitude;
            double finalAmplitude = (baseAmplitude + spike) * severity;
            double amplitudePulse = (baseAmplitude + spike) / (baseAmplitude + spikeAmplitude);

            // Mismatched X/Z frequencies to prevent diagonal rocking
            double entityPhase = entity.getUUID().hashCode() * 0.61803398875D;
            state.tremorOffsetX = Math.sin(renderTime * horizontalXFrequency + entityPhase) * finalAmplitude;
            state.tremorOffsetZ = Math.cos(renderTime * horizontalZFrequency + entityPhase) * finalAmplitude;
            state.tremorRoll = (float) (Math.toRadians(2.5D)
                    * amplitudePulse
                    * severity
                    * Math.sin(renderTime * 4.17D + entityPhase));
        }

        // --- 4. Smooth Ground Surface Height Adjustment ---
        if (inGui || entity.isShoulderPet() || entity.isProjectileDummy) {
            entity.renderedGroundYOffset = 0.0F;
            state.groundYOffset = 0.0F;
        } else {
            float targetYOffset = HamsterRenderUtil.getGroundSurfaceOffset(entity);
            entity.renderedGroundYOffset += (targetYOffset - entity.renderedGroundYOffset) * 0.15F;
            state.groundYOffset = entity.renderedGroundYOffset;
        }


        // --- 5b. Rolling Shadow Offset ---
        // 26.2 port: this used to be an EntityRenderDispatcher.renderShadow mixin.
        // Shadows are now a list of pieces on the render state, so the roll offset
        // is applied by shifting each piece relative to the entity instead.
        double rollOffset = entity.getRollShadowOffset(partialTick);
        if (rollOffset > 0.0D && !state.shadowPieces.isEmpty()) {
            float bodyYaw = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            float yawRadians = (float) Math.toRadians(bodyYaw);
            float dx = (float) (Math.sin(yawRadians) * rollOffset);
            float dz = (float) (-Math.cos(yawRadians) * rollOffset);
            state.shadowPieces.replaceAll(piece -> new EntityRenderState.ShadowPiece(
                    piece.relativeX() + dx, piece.relativeY(), piece.relativeZ() + dz,
                    piece.shapeBelow(), piece.alpha()));
        }

        // --- 6. Data the post pass needs ---
        state.entity = entity;
        state.hasPassengers = !entity.getPassengers().isEmpty();
        extractMouthItem(entity, state);
        extractRiders(entity, state, partialTick);
    }

    /** Applies the offsets resolved during extraction. */
    @Override
    public void adjustRenderPose(RenderPassInfo<HamsterRenderState> renderPass) {
        super.adjustRenderPose(renderPass);

        HamsterRenderState state = renderPass.renderState();
        PoseStack poseStack = renderPass.poseStack();

        if (state.tremorOffsetX != 0.0D || state.tremorOffsetZ != 0.0D) {
            poseStack.translate(state.tremorOffsetX, 0.0D, state.tremorOffsetZ);
        }
        if (state.tremorRoll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(state.tremorRoll));
        }
        if (state.groundYOffset != 0.0F) {
            poseStack.translate(0.0D, state.groundYOffset, 0.0D);
        }
    }

    @Override
    public void postRenderPass(RenderPassInfo<HamsterRenderState> renderPass, SubmitNodeCollector collector) {
        super.postRenderPass(renderPass, collector);

        HamsterRenderState state = renderPass.renderState();
        HamsterEntity animatable = state.entity;
        if (animatable == null) {
            return;
        }

        BakedGeoModel model = renderPass.model();

        // --- 1. Handle Keyframe Particles ---
        if (animatable.particleEffectId != null) {
            handleParticleKeyframes(animatable, state);
        }

        // --- 2. Handle Keyframe Sounds ---
        if (animatable.soundEffectId != null) {
            handleSoundKeyframes(animatable, state);
        }

    }

    /**
     * 26.2 port: GeckoLib 5 removed GeoModel.setCustomAnimations. Per-frame bone
     * work now belongs to the renderer, so this arrived here wholesale -- bones
     * come from the baked model instead of the animation processor.
     */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<HamsterRenderState> renderPass, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPass, snapshots);

        final HamsterEntity entity = renderPass.renderState().entity;
        if (entity == null) {
            return;
        }
        final BakedGeoModel model = renderPass.model();


        // --- Performance Mode ---
        if (AdorableHamsterPetsClient.isPerformanceModeEnabled) {
            // Hide everything except absolute essentials
            for (GeoBone bone : model.boneLookup().get().values()) {
                String name = bone.name();
                boolean keepVisible = name.equals("root")
                        || name.equals("body_parent")
                        || name.equals("body_child");
                snapshots.get(bone).skipRender(!keepVisible);
            }

            return; // Skip all other visual calculations
        } else {
            // Restore visibility to all bones when performance mode off
            for (GeoBone bone : model.boneLookup().get().values()) {
                snapshots.get(bone).skipRender(false);
            }
        }

        // --- Normal Mode ---
        // --- Bone References ---
        var rootBone = model.getBone("root").orElse(null);
        var headParentBone = model.getBone("head_parent").orElse(null);
        var leftCheekDefBone = model.getBone("left_cheek_deflated").orElse(null);
        var rightCheekDefBone = model.getBone("right_cheek_deflated").orElse(null);
        var leftCheekInfBone = model.getBone("left_cheek_inflated").orElse(null);
        var rightCheekInfBone = model.getBone("right_cheek_inflated").orElse(null);
        var rightEarBone = model.getBone("right_ear").orElse(null);
        var acornHatBone = model.getBone("acorn_hat").orElse(null);
        var flowerHeadNoArmorBone = model.getBone("flower_head_no_armor").orElse(null);
        var flowerSideNoArmorBone = model.getBone("flower_side_no_armor").orElse(null);
        var flowerBackNoArmorBone = model.getBone("flower_lower_back_no_armor").orElse(null);
        var flowerHeadWithArmorBone = model.getBone("flower_head_with_armor").orElse(null);
        var flowerSideWithArmorBone = model.getBone("flower_side_with_armor").orElse(null);
        var flowerBackWithArmorBone = model.getBone("flower_lower_back_with_armor").orElse(null);

        // --- Statue / AI Disabled Logic ---
        var closedEyesBone = model.getBone("closed_eyes").orElse(null);
        if (closedEyesBone != null) {
            snapshots.get(closedEyesBone).skipRender(entity.isNoAi()); // Ensure eyes remain open in t-pose
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

        if (flowerHeadNoArmorBone != null) snapshots.get(flowerHeadNoArmorBone).skipRender(flowerType != 1 || useArmorFlowers);
        if (flowerSideNoArmorBone != null) snapshots.get(flowerSideNoArmorBone).skipRender(flowerType != 2 || useArmorFlowers);
        if (flowerBackNoArmorBone != null) snapshots.get(flowerBackNoArmorBone).skipRender(flowerType != 3 || useArmorFlowers);

        if (flowerHeadWithArmorBone != null) snapshots.get(flowerHeadWithArmorBone).skipRender(flowerType != 1 || !useArmorFlowers);
        if (flowerSideWithArmorBone != null) snapshots.get(flowerSideWithArmorBone).skipRender(flowerType != 2 || !useArmorFlowers);
        if (flowerBackWithArmorBone != null) snapshots.get(flowerBackWithArmorBone).skipRender(flowerType != 3 || !useArmorFlowers);

        // --- Cheek Pouch Logic ---
        if (leftCheekDefBone != null && leftCheekInfBone != null) {
            boolean leftFull = entity.isLeftCheekFull();
            snapshots.get(leftCheekDefBone).skipRender(leftFull);
            snapshots.get(leftCheekInfBone).skipRender(!leftFull);
        }
        if (rightCheekDefBone != null && rightCheekInfBone != null) {
            boolean rightFull = entity.isRightCheekFull();
            snapshots.get(rightCheekDefBone).skipRender(rightFull);
            snapshots.get(rightCheekInfBone).skipRender(!rightFull);
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

            snapshots.get(rightEarBone).skipRender(shouldHideEar);

            if (acornHatBone != null) {
                snapshots.get(acornHatBone).skipRender(!shouldShowHat);
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

            snapshots.get(rootBone).setScaleX(currentBaseScale);
            snapshots.get(rootBone).setScaleZ(currentBaseScale);

            // Override y scale for dynamic squash and stretch if shoulder pet
            if (entity.isShoulderPet()) {
                snapshots.get(rootBone).setScaleY(currentBaseScale * entity.dynamicScaleY);
            } else {
                snapshots.get(rootBone).setScaleY(currentBaseScale);
            }

            snapshots.get(headParentBone).setScaleX(currentHeadScale);
            snapshots.get(headParentBone).setScaleY(currentHeadScale);
            snapshots.get(headParentBone).setScaleZ(currentHeadScale);

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
                float partialTick = renderPass.renderState().getPartialTick();
                pitchOffset = Mth.lerp(partialTick, entity.prevClientSwimPitch, entity.clientSwimPitch);
            } else if (entity.clientFallPitchProgress > 0.0f || entity.prevClientFallPitchProgress > 0.0f) {
                float partialTick = renderPass.renderState().getPartialTick();
                float lerpedProgress = Mth.lerp(partialTick, entity.prevClientFallPitchProgress, entity.clientFallPitchProgress);

                // Natural Fall Mode: Procedural Nose Dive (Cosine Interpolation)
                float interpolated = (1.0f - Mth.cos(lerpedProgress * (float) Math.PI)) * 0.5f;

                // Rotate to face downward
                pitchOffset = (float) (-Math.PI / 2.0) * interpolated;
            }

            // Absolute assignment
            snapshots.get(rootBone).setRotX(pitchOffset);

            // Easter Egg rotation if applicable
            if (isMoonwalking) {
                snapshots.get(rootBone).setRotY((float) Math.PI);
            } else {
                snapshots.get(rootBone).setRotY(0.0f);
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Keyframe Effects
     * ────────────────────────────────────────────────────────────────────────────*/

    /** Bone world position captured this pass, as the JOML vector the effect code expects. */
    private static java.util.Optional<Vector3d> bonePos(HamsterRenderState state, String boneName) {
        Vec3 v = state.bonePositions.get(boneName);
        return v == null ? java.util.Optional.empty() : java.util.Optional.of(new Vector3d(v.x, v.y, v.z));
    }

    private void handleParticleKeyframes(HamsterEntity animatable, HamsterRenderState renderState) {
        RandomSource random = animatable.getRandom();
        switch (animatable.particleEffectId) {
            case "attack_poof":
                bonePos(renderState, "left_foot").ifPresent(pos -> {
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
                bonePos(renderState, "nose").ifPresent(pos -> {
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
                bonePos(renderState, "nose").ifPresent(pos -> {
                    // 1. Item Particles (if holding item)
                    ItemStack mouthStack = animatable.getMouthItemStack();
                    if (!mouthStack.isEmpty()) {
                        for (int i = 0; i < 5; i++) {
                            animatable.level().addParticle(
                                    new ItemParticleOption(ParticleTypes.ITEM, mouthStack.getItem()),
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

    private void handleSoundKeyframes(HamsterEntity animatable, HamsterRenderState renderState) {
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
