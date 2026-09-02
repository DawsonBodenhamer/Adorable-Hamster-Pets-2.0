package net.dawson.adorablehamsterpets.entity.client;

import net.minecraft.world.phys.Vec3;
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

    /* GeckoLib 5 mixes GeoRenderState into the vanilla render state and its own
       addGeckolibData/hasGeckolibData write to a mixin-private map. Override them so
       every read and write goes through this class's single map. */
    @Override
    public <D> void addGeckolibData(DataTicket<D> dataTicket, D data) {
        this.geckolibData.put(dataTicket, data);
    }

    @Override
    public boolean hasGeckolibData(DataTicket<?> dataTicket) {
        return this.geckolibData.containsKey(dataTicket);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> D getGeckolibData(DataTicket<D> dataTicket) {
        return (D) this.geckolibData.get(dataTicket);
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
    /** Mouth item resolved during extraction (26.2 item pipeline); null when empty. */
    public net.minecraft.client.renderer.item.ItemStackRenderState mouthItemRenderState;
    /** Riders extracted during extraction, drawn bone-locked to the seat bone in the post pass. */
    public final java.util.List<Rider> riders = new java.util.ArrayList<>();
    /** Snapshot of one rider: its own render state, the seat offset and the yaw to face. */
    public record Rider(net.minecraft.client.renderer.entity.state.EntityRenderState state, net.minecraft.world.phys.Vec3 seatOffset, float yaw) {}

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

    /** World-space bone positions captured by listeners during the render pass (left_foot, nose, seat). */
    public final Map<String, Vec3> bonePositions = new HashMap<>();
}
