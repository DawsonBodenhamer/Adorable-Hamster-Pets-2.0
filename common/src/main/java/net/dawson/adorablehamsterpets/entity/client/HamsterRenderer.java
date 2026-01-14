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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;

public class HamsterRenderer extends GeoEntityRenderer<HamsterEntity> {

    private final float adultShadowRadius;
    private static final Map<Integer, HamsterCleaningSoundInstance> activeCleaningSounds = new HashMap<>();

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
        // Adds the entity's ID to a set to determine which entities are no longer being rendered.
        AdorableHamsterPetsClient.onHamsterRendered(entity.getId());

        // --- 4. Smooth Snow Layer Height Adjustment ---
        poseStack.push();
        float targetYOffset = 0.0f;
        BlockPos pos = entity.getBlockPos();
        BlockState blockState = entity.getWorld().getBlockState(pos);

        // If the block is a snow layer, apply a fixed offset equal to one layer's height.
        if (blockState.isOf(Blocks.SNOW)) {
            targetYOffset = 1.0f / 8.0f;
        }

        // Smoothly interpolate the current offset towards the target offset.
        // The 0.25f factor controls the speed of the transition.
        entity.renderedSnowYOffset += (targetYOffset - entity.renderedSnowYOffset) * 0.15f;
        poseStack.translate(0.0, entity.renderedSnowYOffset, 0.0);

        // --- 5. Force Animation Update for In-World Entities ---
        // This prevents animations from shoulder-pet dummies (FeatureRenderer)
        // from "bleeding" onto in-world hamsters during Flashback replays.
        if (!entity.isShoulderPet()) {
            software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache = entity.getAnimatableInstanceCache();
            if (cache != null) {
                software.bernie.geckolib.core.animation.AnimatableManager<?> manager = cache.getManagerForId(entity.getId());
                if (manager != null) {
                    manager.updatedAt(0.0);
                }
            }
        }

        // --- 6. Call Superclass Method ---
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // --- 7. Pop Matrix for Snow Adjustment ---
        poseStack.pop();
    }

    @Override
    public void preRender(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model, @Nullable VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        model.getBone("left_foot").ifPresent(bone -> bone.setTrackingMatrices(true));
        model.getBone("nose").ifPresent(bone -> bone.setTrackingMatrices(true));
    }

    /**
     * Performs the final rendering steps for the entity, including handling post-animation particle effects.
     * <p>
     * This method polls a transient {@code particleEffectId} flag on the animatable entity each frame.
     * If the flag is set (by a particle keyframe event), it spawns the corresponding particle effect
     * at the animated bone's calculated world position and then immediately resets the flag to {@code null}
     * to prevent re-triggering.
     */
    @Override
    public void renderFinal(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        if (animatable.particleEffectId != null) {
            Random random = animatable.getRandom();
            switch (animatable.particleEffectId) {
                case "attack_poof":
                    model.getBone("left_foot").ifPresent(bone -> {
                        Vector3d pos = bone.getWorldPosition();

                        for (int i = 0; i < 8; ++i) {
                            double d = random.nextGaussian() * 0.1;
                            double e = random.nextGaussian() * 0.2;
                            double f = random.nextGaussian() * 0.1;
                            animatable.getWorld().addParticle(ParticleTypes.POOF,
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
                                    0.0, 0.0, 0.0); // Speed is default
                        }
                    });
                    break;
            }
            animatable.particleEffectId = null;
        }
        // --- Handle Sound Spawning via Flag ---
        if (animatable.soundEffectId != null) {
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
            // Reset the flag
            animatable.soundEffectId = null;
        }
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, HamsterEntity animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // First, call the super method to render the bone itself, passing the correct 14 arguments
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // Get the stolen item stack directly from the animatable entity
        ItemStack itemHeldInMouthStack = animatable.getInterestItemStack();

        // Now, check if this is the bone we want to attach our item to
        if (bone.getName().equals("nose") && animatable.isHoldingInterestItem()) {
            ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

            poseStack.push();
            // Move the matrix to the bone's position and apply its transformations
            poseStack.translate(bone.getPosX(), bone.getPosY(), bone.getPosZ());
            poseStack.multiply(new Quaternionf().rotateZ(bone.getRotZ()));
            poseStack.multiply(new Quaternionf().rotateY(bone.getRotY()));
            poseStack.multiply(new Quaternionf().rotateX(bone.getRotX()));
            poseStack.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());

            // --- MANUAL ADJUSTMENTS ---
            // These transformations are applied *relative to the nose bone's pivot point*.
            // The coordinate system is: +X is right, +Y is up, +Z is backward.

            // poseStack.translate(X, Y, Z): Moves the item.
            // X: Positive values move it to the hamster's right. Negative to the left.
            // Y: Positive values move it up. Negative moves it down.
            // Z: Positive values move it towards the hamster's tail. Negative values move it forward, away from the tail.
            // Using negative Z value to fix the item appearing at the tail.
            poseStack.translate(0, 0.13F, -0.2F); // If Math.toRadians = 90 (top of item pointing out), use (0, 0.22F, -0.4F) instead.

            // poseStack.scale(X, Y, Z): Resizes the item.
            // Values > 1.0 make it bigger. Values < 1.0 make it smaller.
            poseStack.scale(0.7f, 0.7f, 0.7f);


            // poseStack.multiply(...): Rotates the item. This is complex.
            // Rotates the item -90 degrees on its X-axis, which makes it lay flat as if the hamster is holding the top part of the item in its mouth, with the "bottom" of the item pointing out.
            poseStack.multiply(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-90), 1, 0, 0)));

            // Render the item from the DataTracker
            itemRenderer.renderItem(itemHeldInMouthStack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, packedLight, packedOverlay, poseStack, bufferSource, animatable.getWorld(), animatable.getId());

            poseStack.pop();
        }
    }
}