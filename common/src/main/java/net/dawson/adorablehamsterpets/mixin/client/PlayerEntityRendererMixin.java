package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.client.feature.HamsterShoulderFeatureRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AvatarRenderer.class, remap = false)
public abstract class PlayerEntityRendererMixin {

    // Inject into the constructor
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        AdorableHamsterPets.LOGGER.trace("[AHP Mixin] PlayerEntityRendererMixin constructor injection is RUNNING.");

        // Cast 'this' (PlayerEntityRenderer) to LivingEntityRenderer first
        LivingEntityRenderer<?, ?, ?> livingRenderer = (LivingEntityRenderer<?, ?, ?>) (Object) this;
        // Then cast the LivingEntityRenderer instance to my separate Invoker interface
        LivingEntityRendererInvoker invoker = (LivingEntityRendererInvoker) livingRenderer;
        // Cast 'this' to PlayerEntityRenderer to pass to the FeatureRenderer constructor
        AvatarRenderer thisRenderer = (AvatarRenderer)(Object)this;

        AdorableHamsterPets.LOGGER.trace("[PlayerRendererMixin] Adding HamsterShoulderFeatureRenderer via Invoker...");
        // Call the protected method using the invoker interface
        boolean added = invoker.callAddFeature(new HamsterShoulderFeatureRenderer(thisRenderer));

        AdorableHamsterPets.LOGGER.trace("[AHP Mixin] Attempted to add HamsterShoulderFeatureRenderer. Success: {}", added);
        if (!added) {
            AdorableHamsterPets.LOGGER.trace("[AHP Mixin] FAILED to add HamsterShoulderFeatureRenderer!");
        }
    }
}