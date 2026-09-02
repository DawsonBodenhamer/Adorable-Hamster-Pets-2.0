package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(Spider.class)
public abstract class SpiderEntityMixin extends Monster {

    protected SpiderEntityMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void adorablehamsterpets$addFleeHamsterGoal(CallbackInfo ci) {
        // This predicate will read the live config value.
        Predicate<LivingEntity> fleeCondition = (livingEntity) -> AdorableHamsterPets.MAIN_CONFIG.enableSpiderFlee;

        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(
                this,
                HamsterEntity.class,
                6.0F,
                1.0D,
                1.2D,
                fleeCondition
        ));
    }
}