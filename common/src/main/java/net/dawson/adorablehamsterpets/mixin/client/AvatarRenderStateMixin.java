package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.client.shoulder.ShoulderHamsterRenderData;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.EnumMap;
import java.util.Map;

/** Carries shoulder-hamster snapshots on the player's render state (see ShoulderHamsterRenderData). */
@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements ShoulderHamsterRenderData {
    @Unique private final Map<ShoulderLocation, Entry> adorablehamsterpets$shoulderHamsters = new EnumMap<>(ShoulderLocation.class);
    @Unique private boolean adorablehamsterpets$slim;
    @Unique private boolean adorablehamsterpets$chestplate;
    @Unique private float adorablehamsterpets$bodyYaw;

    @Override public Map<ShoulderLocation, Entry> adorablehamsterpets$getShoulderHamsters() { return this.adorablehamsterpets$shoulderHamsters; }
    @Override public void adorablehamsterpets$setSlim(boolean slim) { this.adorablehamsterpets$slim = slim; }
    @Override public boolean adorablehamsterpets$isSlim() { return this.adorablehamsterpets$slim; }
    @Override public void adorablehamsterpets$setWearingChestplate(boolean wearing) { this.adorablehamsterpets$chestplate = wearing; }
    @Override public boolean adorablehamsterpets$isWearingChestplate() { return this.adorablehamsterpets$chestplate; }
    @Override public void adorablehamsterpets$setBodyYaw(float yaw) { this.adorablehamsterpets$bodyYaw = yaw; }
    @Override public float adorablehamsterpets$getBodyYaw() { return this.adorablehamsterpets$bodyYaw; }
}
