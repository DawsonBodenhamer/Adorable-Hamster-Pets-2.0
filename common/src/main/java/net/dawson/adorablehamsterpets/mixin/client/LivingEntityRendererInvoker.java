package net.dawson.adorablehamsterpets.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Target LivingEntityRenderer directly
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {

    // Use @Invoker to call the protected addFeature method
    @Invoker("addLayer")
    boolean callAddFeature(RenderLayer<?, ?> feature);
}