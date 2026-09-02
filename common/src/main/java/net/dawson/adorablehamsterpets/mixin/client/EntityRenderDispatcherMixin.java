package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Riders of a hamster are drawn by HamsterRenderer, locked to the animated seat
 * bone, so vanilla must not draw them a second time at the static riding position.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void adorablehamsterpets$skipHamsterRiders(E entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (entity.getVehicle() instanceof HamsterEntity) {
            Minecraft client = Minecraft.getInstance();
            if (entity == client.player && client.options.getCameraType().isFirstPerson()) return;
            cir.setReturnValue(false);
        }
    }
}
