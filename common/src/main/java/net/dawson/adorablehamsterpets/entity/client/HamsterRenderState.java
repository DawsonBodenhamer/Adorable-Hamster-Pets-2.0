package net.dawson.adorablehamsterpets.entity.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-frame snapshot of everything the hamster renderer needs.
 *
 * <p>26.2 port: Minecraft now separates "read the entity" from "draw it". The
 * renderer never sees the {@code HamsterEntity} during drawing, so anything the
 * old {@code render}/{@code renderFinal} used to read straight off the entity is
 * captured here first, in
 * {@link HamsterRenderer#extractRenderState}.
 *
 * <p>Values that only feed one calculation are stored already-computed (the
 * redstone tremor offsets, for instance) rather than storing their inputs and
 * redoing the math at draw time.
 */
public class HamsterRenderState extends LivingEntityRenderState implements GeoRenderState {

    /* ── GeckoLib data map ───────────────────────────────────────────────── */

    private final Map<DataTicket<?>, Object> geckolibData = new HashMap<>();

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckolibData;
    }

    /* ── Hamster-specific snapshot ───────────────────────────────────────── */

    /** Horizontal shake from Redstone Fever, already resolved for this frame. */
    public double tremorOffsetX;
    public double tremorOffsetZ;
    /** Roll in radians from the same tremor. */
    public float tremorRoll;

    /** Smoothed offset that keeps the model sitting on uneven ground. */
    public float groundYOffset;

    /** Rendering into an inventory/GUI rather than the world: suppresses tremor and name tags. */
    public boolean renderingInGui;

    /** Held in the mouth bone, or empty. */
    public ItemStack mouthItem = ItemStack.EMPTY;

    /** True when something is riding the seat bone. */
    public boolean hasPassengers;

    /**
     * Resolved fur texture. The lookup needs the entity's genetics and mood, so it
     * happens during extraction; {@code getTextureLocation} only reads it back.
     */
    public Identifier textureLocation;

    /**
     * The entity itself.
     *
     * <p>A deliberate exception to the render-state contract. Three features --
     * keyframe particles, keyframe sounds and the mouth-item/passenger passes --
     * need live bone transforms <em>and</em> the entity's level, position and
     * inventory at the same moment, so their inputs cannot be snapshotted ahead of
     * time. Everything else in this class is a plain snapshot; only these paths
     * touch this field, and only on the render thread.
     */
    public HamsterEntity entity;
}
