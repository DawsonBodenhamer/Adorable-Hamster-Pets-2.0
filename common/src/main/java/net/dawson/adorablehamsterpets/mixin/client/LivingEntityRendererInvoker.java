package net.dawson.adorablehamsterpets.mixin.client;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Target LivingEntityRenderer directly
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {

    // Use @Invoker to call the protected addFeature method
    @Invoker("addFeature")
    boolean callAddFeature(FeatureRenderer<?, ?> feature);
}