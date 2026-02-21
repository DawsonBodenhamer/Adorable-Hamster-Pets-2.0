package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.UUID;

/**
 * Handles specialized physics, trajectory simulations, and attribute calculations for hamsters.
 */
public final class HamsterPhysicsUtil {

    // 1.20.1: Use UUIDs for Attribute Modifiers
    private static final UUID ARMOR_SPEED_BOOST_UUID = UUID.fromString("74ba7508-3010-449e-97c7-573531b7987e");
    private static final UUID ARMOR_KNOCKBACK_RESISTANCE_UUID = UUID.fromString("a8470a74-d2ca-4c8d-806d-6215d290680d");

    public static final double THROWN_GRAVITY = -0.05;

    private HamsterPhysicsUtil() {}

    /**
     * Simulates the hamster's trajectory 1 second (20 ticks) into the future
     * Plays the "Incoming" sound at the target location if an impact is predicted
     */
    public static void simulateTrajectoryAndCheckSound(HamsterEntity hamster) {
        Vec3d simPos = hamster.getPos();
        Vec3d simVel = hamster.getVelocity();

        // Simulate up to 20 ticks ahead
        for (int i = 1; i <= 20; i++) {
            if (!hamster.hasNoGravity()) {
                simVel = simVel.add(0.0, THROWN_GRAVITY, 0.0);
            }

            Vec3d nextPos = simPos.add(simVel);

            // 1. Block Collision Check
            HitResult blockHit = hamster.getWorld().raycast(new RaycastContext(
                    simPos,
                    nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    hamster
            ));

            // 2. Entity Collision Check
            EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                    hamster.getWorld(),
                    hamster,
                    simPos,
                    nextPos,
                    hamster.getBoundingBox().stretch(simVel).expand(1.0),
                    hamster::canHitEntity
            );

            Vec3d impactPos = null;

            if (entityHit != null) {
                impactPos = entityHit.getPos();
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                impactPos = blockHit.getPos();
            }

            if (impactPos != null) {
                // Collision predicted in 'i' ticks
                if (hamster.throwTicks + i >= 20) {
                    hamster.getWorld().playSound(null, impactPos.x, impactPos.y, impactPos.z, ModSounds.HAMSTER_INCOMING.get(), SoundCategory.NEUTRAL, 1.0f, 1.0f);
                    AdorableHamsterPets.LOGGER.debug("Played Incoming sound at target: {}", impactPos);
                }
                hamster.setHasPlayedIncomingSound(true);
                return;
            }

            simPos = nextPos;
        }
    }

    /**
     * Plays an impact sound for all players within range, bypassing vanilla attenuation to ensure
     * consistent audibility across distances. Uses a custom volume gradient to mimic natural falloff
     * while maintaining clarity at long ranges. Checks for non-organic armor and plays a shield block sound if present.
     */
    public static void broadcastImpactSound(HamsterEntity hamster, SoundEvent sound, float pitch) {
        if (hamster.getWorld().isClient()) return;

        double impactX = hamster.getX();
        double impactY = hamster.getY();
        double impactZ = hamster.getZ();

        SoundEvent armorSound = null;
        float armorPitch = 1.0f;

        if (hamster.getItems().size() > HamsterInventoryUtil.ARMOR_SLOT_INDEX) {
            ItemStack armorStack = hamster.getItems().get(HamsterInventoryUtil.ARMOR_SLOT_INDEX);
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
                if (armorItem.getMaterial() != HamsterArmorItem.HamsterArmorMaterial.ACORN) {
                    armorSound = SoundEvents.BLOCK_BELL_USE;
                    armorPitch = 2.0f + hamster.getRandom().nextFloat() * 0.5f;
                }
            }
        }

        for (ServerPlayerEntity player : ((ServerWorld) hamster.getWorld()).getPlayers()) {
            double distSq = player.squaredDistanceTo(impactX, impactY, impactZ);

            if (distSq <= 2500) { // 50 blocks squared
                double distance = Math.sqrt(distSq);
                float volume;

                if (distance <= 16.0) {
                    // Stage 1: Close range (0 to 16 blocks) - Linear 1.0 -> 0.18
                    volume = 1.0F - (0.82F * (float) (distance / 16.0));
                } else {
                    // Stage 2: Distant range (16 to 50 blocks) - Linear 0.18 -> 0.10
                    float remainingProgress = (float) (distance - 16.0) / 34.0F;
                    volume = 0.18F - (0.08F * remainingProgress);
                }

                volume = MathHelper.clamp(volume, 0.10F, 1.0F);

                // 1.20.1: Use ModPackets.CHANNEL
                ModPackets.CHANNEL.sendToPlayer(player, new ModPackets.PlayDistantSoundS2CPacket(sound.getId(), volume, pitch));

                if (armorSound != null) {
                    float armorVolume = Math.min(1.0f, volume * 0.5f);
                    ModPackets.CHANNEL.sendToPlayer(player, new ModPackets.PlayDistantSoundS2CPacket(armorSound.getId(), armorVolume, armorPitch));
                }
            }
        }
    }

    /**
     * Evaluates and updates dynamic attribute modifiers based on current armor and config
     */
    public static void updateArmorModifiers(HamsterEntity hamster, ItemStack armorStack) {
        EntityAttributeInstance speedAttribute = hamster.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        EntityAttributeInstance knockbackAttribute = hamster.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);

        boolean perksEnabled = Configs.AHP.enableArmorPerks.get();
        boolean shouldHaveSpeed = false;
        boolean shouldHaveKnockback = false;

        if (perksEnabled && !armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
            HamsterArmorItem.HamsterArmorMaterial material = armorItem.getMaterial();
            if (material == HamsterArmorItem.HamsterArmorMaterial.GOLD) {
                shouldHaveSpeed = true;
            } else if (material == HamsterArmorItem.HamsterArmorMaterial.NETHERITE) {
                shouldHaveKnockback = true;
            }
        }

        if (speedAttribute != null) {
            // 1.20.1: Check by UUID
            boolean hasSpeed = speedAttribute.getModifier(ARMOR_SPEED_BOOST_UUID) != null;

            if (shouldHaveSpeed && !hasSpeed) {
                // 1.20.1: Use UUID constructor and MULTIPLY_BASE
                speedAttribute.addTemporaryModifier(new EntityAttributeModifier(
                        ARMOR_SPEED_BOOST_UUID, "Hamster Armor Speed", 0.20D, EntityAttributeModifier.Operation.MULTIPLY_BASE
                ));
            } else if (!shouldHaveSpeed && hasSpeed) {
                speedAttribute.removeModifier(ARMOR_SPEED_BOOST_UUID);
            }
        }

        if (knockbackAttribute != null) {
            // 1.20.1: Check by UUID
            boolean hasKnockback = knockbackAttribute.getModifier(ARMOR_KNOCKBACK_RESISTANCE_UUID) != null;

            if (shouldHaveKnockback && !hasKnockback) {
                // 1.20.1: Use UUID constructor and ADDITION
                knockbackAttribute.addTemporaryModifier(new EntityAttributeModifier(
                        ARMOR_KNOCKBACK_RESISTANCE_UUID, "Hamster Armor KB Resist", 0.5D, EntityAttributeModifier.Operation.ADDITION
                ));
            } else if (!shouldHaveKnockback && hasKnockback) {
                knockbackAttribute.removeModifier(ARMOR_KNOCKBACK_RESISTANCE_UUID);
            }
        }
    }

    /**
     * Calculates total throw damage based on configuration and current armor
     */
    public static float calculateThrowDamage(HamsterEntity hamster, ItemStack armorStack) {
        float damageAmount = Configs.AHP.hamsterThrowDamage.get().floatValue();

        if (Configs.AHP.enableArmorPerks.get() && !armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
            if (armorItem.getMaterial() == HamsterArmorItem.HamsterArmorMaterial.NETHERITE) {
                damageAmount += Configs.AHP.netheriteArmorThrowDamageBonus.get().floatValue();
            }
        }

        return damageAmount;
    }
}