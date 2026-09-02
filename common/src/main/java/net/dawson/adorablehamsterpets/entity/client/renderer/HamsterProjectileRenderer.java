package net.dawson.adorablehamsterpets.entity.client.renderer;

import net.minecraft.world.entity.EntitySpawnReason;
import net.dawson.adorablehamsterpets.util.HamsterInventoryNbt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterProjectileEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.dawson.adorablehamsterpets.util.HamsterState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.phys.Vec3;

/**
 * A specialized renderer for the HamsterProjectileEntity.
 * Unpacks NBT to create a dummy hamster that renders exactly where the projectile is.
 */
public class HamsterProjectileRenderer extends EntityRenderer<HamsterProjectileEntity, HamsterProjectileRenderState> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterRenderer hamsterRenderer;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.hamsterRenderer = new HamsterRenderer(ctx);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean shouldRender(HamsterProjectileEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public HamsterProjectileRenderState createRenderState() {
        return new HamsterProjectileRenderState();
    }

    /**
     * 26.2 port: the dummy hamster is prepared here, while the entity is
     * reachable, and turned into a HamsterRenderState the submit pass draws.
     */
    @Override
    public void extractRenderState(HamsterProjectileEntity entity, HamsterProjectileRenderState renderState, float tickDelta) {
        super.extractRenderState(entity, renderState, tickDelta);
        // Ensure dummy exists
        if (entity.clientDummyHamster == null) {
            entity.clientDummyHamster = ModEntities.HAMSTER.get().create(entity.level(), EntitySpawnReason.LOAD);
            if (entity.clientDummyHamster != null) entity.clientDummyHamster.setId(-2000 - (entity.getId() & 0xFFFF)); // 26.2: dummies need an id
            if (entity.clientDummyHamster != null) {
                entity.clientDummyHamster.setNoGravity(true);
                entity.clientDummyHamster.setNoAi(true); // Disable AI ticking
                entity.clientDummyHamster.isProjectileDummy = true;

                // Decode NBT for visuals
                CompoundTag nbt = entity.getHamsterData();
                if (nbt != null && !nbt.isEmpty()) {
                    HamsterState.fromNbt(nbt).ifPresent(state -> {
                        entity.clientDummyHamster.setGenome(HamsterGenome.readFromNbt(state.genomeNbt()));
                        entity.clientDummyHamster.setBaby(state.breedingAge() < 0);
                        entity.clientDummyHamster.getEntityData().set(HamsterEntity.FLOWER_POS, state.flowerPosition());
                        entity.clientDummyHamster.setArmorVisible(state.armorVisible());

                        if (!state.inventoryNbt().isEmpty()) {
                            entity.clientDummyHamster.getItems().clear();
                            HamsterInventoryNbt.load(state.inventoryNbt(), entity.clientDummyHamster.getItems());
                            HamsterInventoryUtil.syncEquipmentTrackers(entity.clientDummyHamster);
                        }
                    });
                }
            }
        }

        if (entity.clientDummyHamster != null) {
            // Sync physics states
            entity.clientDummyHamster.setDeltaMovement(entity.getDeltaMovement());
            entity.clientDummyHamster.setPos(entity.getX(), entity.getY(), entity.getZ());
            entity.clientDummyHamster.tickCount = entity.tickCount;

            // Force yaw sync
            Vec3 vel = entity.getDeltaMovement();
            float livingYaw = (float)(Mth.atan2(-vel.x, vel.z) * Mth.RAD_TO_DEG);

            entity.clientDummyHamster.setYRot(livingYaw);
            entity.clientDummyHamster.yBodyRot = livingYaw;
            entity.clientDummyHamster.yBodyRotO = livingYaw;
            entity.clientDummyHamster.yHeadRot = livingYaw;
            entity.clientDummyHamster.yHeadRotO = livingYaw;
        }
        renderState.hamster = entity.clientDummyHamster != null
                ? this.hamsterRenderer.createRenderState(entity.clientDummyHamster, tickDelta)
                : null;
    }

    @Override
    public void submit(HamsterProjectileRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(renderState, poseStack, collector, camera);
        if (renderState.hamster != null) {
            this.hamsterRenderer.submit(renderState.hamster, poseStack, collector, camera);
        }
    }
}
