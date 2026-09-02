package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$rejectContractTarget(
            @Nullable LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
		if (target != null && AcornRingUtil.protects(self, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickNewAi", at = @At("HEAD"))
    private void adorablehamsterpets$clearContractTargetsBeforeAi(CallbackInfo ci) {
        adorablehamsterpets$clearContractTargets((Mob) (Object) this);
    }

    @Inject(method = "tickNewAi", at = @At("RETURN"))
    private void adorablehamsterpets$clearContractTargetsAfterAi(CallbackInfo ci) {
        adorablehamsterpets$clearContractTargets((Mob) (Object) this);
    }

    private static void adorablehamsterpets$clearContractTargets(Mob mob) {
		if (mob.level().isClientSide() || !AcornRingUtil.isEligiblePet(mob)) {
            return;
        }

        boolean removedPursuit = false;
        LivingEntity target = mob.getTarget();
		if (target != null && AcornRingUtil.protects(mob, target)) {
            mob.setTarget(null);
            removedPursuit = true;
        }

        Brain<?> brain = mob.getBrain();
        LivingEntity attackTarget = adorablehamsterpets$getMemory(brain, MemoryModuleType.ATTACK_TARGET);
		if (attackTarget != null && AcornRingUtil.protects(mob, attackTarget)) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            removedPursuit = true;
        }

        UUID angryAt = adorablehamsterpets$getMemory(brain, MemoryModuleType.ANGRY_AT);
        LivingEntity angryTarget = adorablehamsterpets$resolveLiving(mob, angryAt);
		if (angryTarget != null && AcornRingUtil.protects(mob, angryTarget)) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            removedPursuit = true;
        }

        WalkTarget walkTarget = adorablehamsterpets$getMemory(brain, MemoryModuleType.WALK_TARGET);
        if (walkTarget != null
                && walkTarget.getTarget() instanceof EntityTracker entityLookTarget
                && entityLookTarget.getEntity() instanceof LivingEntity walkEntity
				&& AcornRingUtil.protects(mob, walkEntity)) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            removedPursuit = true;
        }

        LivingEntity hurtByEntity =
                adorablehamsterpets$getMemory(brain, MemoryModuleType.HURT_BY_ENTITY);
		if (hurtByEntity != null && AcornRingUtil.protects(mob, hurtByEntity)) {
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            removedPursuit = true;
        }

        DamageSource hurtBy = adorablehamsterpets$getMemory(brain, MemoryModuleType.HURT_BY);
        if (hurtBy != null
                && hurtBy.getEntity() instanceof LivingEntity hurtByAttacker
				&& AcornRingUtil.protects(mob, hurtByAttacker)) {
            brain.eraseMemory(MemoryModuleType.HURT_BY);
            removedPursuit = true;
        }

        LivingEntity attacker = mob.getLastHurtByMob();
		if (attacker != null && AcornRingUtil.protects(mob, attacker)) {
            mob.setLastHurtByMob(null);
            removedPursuit = true;
        }

        if (removedPursuit) {
            mob.getNavigation().stop();
        }
    }

    @Nullable
    private static <T> T adorablehamsterpets$getMemory(
            Brain<?> brain, MemoryModuleType<T> memoryType) {
        Optional<T> memory = brain.getMemoryInternal(memoryType);
        return memory == null ? null : memory.orElse(null);
    }

    @Nullable
    private static LivingEntity adorablehamsterpets$resolveLiving(
            Mob mob, @Nullable UUID uuid) {
        if (uuid == null || !(mob.level() instanceof ServerLevel serverWorld)) {
            return null;
        }
        ServerPlayer player = serverWorld.getServer().getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player;
        }
        Entity entity = serverWorld.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }
}
