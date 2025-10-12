package net.dawson.adorablehamsterpets.mixin.client;
/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

// Or your invoker package

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