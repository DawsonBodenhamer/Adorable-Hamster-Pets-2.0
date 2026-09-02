package net.dawson.adorablehamsterpets.entity.client.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dawson.adorablehamsterpets.client.shoulder.ShoulderHamsterRenderData;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderHamsterState;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.util.HamsterInventoryNbt;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.dawson.adorablehamsterpets.util.HamsterState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Draws hamsters riding on the player's shoulders/head.
 *
 * <p>26.2 port: mirrors vanilla's ParrotOnShoulderLayer. The layer only sees the
 * player's render state, so the snapshots come from
 * {@code ShoulderHamsterExtractor} (run from AvatarRenderer.extractRenderState)
 * and are stored on the state via {@link ShoulderHamsterRenderData}.
 */
public class HamsterShoulderFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final float HAMSTER_SHOULDER_SCALE = 0.8f;

    public HamsterShoulderFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float yRot, float xRot) {
        if (!(state instanceof ShoulderHamsterRenderData data)) return;
        var hamsters = data.adorablehamsterpets$getShoulderHamsters();
        if (hamsters.isEmpty()) return;

        CameraRenderState camera = currentCamera();
        for (var entry : hamsters.entrySet()) {
            ShoulderLocation location = entry.getKey();
            ShoulderHamsterRenderData.Entry hamster = entry.getValue();
            HamsterRenderer renderer = rendererFor(hamster);
            if (renderer == null) continue;

            poseStack.pushPose();
            boolean chestplate = data.adorablehamsterpets$isWearingChestplate();
            boolean slim = data.adorablehamsterpets$isSlim();
            switch (location) {
                case RIGHT_SHOULDER -> {
                    this.getParentModel().rightArm.translateAndRotate(poseStack);
                    float xOffset = chestplate ? -0.18F : (slim ? -0.08F : -0.12F);
                    float yOffset = chestplate ? -0.18F : -0.12F;
                    poseStack.translate(xOffset, yOffset, -0.016F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
                }
                case LEFT_SHOULDER -> {
                    this.getParentModel().leftArm.translateAndRotate(poseStack);
                    float xOffset = chestplate ? 0.18F : (slim ? 0.08F : 0.12F);
                    float yOffset = chestplate ? -0.18F : -0.12F;
                    poseStack.translate(xOffset, yOffset, -0.016F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-15.0F));
                }
                case HEAD -> {
                    this.getParentModel().head.translateAndRotate(poseStack);
                    poseStack.translate(0.0F, -0.5F, -0.05F);
                }
            }
            poseStack.translate(0.0F, -hamster.renderOffsetY(), 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(HAMSTER_SHOULDER_SCALE, HAMSTER_SHOULDER_SCALE, HAMSTER_SHOULDER_SCALE);
            // Face the way the player's body faces (was passed as entityYaw to render())
            hamster.renderState().bodyRot = data.adorablehamsterpets$getBodyYaw();
            hamster.renderState().yRot = data.adorablehamsterpets$getBodyYaw();
            renderer.submit(hamster.renderState(), poseStack, collector, camera);
            poseStack.popPose();
        }
    }

    private static HamsterRenderer rendererFor(ShoulderHamsterRenderData.Entry entry) {
        HamsterEntity dummy = entry.renderState().entity;
        if (dummy == null) return null;
        return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy) instanceof HamsterRenderer r ? r : null;
    }

    /** GeckoLib's submit wants a camera state; the layer isn't handed one, so mirror the live camera. */
    private static CameraRenderState currentCamera() {
        CameraRenderState cam = new CameraRenderState();
        Camera live = Minecraft.getInstance().gameRenderer.mainCamera();
        cam.pos = live.position();
        cam.blockPos = live.blockPosition();
        cam.xRot = live.xRot();
        cam.yRot = live.yRot();
        cam.initialized = true;
        return cam;
    }

    /**
     * Applies visual data from the stored shoulder NBT to a specific dummy entity.
     * This ensures the rendered model has the correct appearance (variant, age, cheeks, etc.).
     */
    public static void applyHamsterState(HamsterEntity dummyHamster, HamsterState data, Player owner) {
        // --- Apply Visual Data ---
        dummyHamster.setGenome(HamsterGenome.readFromNbt(data.genomeNbt()));
        dummyHamster.setLeftCheekFull((data.hamsterFlags() & HamsterEntity.LEFT_CHEEK_FULL_FLAG) != 0);
        dummyHamster.setRightCheekFull((data.hamsterFlags() & HamsterEntity.RIGHT_CHEEK_FULL_FLAG) != 0);
        dummyHamster.getEntityData().set(HamsterEntity.FLOWER_POS, data.flowerPosition());
        dummyHamster.getEntityData().set(HamsterEntity.ANIMATION_PERSONALITY_ID, data.animationPersonalityId());
        dummyHamster.setArmorVisible(data.armorVisible());
        dummyHamster.setAge(data.breedingAge());
        dummyHamster.getEntityData().set(HamsterEntity.EXACT_AGE, data.breedingAge());
        dummyHamster.setBaby(data.breedingAge() < 0);

        // --- Apply Core Flags ---
        // Restore all states from when the hamster was picked up
        dummyHamster.getEntityData().set(HamsterEntity.HAMSTER_FLAGS, data.hamsterFlags());

        // --- Mark as shoulder hamster for animation controller ---
        dummyHamster.setShoulderPet(true);

        // --- Set Custom Name for Easter Eggs ---
        dummyHamster.setCustomName(null);
        data.customName().ifPresent(name -> {
            if (!name.isEmpty()) {
                dummyHamster.setCustomName(Component.literal(name));
            }
        });

        // --- Apply Inventory for Armor/Accessories ---
        // Clear the inventory first to avoid ghost items if the data is empty/changed
        dummyHamster.getItems().clear();

        if (!data.inventoryNbt().isEmpty()) {
            // Use the owner's registry manager since we are on the client
            HolderLookup.Provider registries = owner.registryAccess();

            // Populate the dummy's inventory from NBT
            HamsterInventoryNbt.load(data.inventoryNbt(), dummyHamster.getItems());

            // Force update the tracked data fields so the RenderLayers can see the items
            HamsterInventoryUtil.syncEquipmentTrackers(dummyHamster);
        }

        // --- Set Ownership for Animation Logic ---
        dummyHamster.setOwnerReference((owner.getUUID()) == null ? null : net.minecraft.world.entity.EntityReference.of(owner.getUUID()));
        dummyHamster.setTame(true, false); // No attribute update needed
    }

    /**
     * Applies all pre-calculated state to the dummy entity right before rendering.
     * This is the final step that bridges the client-thread logic with the render-thread object.
     */
    public static void updateDummyState(HamsterEntity dummyHamster, HamsterState nbtData, ClientShoulderHamsterData clientData, ShoulderLocation location, Player owner) {
        // --- 1. Apply visual data from NBT ---
        applyHamsterState(dummyHamster, nbtData, owner);

        // --- 2. Apply animation clock from client data ---
        dummyHamster.tickCount = clientData.getAnimationAge(location);

        // Sync position so audio/particle keyframes play at the player's location instead of world origin
        dummyHamster.setPos(owner.getX(), owner.getY(), owner.getZ());

        // --- 3. Apply animation state from client data ---
        ShoulderHamsterState state = clientData.getHamsterState(location);
        if (state != null) {
            ShoulderAnimationState currentState = state.getCurrentState();
            dummyHamster.getEntityData().set(HamsterEntity.SHOULDER_ANIMATION_STATE, currentState.ordinal());
            dummyHamster.setSitting(currentState == ShoulderAnimationState.SITTING, true);
        }

        // --- 4. Inform dummy of its location for animation controller ---
        dummyHamster.shoulderLocation = location;
    }
}
