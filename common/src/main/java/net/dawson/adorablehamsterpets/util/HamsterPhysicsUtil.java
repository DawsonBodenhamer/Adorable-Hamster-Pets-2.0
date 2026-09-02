package net.dawson.adorablehamsterpets.util;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterProjectileEntity;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.networking.payload.PlayDistantSoundPayload;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Handles specialized physics, trajectory simulations, and attribute calculations for hamsters.
 */
public final class HamsterPhysicsUtil {

    public static final Identifier ARMOR_SPEED_BOOST_ID = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "armor_speed_boost");
    public static final Identifier ARMOR_KNOCKBACK_RESISTANCE_ID = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "armor_knockback_resistance");

    private HamsterPhysicsUtil() {}

    /**
     * Calculates the smoothed coordinate for a lunging/pouncing animation using quadratic ease-in.
     * Keeps the original Y coordinate to prevent the entity from visually floating or sinking mid-lunge.
     *
     * @param startPos       The initial position.
     * @param targetPos      The target position.
     * @param remainingTicks The ticks remaining in the lunge sequence.
     * @param totalTicks     The total duration of the lunge sequence.
     * @return The interpolated Vec3d coordinate.
     */
    public static Vec3 calculatePouncePosition(Vec3 startPos, Vec3 targetPos, int remainingTicks, int totalTicks) {
        double progress = (double) (totalTicks - remainingTicks) / totalTicks;
        double easedProgress = progress * progress; // Quadratic ease-in

        double newX = startPos.x + easedProgress * (targetPos.x - startPos.x);
        double newZ = startPos.z + easedProgress * (targetPos.z - startPos.z);

        return new Vec3(newX, startPos.y, newZ);
    }

    /**
     * Simulates the hamster's trajectory 1 second (20 ticks) into the future
     * Plays the "Incoming" sound at the target location if an impact is predicted
     */
    public static void simulateTrajectoryAndCheckSound(HamsterProjectileEntity projectileDummy) {
        Vec3 simPos = projectileDummy.position();
        Vec3 simVel = projectileDummy.getDeltaMovement();

        for (int i = 1; i <= 20; i++) {
            if (!projectileDummy.isNoGravity()) {
                simVel = simVel.add(0.0, -Configs.AHP_MAIN.hamsterThrowGravity.get(), 0.0);
            }

            Vec3 nextPos = simPos.add(simVel);

            // 1. Block Collision Check
            HitResult blockHit = projectileDummy.level().clip(new ClipContext(
                    simPos,
                    nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    projectileDummy
            ));

            // 2. Entity Collision Check
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    projectileDummy.level(),
                    projectileDummy,
                    simPos,
                    nextPos,
                    projectileDummy.getBoundingBox().expandTowards(simVel).inflate(1.0),
                    projectileDummy::isHitTargetValid
            );

            Vec3 impactPos = null;

            if (entityHit != null) {
                impactPos = entityHit.getLocation();
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                impactPos = blockHit.getLocation();
            }

            if (impactPos != null) {
                // Collision predicted in 'i' ticks
                if (projectileDummy.tickCount + i >= 20) {
                    projectileDummy.level().playSound(null, impactPos.x, impactPos.y, impactPos.z, ModSounds.HAMSTER_INCOMING.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
                projectileDummy.setHasPlayedIncomingSound(true);
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
        if (hamster.level().isClientSide()) return;

        double impactX = hamster.getX();
        double impactY = hamster.getY();
        double impactZ = hamster.getZ();

        SoundEvent armorSound = null;
        float armorPitch = 1.0f;

        if (hamster.getItems().size() > HamsterInventoryUtil.ARMOR_SLOT_INDEX) {
            ItemStack armorStack = hamster.getItems().get(HamsterInventoryUtil.ARMOR_SLOT_INDEX);
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
                if (armorItem.getMaterial() != HamsterArmorItem.HamsterArmorMaterial.ACORN) {
                    armorSound = SoundEvents.BELL_BLOCK;
                    armorPitch = 2.0f + hamster.getRandom().nextFloat() * 0.5f;
                }
            }
        }

        for (ServerPlayer player : ((ServerLevel) hamster.level()).players()) {
            double distSq = player.distanceToSqr(impactX, impactY, impactZ);

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

                volume = Mth.clamp(volume, 0.10F, 1.0F);

                NetworkManager.sendToPlayer(player, new PlayDistantSoundPayload(sound.getLocation(), volume, pitch));

                if (armorSound != null) {
                    float armorVolume = Math.min(1.0f, volume * 0.5f);
                    NetworkManager.sendToPlayer(player, new PlayDistantSoundPayload(armorSound.getLocation(), armorVolume, armorPitch));
                }
            }
        }
    }

    /**
     * Evaluates and updates dynamic attribute modifiers based on current armor and config
     */
    public static void updateArmorModifiers(HamsterEntity hamster, ItemStack armorStack) {
        AttributeInstance speedAttribute = hamster.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance knockbackAttribute = hamster.getAttribute(Attributes.KNOCKBACK_RESISTANCE);

        boolean perksEnabled = Configs.AHP_MAIN.enableArmorPerks.get();
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
            boolean hasSpeed = speedAttribute.hasModifier(ARMOR_SPEED_BOOST_ID);

            if (shouldHaveSpeed && !hasSpeed) {
                double boost = Configs.AHP_MAIN.goldArmorSpeedBoost.get();
                speedAttribute.addTransientModifier(new AttributeModifier(
                        ARMOR_SPEED_BOOST_ID, boost, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            } else if (!shouldHaveSpeed && hasSpeed) {
                speedAttribute.removeModifier(ARMOR_SPEED_BOOST_ID);
            }
        }

        if (knockbackAttribute != null) {
            boolean hasKnockback = knockbackAttribute.hasModifier(ARMOR_KNOCKBACK_RESISTANCE_ID);

            if (shouldHaveKnockback && !hasKnockback) {
                double resist = Configs.AHP_MAIN.netheriteArmorKnockbackResist.get();
                knockbackAttribute.addTransientModifier(new AttributeModifier(
                        ARMOR_KNOCKBACK_RESISTANCE_ID, resist, AttributeModifier.Operation.ADD_VALUE
                ));
            } else if (!shouldHaveKnockback && hasKnockback) {
                knockbackAttribute.removeModifier(ARMOR_KNOCKBACK_RESISTANCE_ID);
            }
        }
    }

    /**
     * Calculates total throw damage based on configuration and current armor
     */
    public static float calculateThrowDamage(HamsterEntity hamster, ItemStack armorStack) {
        float damageAmount = Configs.AHP_MAIN.hamsterThrowDamage.get().floatValue();

        if (Configs.AHP_MAIN.enableArmorPerks.get() && !armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
            if (armorItem.getMaterial() == HamsterArmorItem.HamsterArmorMaterial.NETHERITE) {
                damageAmount += Configs.AHP_MAIN.netheriteArmorThrowDamageBonus.get().floatValue();
            }
        }

        return damageAmount;
    }

    /**
     * Finalizes the impact of a thrown hamster, calculating ricochet or bounce physics
     * based on the impacted surface and spawns the entity.
     */
    public static void finalizeImpact(HamsterEntity hamster, Vec3 incomingVel, Vec3 spawnPos, @Nullable Direction hitFace, @Nullable BlockState hitState) {
        // --- 1. Determine Surface Modifiers ---
        // Modify bounce intensity based on block type
        double bounceMultiplier = 0.3;
        double verticalBounce = 0.3;

        if (hitState != null) {
            if (hitState.is(Blocks.HONEY_BLOCK)) {
                bounceMultiplier = 0.0;
                verticalBounce = 0.0;
            } else if (hitState.is(Blocks.SLIME_BLOCK)) {
                bounceMultiplier = 0.6;
                verticalBounce = 0.6;
            }
        }

        // --- 2. Calculate Velocity ---
        Vec3 bounceVel;
        if (hitFace == Direction.UP) {
            // Bounce off top of block -> continue forward & bounce up
            bounceVel = new Vec3(incomingVel.x * bounceMultiplier, verticalBounce, incomingVel.z * bounceMultiplier);
        } else if (hitFace == Direction.DOWN) {
            // Bounce off bottom of block -> continue forward & deflect down
            bounceVel = new Vec3(incomingVel.x * bounceMultiplier, -verticalBounce, incomingVel.z * bounceMultiplier);
        } else if (hitFace == Direction.NORTH || hitFace == Direction.SOUTH) {
            // Ricochet off Z-axis wall -> reverse Z, maintain X
            bounceVel = new Vec3(incomingVel.x * bounceMultiplier, 0.0, incomingVel.z * -bounceMultiplier);
        } else if (hitFace == Direction.EAST || hitFace == Direction.WEST) {
            // Ricochet off X-axis wall -> reverse X, maintain Z
            bounceVel = new Vec3(incomingVel.x * -bounceMultiplier, 0.0, incomingVel.z * bounceMultiplier);
        } else {
            // Ricochet off entity (hitFace is null) -> reverse horizontal & drop vertical
            bounceVel = new Vec3(incomingVel.x * -bounceMultiplier, 0.0, incomingVel.z * -bounceMultiplier);
        }

        // --- 3. Calculate Yaw ---
        float yaw;
        // If bouncing off wall, face the direction of bounce
        if (bounceVel.horizontalDistanceSqr() > 0.001) {
            yaw = (float) (Mth.atan2(-bounceVel.x, bounceVel.z) * Mth.RAD_TO_DEG);
        } else {
            yaw = (float) (Mth.atan2(-incomingVel.x, incomingVel.z) * Mth.RAD_TO_DEG);
        }

        // --- 4. Apply State and Spawn ---
        hamster.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0f);

        hamster.setYBodyRot(yaw);
        hamster.setYHeadRot(yaw);
        hamster.yRotO = yaw;
        hamster.yBodyRotO = yaw;
        hamster.yHeadRotO = yaw;

        hamster.setDeltaMovement(bounceVel);
        hamster.hasImpulse = true;

        hamster.setKnockedOut(true);
        hamster.setInSittingPose(true);

        hamster.level().addFreshEntity(hamster);
        hamster.triggerAnimOnServer("mainController", "crash");

        // --- 5. Spawn Impact Particles ---
        if (hitState != null && !hitState.isAir() && !hamster.level().isClientSide()) {
            ParticleEffectsUtil.spawnParticles(
                    hamster.level(),
                    new Vec3(hamster.getX(), hamster.getY() + hamster.getBbHeight() / 2.0, hamster.getZ()),
                    new BlockParticleOption(ParticleTypes.BLOCK, hitState),
                    30,
                    new Vec3(0.3, 0.3, 0.3),
                    0.0
            );
        }
    }
}