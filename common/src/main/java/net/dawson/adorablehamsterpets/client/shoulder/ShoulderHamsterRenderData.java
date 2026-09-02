package net.dawson.adorablehamsterpets.client.shoulder;

import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderState;

import java.util.Map;

/**
 * 26.2 port: shoulder hamsters are drawn from the player's render state, the
 * way vanilla draws shoulder parrots. This is bolted onto AvatarRenderState by
 * mixin; the avatar renderer fills it during extraction and the shoulder layer
 * reads it while submitting.
 */
public interface ShoulderHamsterRenderData {
    Map<ShoulderLocation, Entry> adorablehamsterpets$getShoulderHamsters();
    void adorablehamsterpets$setSlim(boolean slim);
    boolean adorablehamsterpets$isSlim();
    void adorablehamsterpets$setWearingChestplate(boolean wearing);
    boolean adorablehamsterpets$isWearingChestplate();
    void adorablehamsterpets$setBodyYaw(float yaw);
    float adorablehamsterpets$getBodyYaw();

    /** One hamster on one shoulder: its snapshot plus the frame's vertical bob. */
    record Entry(HamsterRenderState renderState, float renderOffsetY) {}
}
