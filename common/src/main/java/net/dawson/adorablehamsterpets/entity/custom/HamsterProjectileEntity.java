package net.dawson.adorablehamsterpets.entity.custom;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterNbtUtil;
import net.dawson.adorablehamsterpets.util.HamsterPhysicsUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.dawson.adorablehamsterpets.util.TreeHeistUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * A standard thrown entity wrapper for hamsters.
 * Acts as a compatibility layer for any mods that interact with projectiles.
 */
public class HamsterProjectileEntity extends ThrownEntity {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final TrackedData<NbtCompound> HAMSTER_DATA = DataTracker.registerData(HamsterProjectileEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean hasPlayedIncomingSound = false;

    // Used exclusively by client renderer
    @Environment(EnvType.CLIENT)
    public HamsterEntity clientDummyHamster;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterProjectileEntity(EntityType<? extends ThrownEntity> entityType, World world) {
        super(entityType, world);
    }

    public HamsterProjectileEntity(World world, LivingEntity owner) {
        super(ModEntities.HAMSTER_PROJECTILE.get(), owner, world);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void tick() {
        // --- Custom Collision Check ---
        // Used for non-solid heistable blocks (e.g. Dynamic Trees mod)
        if (!this.getWorld().isClient()) {
            BlockHitResult bhr = TreeHeistUtil.checkNonSolidCollision(this);

            if (bhr != null) {
                this.onCollision(bhr);
                return; // Stop tick, entity is discarded in onCollision
            }
        }

        super.tick();

        // Simulate trajectory to play warning sound
        if (!this.getWorld().isClient() && !this.hasPlayedIncomingSound && this.age > 1) {
            HamsterPhysicsUtil.simulateTrajectoryAndCheckSound(this);
        }

        // --- Particle Trail Logic ---
        if (!this.getWorld().isClient()) {
            boolean isBuffed = false;
            NbtCompound nbt = this.getHamsterData();
            if (!nbt.isEmpty() && nbt.contains("greenBeanBuffData")) {
                NbtCompound buffData = nbt.getCompound("greenBeanBuffData");
                isBuffed = buffData.getLong("greenBeanBuffDuration") > this.getWorld().getTime();
            }

            int particleDelay = isBuffed ? 3 : 5;

            if (this.age > particleDelay) {
                Vec3d currentVelocity = this.getVelocity();
                double offsetMultiplier = 1.5;
                double spawnX = this.prevX - (currentVelocity.x * offsetMultiplier);
                double spawnY = this.prevY + (this.getHeight() / 2.0) - (currentVelocity.y * offsetMultiplier);
                double spawnZ = this.prevZ - (currentVelocity.z * offsetMultiplier);

                ParticleEffectsUtil.spawnParticles(
                        this.getWorld(),
                        new Vec3d(spawnX, spawnY, spawnZ),
                        ParticleTypes.GUST,
                        1,
                        new Vec3d(0.1, 0.1, 0.1),
                        0.0
                );
            }
        }

        // Failsafe
        if (!this.getWorld().isClient() && this.getHamsterData().isEmpty()) {
            this.discard();
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public void setHamsterData(NbtCompound nbt) {
        this.dataTracker.set(HAMSTER_DATA, nbt);
    }

    public NbtCompound getHamsterData() {
        return this.dataTracker.get(HAMSTER_DATA);
    }

    public void setHasPlayedIncomingSound(boolean val) {
        this.hasPlayedIncomingSound = val;
    }

    public boolean hasPlayedIncomingSound() {
        return this.hasPlayedIncomingSound;
    }

    public boolean isHitTargetValid(Entity entity) {
        return this.canHit(entity);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put("HamsterData", this.getHamsterData());
        nbt.putBoolean("HasPlayedIncomingSound", this.hasPlayedIncomingSound);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("HamsterData", 10)) { // 10 = Compound
            this.setHamsterData(nbt.getCompound("HamsterData"));
        }
        this.hasPlayedIncomingSound = nbt.getBoolean("HasPlayedIncomingSound");
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(HAMSTER_DATA, new NbtCompound());
    }

    @Override
    protected double getGravity() {
        return Math.abs(HamsterPhysicsUtil.THROWN_GRAVITY);
    }

    @Override
    protected boolean canHit(Entity entity) {
        if (entity == this) {
            return false;
        }

        if (entity instanceof ArmorStandEntity) {
            return !entity.isSpectator();
        }

        Entity owner = this.getOwner();
        if (owner != null) {
            if (!Configs.AHP.yeetFriendlyFire) {
                return false;
            }
        }

        return super.canHit(entity);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (this.getWorld().isClient()) return;

        Entity hitEntity = entityHitResult.getEntity();
        NbtCompound hamsterNbt = this.getHamsterData();

        // Safety check
        PlayerEntity ownerPlayer = this.getOwner() instanceof PlayerEntity p ? p : null;

        if (!hamsterNbt.isEmpty()) {
            HamsterEntity hamster = HamsterNbtUtil.createFromNbt((ServerWorld) this.getWorld(), ownerPlayer, hamsterNbt);
            if (hamster != null) {
                boolean playEffects = false;
                SoundEvent impactSound = SoundEvents.ENTITY_GENERIC_SMALL_FALL; // Fallback

                // Create DamageSource where thrown hamster is attacker
                DamageSource damageSource = hamster.getDamageSources().mobAttack(hamster);

                if (hitEntity instanceof ArmorStandEntity) {
                    playEffects = true;
                    impactSound = ModSounds.getDynamicEntitySound(hitEntity, false, damageSource);
                } else if (hitEntity instanceof LivingEntity livingHit && this.getOwner() != null) {

                    float damageAmount = HamsterPhysicsUtil.calculateThrowDamage(hamster, hamster.getArmorStack());
                    boolean damaged = livingHit.damage(damageSource, damageAmount); // Hamster is damage source

                    if (damaged) {
                        boolean isDeath = livingHit.isDead() || livingHit.getHealth() <= 0.0f;
                        impactSound = ModSounds.getDynamicEntitySound(hitEntity, isDeath, damageSource);

                        // Apply damage
                        livingHit.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20, 0, false, false, false));

                        // Calculate knockback direction based on velocity
                        Vec3d currentVel = this.getVelocity();
                        double knockbackStrength = 0.5;

                        // Apply knockback
                        livingHit.takeKnockback(knockbackStrength, -currentVel.x, -currentVel.z);
                        playEffects = true;
                    }
                } else {
                    playEffects = true;
                    impactSound = ModSounds.getDynamicEntitySound(hitEntity, false, damageSource);
                }

                if (playEffects) {
                    // Temporarily give unspawned hamster the projectile's position so SFX works
                    hamster.setPosition(this.getPos());
                    // Feedback
                    HamsterPhysicsUtil.broadcastImpactSound(hamster, impactSound, 1.0f);
                    HamsterPhysicsUtil.broadcastImpactSound(hamster, ModSounds.HAMSTER_IMPACT.get(), 1.0f);
                    ParticleEffectsUtil.spawnParticles(this.getWorld(), new Vec3d(this.getX(), this.getY() + this.getHeight() / 2.0, this.getZ()), ParticleTypes.POOF, 50, new Vec3d(0.4, 0.4, 0.4), 0.1);
                }

                Vec3d impactPos = this.getPos();

                // Pass null for face & state since impacting an entity
                HamsterPhysicsUtil.finalizeImpact(hamster, this.getVelocity(), impactPos, null, null);
            }
        }
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld().isClient()) return;

        NbtCompound hamsterNbt = this.getHamsterData();
        PlayerEntity ownerPlayer = this.getOwner() instanceof PlayerEntity p ? p : null;

        if (!hamsterNbt.isEmpty()) {
            HamsterEntity hamster = HamsterNbtUtil.createFromNbt((ServerWorld) this.getWorld(), ownerPlayer, hamsterNbt);
            if (hamster != null) {
                BlockPos hitPos = blockHitResult.getBlockPos();
                BlockState hitState = this.getWorld().getBlockState(hitPos);

                if (ConfigDataCache.isHeistableLeaf(hitState) || ConfigDataCache.isHeistableLog(hitState)) {
                    // 1. Scan first to identify the tree anchor
                    TreeHeistUtil.TreeScanResult scanResult = TreeHeistUtil.scanForTree(this.getWorld(), hitPos);

                    // 2. Check occupancy
                    if (HamsterTreeSearcherEntity.isTreeBlocked(this.getWorld(), scanResult.treeId())) {
                        // Tree is busy
                        if (ownerPlayer != null) {
                            ownerPlayer.sendMessage(Text.translatable("message.adorablehamsterpets.tree_heist_occupied").formatted(Formatting.RED), true);
                        }
                        fallbackBlockHit(blockHitResult, hamster);
                    } else {
                        // Tree is free. Start Heist
                        hamster.triggerLeafPopEffects(hitPos, true);
                        HamsterTreeSearcherEntity searcher = ModEntities.HAMSTER_TREE_SEARCHER.get().create(this.getWorld());
                        if (searcher != null) {
                            NbtCompound fullNbt = new NbtCompound();
                            hamster.writeNbt(fullNbt); // Use writeNbt to capture full entity state (Owner, Attributes, etc.)
                            // Pass already-calculated scan result
                            searcher.initializeSearch(hitPos, scanResult, fullNbt);
                            this.getWorld().spawnEntity(searcher);
                        }
                    }
                } else {
                    fallbackBlockHit(blockHitResult, hamster);
                }
            }
        }
        this.discard();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private void fallbackBlockHit(BlockHitResult blockHitResult, HamsterEntity hamster) {
        // --- Standard Block Collision Handling ---
        Vec3d impactPos = this.getPos();
        BlockState hitState = this.getWorld().getBlockState(blockHitResult.getBlockPos());

        // Temporarily position so SFX works
        hamster.setPosition(impactPos);

        // Feedback
        SoundEvent impactSound = ModSounds.getDynamicBlockSound(hitState);
        HamsterPhysicsUtil.broadcastImpactSound(hamster, impactSound, 1.2f); // Dynamic block sound based on surface
        HamsterPhysicsUtil.broadcastImpactSound(hamster, SoundEvents.ENTITY_GENERIC_SMALL_FALL, 1.2f); // Armor sound if applicable

        HamsterPhysicsUtil.finalizeImpact(hamster, this.getVelocity(), impactPos, blockHitResult.getSide(), hitState);
    }
}