package net.dawson.adorablehamsterpets.client.shoulder;

import net.minecraft.world.entity.player.PlayerModelType;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderState;
import net.dawson.adorablehamsterpets.entity.client.feature.HamsterShoulderFeatureRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Builds the per-frame shoulder-hamster snapshots from the player entity.
 * Runs at the end of AvatarRenderer.extractRenderState via mixin.
 */
public final class ShoulderHamsterExtractor {
    private ShoulderHamsterExtractor() {}

    public static void extract(Avatar avatar, AvatarRenderState state, float partialTick) {
        if (!(state instanceof ShoulderHamsterRenderData data)) return;
        data.adorablehamsterpets$getShoulderHamsters().clear();
        if (!(avatar instanceof AbstractClientPlayer player) || !(player instanceof PlayerEntityAccessor accessor)) return;
        if (!hasHamsterStateSafe(accessor)) return;

        ClientShoulderHamsterData clientData = accessor.adorablehamsterpets$getClientHamsterState();
        if (clientData == null) return;

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        data.adorablehamsterpets$setWearingChestplate(!chestStack.isEmpty() && !chestStack.is(Items.ELYTRA));
        data.adorablehamsterpets$setSlim(player.getSkin().model() == PlayerModelType.SLIM);
        data.adorablehamsterpets$setBodyYaw(180.0F - player.getVisualRotationYInDegrees());

        for (ShoulderLocation location : ShoulderLocation.values()) {
            CompoundTag shoulderNbt = accessor.getShoulderHamster(location);
            if (shoulderNbt.isEmpty()) continue;
            HamsterState.fromNbt(shoulderNbt).ifPresent(hamsterState -> {
                HamsterEntity dummy = clientData.getOrCreateDummy(location, player.level());
                if (dummy == null) return;
                HamsterShoulderFeatureRenderer.updateDummyState(dummy, hamsterState, clientData, location, player);
                dummy.dynamicScaleY = clientData.getRenderScaleY(location, partialTick);
                if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy) instanceof HamsterRenderer renderer)) return;
                HamsterRenderState snapshot = renderer.createRenderState(dummy, partialTick);
                data.adorablehamsterpets$getShoulderHamsters().put(location,
                        new ShoulderHamsterRenderData.Entry(snapshot, clientData.getRenderOffsetY(location, partialTick)));
            });
        }
    }

    /** DataTracker access can throw for malformed/fake players (shader shadows); treat as "no hamster". */
    private static boolean hasHamsterStateSafe(PlayerEntityAccessor accessor) {
        try {
            return accessor.hasAnyShoulderHamster();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
