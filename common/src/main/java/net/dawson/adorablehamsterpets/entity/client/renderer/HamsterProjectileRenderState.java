package net.dawson.adorablehamsterpets.entity.client.renderer;

import net.dawson.adorablehamsterpets.entity.client.HamsterRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jetbrains.annotations.Nullable;

/** Render state for a thrown hamster: just the snapshot of the hamster it is drawn as. */
public class HamsterProjectileRenderState extends EntityRenderState {
    /** Null until the client-side dummy hamster exists. */
    @Nullable
    public HamsterRenderState hamster;
}
