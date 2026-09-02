package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.client.ClientInputUtil;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class HamsterBeddingItem extends Item {
    public HamsterBeddingItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (world.isClientSide()) {
            // Perform a raycast to see what the player is looking at
            BlockHitResult hitResult = getPlayerPOVHitResult(world, user, ClipContext.Fluid.NONE);
            Vec3 particlePos;

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                // Player is looking at a block, spawn particles in the adjacent air block
                BlockPos adjacentPos = hitResult.getBlockPos().relative(hitResult.getDirection());
                particlePos = Vec3.atCenterOf(adjacentPos);
            } else {
                // Player is looking at the air, spawn particles in front of them
                particlePos = user.getEyePosition().add(user.getViewVector(1.0f).scale(1.5));
            }

            // Spawn a puff of leaf particles
            for (int i = 0; i < 100; i++) {
                double offsetX = world.getRandom().nextGaussian() * 1.2;
                double offsetY = world.getRandom().nextGaussian() * 1.2;
                double offsetZ = world.getRandom().nextGaussian() * 1.2;
                world.addParticle(ModParticles.getForVariant(WoodVariant.OAK), // Use OAK as default
                        particlePos.x + offsetX, particlePos.y + offsetY, particlePos.z + offsetZ,
                        0, HamsterBeddingParticle.BEDDING_ITEM_FLAG, 0);
            }

            // Play leaf sound
            SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.getRandom());
            if (rustleSound != null) {
                world.playSound(user, user.blockPosition(), rustleSound, SoundSource.PLAYERS, 0.2f, 1.5f);
            }
        }

        // Trigger advancement on server
        if (!world.isClientSide() && user instanceof ServerPlayer serverPlayer) {
            ModCriteria.USED_HAMSTER_BEDDING.get().trigger(serverPlayer);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        if (Configs.AHP_UI.enableItemTooltips) {
            if (ClientInputUtil.hasShiftDown()) {
                // --- Expanded Tooltip (Shift) ---
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bedding.hint1").withStyle(ChatFormatting.GOLD));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bedding.hint2").withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bedding.hint3").withStyle(ChatFormatting.GRAY));
            } else {
                // --- Default Tooltip ---
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bedding.hint1").withStyle(ChatFormatting.GOLD));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.shift_for_info").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (!Platform.isModLoaded("emi")) {
            tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }
        super.appendHoverText(stack, context, display, tooltip, type);
    }
}