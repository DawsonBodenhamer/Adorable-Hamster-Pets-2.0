package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.util.AcornRingContractUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$rejectContractTarget(
            @Nullable LivingEntity target, CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (target != null && AcornRingContractUtil.protects(self, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickNewAi", at = @At("HEAD"))
    private void adorablehamsterpets$clearContractTargetsBeforeAi(CallbackInfo ci) {
        adorablehamsterpets$clearContractTargets((MobEntity) (Object) this);
    }

    @Inject(method = "tickNewAi", at = @At("RETURN"))
    private void adorablehamsterpets$clearContractTargetsAfterAi(CallbackInfo ci) {
        adorablehamsterpets$clearContractTargets((MobEntity) (Object) this);
    }

    private static void adorablehamsterpets$clearContractTargets(MobEntity mob) {
        if (mob.getWorld().isClient() || !AcornRingContractUtil.isEligiblePet(mob)) {
            return;
        }

        boolean removedPursuit = false;
        LivingEntity target = mob.getTarget();
        if (target != null && AcornRingContractUtil.protects(mob, target)) {
            mob.setTarget(null);
            removedPursuit = true;
        }

        Brain<?> brain = mob.getBrain();
        LivingEntity attackTarget = adorablehamsterpets$getMemory(brain, MemoryModuleType.ATTACK_TARGET);
        if (attackTarget != null && AcornRingContractUtil.protects(mob, attackTarget)) {
            brain.forget(MemoryModuleType.ATTACK_TARGET);
            removedPursuit = true;
        }

        UUID angryAt = adorablehamsterpets$getMemory(brain, MemoryModuleType.ANGRY_AT);
        LivingEntity angryTarget = adorablehamsterpets$resolveLiving(mob, angryAt);
        if (angryTarget != null && AcornRingContractUtil.protects(mob, angryTarget)) {
            brain.forget(MemoryModuleType.ANGRY_AT);
            removedPursuit = true;
        }

        WalkTarget walkTarget = adorablehamsterpets$getMemory(brain, MemoryModuleType.WALK_TARGET);
        if (walkTarget != null
                && walkTarget.getLookTarget() instanceof EntityLookTarget entityLookTarget
                && entityLookTarget.getEntity() instanceof LivingEntity walkEntity
                && AcornRingContractUtil.protects(mob, walkEntity)) {
            brain.forget(MemoryModuleType.WALK_TARGET);
            removedPursuit = true;
        }

        LivingEntity hurtByEntity =
                adorablehamsterpets$getMemory(brain, MemoryModuleType.HURT_BY_ENTITY);
        if (hurtByEntity != null && AcornRingContractUtil.protects(mob, hurtByEntity)) {
            brain.forget(MemoryModuleType.HURT_BY_ENTITY);
            removedPursuit = true;
        }

        DamageSource hurtBy = adorablehamsterpets$getMemory(brain, MemoryModuleType.HURT_BY);
        if (hurtBy != null
                && hurtBy.getAttacker() instanceof LivingEntity hurtByAttacker
                && AcornRingContractUtil.protects(mob, hurtByAttacker)) {
            brain.forget(MemoryModuleType.HURT_BY);
            removedPursuit = true;
        }

        LivingEntity attacker = mob.getAttacker();
        if (attacker != null && AcornRingContractUtil.protects(mob, attacker)) {
            mob.setAttacker(null);
            removedPursuit = true;
        }

        if (removedPursuit) {
            mob.getNavigation().stop();
        }
    }

    @Nullable
    private static <T> T adorablehamsterpets$getMemory(
            Brain<?> brain, MemoryModuleType<T> memoryType) {
        Optional<T> memory = brain.getOptionalMemory(memoryType);
        return memory == null ? null : memory.orElse(null);
    }

    @Nullable
    private static LivingEntity adorablehamsterpets$resolveLiving(
            MobEntity mob, @Nullable UUID uuid) {
        if (uuid == null || !(mob.getWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }
        ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player;
        }
        Entity entity = serverWorld.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }
}
