package net.dawson.adorablehamsterpets.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterProjectileEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.dawson.adorablehamsterpets.util.HamsterState;
import net.minecraft.client.renderer.MultiBufferSource;
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
public class HamsterProjectileRenderer extends EntityRenderer<HamsterProjectileEntity> {

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
    public Identifier getTextureLocation(HamsterProjectileEntity entity) {
        // Fallback; never actually drawn
        return Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/fur_pattern.png");
    }

    @Override
    public boolean shouldRender(HamsterProjectileEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(HamsterProjectileEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        // Ensure dummy exists
        if (entity.clientDummyHamster == null) {
            entity.clientDummyHamster = ModEntities.HAMSTER.get().create(entity.level());
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
                            ContainerHelper.loadAllItems(state.inventoryNbt(), entity.clientDummyHamster.getItems(), entity.level().registryAccess());
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

            // Delegate to real renderer
            this.hamsterRenderer.render(entity.clientDummyHamster, livingYaw, tickDelta, matrices, vertexConsumers, light);
        }

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
