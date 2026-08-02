package net.dawson.adorablehamsterpets.entity.client.renderer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterProjectileEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.dawson.adorablehamsterpets.util.HamsterState;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventories;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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

    public HamsterProjectileRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.hamsterRenderer = new HamsterRenderer(ctx);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public Identifier getTexture(HamsterProjectileEntity entity) {
        // Fallback; never actually drawn
        return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/fur_pattern.png");
    }

    @Override
    public boolean shouldRender(HamsterProjectileEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(HamsterProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Ensure dummy exists
        if (entity.clientDummyHamster == null) {
            entity.clientDummyHamster = ModEntities.HAMSTER.get().create(entity.getWorld());
            if (entity.clientDummyHamster != null) {
                entity.clientDummyHamster.setNoGravity(true);
                entity.clientDummyHamster.setAiDisabled(true); // Disable AI ticking
                entity.clientDummyHamster.isProjectileDummy = true;

                // Decode NBT for visuals
                NbtCompound nbt = entity.getHamsterData();
                if (nbt != null && !nbt.isEmpty()) {
                    HamsterState.fromNbt(nbt).ifPresent(state -> {
                        entity.clientDummyHamster.setGenome(HamsterGenome.readFromNbt(state.genomeNbt()));
                        entity.clientDummyHamster.setBaby(state.breedingAge() < 0);
                        entity.clientDummyHamster.getDataTracker().set(HamsterEntity.FLOWER_POS, state.flowerPosition());
                        entity.clientDummyHamster.setArmorVisible(state.armorVisible());

                        if (!state.inventoryNbt().isEmpty()) {
                            entity.clientDummyHamster.getItems().clear();
                            Inventories.readNbt(state.inventoryNbt(), entity.clientDummyHamster.getItems());
                            HamsterInventoryUtil.syncEquipmentTrackers(entity.clientDummyHamster);
                        }
                    });
                }
            }
        }

        if (entity.clientDummyHamster != null) {
            // Sync physics states
            entity.clientDummyHamster.setVelocity(entity.getVelocity());
            entity.clientDummyHamster.setPosition(entity.getX(), entity.getY(), entity.getZ());
            entity.clientDummyHamster.age = entity.age;

            // Force yaw sync
            Vec3d vel = entity.getVelocity();
            float livingYaw = (float)(MathHelper.atan2(-vel.x, vel.z) * MathHelper.DEGREES_PER_RADIAN);

            entity.clientDummyHamster.setYaw(livingYaw);
            entity.clientDummyHamster.bodyYaw = livingYaw;
            entity.clientDummyHamster.prevBodyYaw = livingYaw;
            entity.clientDummyHamster.headYaw = livingYaw;
            entity.clientDummyHamster.prevHeadYaw = livingYaw;

            // Delegate to real renderer
            this.hamsterRenderer.render(entity.clientDummyHamster, livingYaw, tickDelta, matrices, vertexConsumers, light);
        }

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
