package net.dawson.adorablehamsterpets.mixin.client;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    /**
     * Injects into the end of the player's tick method on the client.
     * This ensures that the animation state for any shoulder-mounted hamsters is updated
     * for ALL player entities on the client, including the local player, remote players,
     * and playback actors from mods like Flashback.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void adorablehamsterpets$onTick(CallbackInfo ci) {
        AbstractClientPlayerEntity thisPlayer = (AbstractClientPlayerEntity) (Object) this;
        // The tick method can be called on the integrated server thread, so we must check.
        if (thisPlayer.getWorld().isClient) {
            ClientShoulderHamsterData clientData = ((PlayerEntityAccessor) thisPlayer).adorablehamsterpets$getClientShoulderData();
            if (clientData != null) {
                clientData.clientTick(thisPlayer);
            }
        }
    }
}