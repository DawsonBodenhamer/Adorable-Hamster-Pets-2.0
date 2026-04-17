package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("getHurtSound")
    SoundEvent adorablehamsterpets$callGetHurtSound(DamageSource source);

    @Invoker("getDeathSound")
    SoundEvent adorablehamsterpets$callGetDeathSound();
}