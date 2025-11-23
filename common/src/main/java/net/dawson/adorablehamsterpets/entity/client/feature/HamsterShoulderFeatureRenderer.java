package net.dawson.adorablehamsterpets.entity.client.feature;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.component.HamsterShoulderData;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.renderer.ShoulderHamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.EnumMap;
import java.util.Map;

/**
 * Renders a Hamster model on the player's shoulder if shoulder data is present.
 * Handles scaling for baby/adult hamsters and texture variations.
 */

public class HamsterShoulderFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    // --- 1. Constants ---
    private static final float HAMSTER_SHOULDER_SCALE = 0.8f;

    // --- 2. Fields (Lazy Initialization) ---
    private final Map<ShoulderLocation, HamsterEntity> dummyHamsters = new EnumMap<>(ShoulderLocation.class);
    private final Map<ShoulderLocation, ShoulderHamsterRenderer> hamsterRenderers = new EnumMap<>(ShoulderLocation.class);

    // --- 3. Constructor ---
    public HamsterShoulderFeatureRenderer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context
    ) {
        super(context);
    }

    // --- 4. Public Methods (Overrides from FeatureRenderer) ---
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;
        if (!playerAccessor.hasAnyShoulderHamster()) {
            return;
        }

        // --- Lazy Initialization (checks if the map is empty) ---
        if (this.dummyHamsters.isEmpty()) {
            initializeDummies(player.getWorld());
        }

        // --- Get the per-player data holder ---
        ClientShoulderHamsterData clientData = playerAccessor.adorablehamsterpets$getClientShoulderData();
        if (clientData == null) return; // Safety check

        // --- Render Hamster for Each Occupied Slot ---
        for (ShoulderLocation location : ShoulderLocation.values()) {
            NbtCompound shoulderNbt = playerAccessor.getShoulderHamster(location);
            if (!shoulderNbt.isEmpty()) {
                HamsterShoulderData.fromNbt(shoulderNbt).ifPresent(shoulderData ->
                        renderShoulderHamster(matrices, vertexConsumers, light, player, shoulderData, tickDelta, clientData, location)
                );
            }
        }
    }

    // --- 5. Private Helper Methods ---
    /**
     * Applies visual data from the stored shoulder NBT to a specific dummy entity.
     * This ensures the rendered model has the correct appearance (variant, age, cheeks, etc.).
     */
    private void applyShoulderData(HamsterEntity dummyHamster, HamsterShoulderData data, PlayerEntity owner) {
        // --- Mark this as a shoulder pet for the animation controller ---
        dummyHamster.setShoulderPet(true);

        // --- Apply Visual Data ---
        dummyHamster.setVariant(data.variantId());
        dummyHamster.setLeftCheekFull((data.hamsterFlags() & HamsterEntity.LEFT_CHEEK_FULL_FLAG) != 0);
        dummyHamster.setRightCheekFull((data.hamsterFlags() & HamsterEntity.RIGHT_CHEEK_FULL_FLAG) != 0);
        dummyHamster.getDataTracker().set(HamsterEntity.PINK_PETAL_TYPE, data.pinkPetalType());
        dummyHamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, data.animationPersonalityId());
        dummyHamster.setBreedingAge(data.breedingAge());

        // --- Set Ownership for Animation Logic ---
        dummyHamster.setOwnerUuid(owner.getUuid());
        dummyHamster.setTamed(true, false); // No attribute update needed
    }

    /**
     * Renders the GeckoLib model on the player's shoulder with appropriate transformations and animations.
     */
    private void renderShoulderHamster(
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            AbstractClientPlayerEntity player, HamsterShoulderData shoulderData, float tickDelta,
            ClientShoulderHamsterData clientData, ShoulderLocation location
    ) {
        // --- 1. Get the correct dummy and renderer for this specific location ---
        HamsterEntity dummyHamster = this.dummyHamsters.get(location);
        ShoulderHamsterRenderer hamsterRenderer = this.hamsterRenderers.get(location);
        if (dummyHamster == null || hamsterRenderer == null) return;

        // --- 2. Update Dummy Entity State from Pre-Ticked Data ---
        updateDummyState(dummyHamster, shoulderData, clientData, location, player);

        // Manually reset the animation manager's update timer.
        // This forces GeckoLib to perform a full animation update for this specific dummy instance,
        // overwriting any polluted state from other entities rendered in the same batch by Iris.
        // WITHOUT THIS BLOCK, IRIS WILL BREAK THE SHOULDER HAMSTERS
        AnimatableInstanceCache cache = dummyHamster.getAnimatableInstanceCache();
        if (cache != null) {
            AnimatableManager<?> manager = cache.getManagerForId(dummyHamster.getId());
            if (manager != null) {
                manager.updatedAt(0);
            }
        }

        // --- 3. Physics Simulation ---
        float renderOffsetY = clientData.getRenderOffsetY(location, tickDelta);
        float renderScaleY = clientData.getRenderScaleY(location, tickDelta);
        dummyHamster.dynamicScaleY = renderScaleY; // Set the scale on the dummy entity

        matrices.push();

        // --- 4. Apply Transformations Based on Location ---
        ItemStack chestStack = player.getInventory().getArmorStack(2);
        boolean isWearingChestplate = !chestStack.isEmpty() && !chestStack.isOf(Items.ELYTRA);
        boolean isSlim = player.getSkinTextures().model() == SkinTextures.Model.SLIM;

        switch (location) {
            case RIGHT_SHOULDER -> {
                this.getContextModel().rightArm.rotate(matrices);
                float xOffset, yOffset;
                if (isWearingChestplate) {
                    // Universal offsets for when armor is worn
                    xOffset = -0.18F;
                    yOffset = -0.18F;
                } else {
                    // Original offsets based on player model
                    xOffset = isSlim ? -0.08F : -0.12F;
                    yOffset = -0.12F;
                }
                matrices.translate(xOffset, yOffset, -0.016F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F));
            }
            case LEFT_SHOULDER -> {
                this.getContextModel().leftArm.rotate(matrices);
                float xOffset, yOffset;
                if (isWearingChestplate) {
                    // Universal offsets for when armor is worn
                    xOffset = 0.18F;
                    yOffset = -0.18F;
                } else {
                    // Original offsets based on player model
                    xOffset = isSlim ? 0.08F : 0.12F;
                    yOffset = -0.12F;
                }
                matrices.translate(xOffset, yOffset, -0.016F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-15.0F));
            }
            case HEAD -> {
                this.getContextModel().head.rotate(matrices);
                matrices.translate(0.0F, -0.5F, -0.05F);
            }
        }

        // --- 5. Apply Physics Offset ---
        // The negative sign is crucial to convert our simulation's "up is positive"
        // into the model's "up is negative" local coordinate space.
        matrices.translate(0.0F, -renderOffsetY, 0.0F);

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        matrices.scale(HAMSTER_SHOULDER_SCALE, HAMSTER_SHOULDER_SCALE, HAMSTER_SHOULDER_SCALE);
        float renderYaw = 180.0F - player.getBodyYaw();

        // --- 6. Render the Dummy Entity ---
        hamsterRenderer.render(dummyHamster, renderYaw, tickDelta, matrices, vertexConsumers, light);

        matrices.pop();
    }

    /**
     * Calculates the absolute world-space Y-coordinate of a player model's bone.
     *
     * @param matrices   The current MatrixStack from the render method.
     * @param anchorBone The ModelPart (e.g., head, rightArm) to measure.
     * @return The world-space Y-coordinate of the bone's pivot point.
     */
    private double getAnchorBoneWorldY(MatrixStack matrices, ModelPart anchorBone) {
        MatrixStack tempMatrices = new MatrixStack();
        tempMatrices.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        anchorBone.rotate(tempMatrices);
        Matrix4f finalMatrix = tempMatrices.peek().getPositionMatrix();
        // The Y translation component is at index m31 in a Matrix4f
        return finalMatrix.m31();
    }

    /**
     * Applies all pre-calculated state to the dummy entity right before rendering.
     * This is the final step that bridges the client-thread logic with the render-thread object.
     */
    private void updateDummyState(HamsterEntity dummyHamster, HamsterShoulderData nbtData, ClientShoulderHamsterData clientData, ShoulderLocation location, PlayerEntity owner) {
        // --- 1. Apply visual data from NBT ---
        applyShoulderData(dummyHamster, nbtData, owner);

        // --- 2. Apply animation clock from client data ---
        dummyHamster.age = clientData.getAnimationAge(location);

        // --- 3. Apply animation state from client data ---
        ShoulderHamsterState state = clientData.getHamsterState(location);
        if (state != null) {
            ShoulderAnimationState currentState = state.getCurrentState();
            dummyHamster.getDataTracker().set(HamsterEntity.SHOULDER_ANIMATION_STATE, currentState.ordinal());
            dummyHamster.setSitting(currentState == ShoulderAnimationState.SITTING, true);
        }

        // --- 4. Inform dummy of its location for animation controller ---
        dummyHamster.shoulderLocation = location;
    }

    /**
     * Initializes the dummy hamster entities and their specialized renderers, one for each shoulder location.
     * This is called once, the first time the feature needs to be rendered.
     */
    private void initializeDummies(World world) {
        if (world == null) return; // Cannot proceed without a world instance

        // We need an EntityRendererFactory.Context to create the renderers.
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRendererFactory.Context context = new EntityRendererFactory.Context(
                client.getEntityRenderDispatcher(),
                client.getItemRenderer(),
                client.getBlockRenderManager(),
                client.getEntityRenderDispatcher().getHeldItemRenderer(),
                client.getResourceManager(),
                client.getEntityModelLoader(),
                client.textRenderer
        );

        for (ShoulderLocation location : ShoulderLocation.values()) {
            // Create a unique dummy entity for this location
            HamsterEntity dummy = new HamsterEntity(ModEntities.HAMSTER.get(), world);
            dummy.setNoGravity(true);
            dummy.setSilent(true);
            this.dummyHamsters.put(location, dummy);

            // Create a unique renderer for this location
            this.hamsterRenderers.put(location, new ShoulderHamsterRenderer(context));
        }
    }
}