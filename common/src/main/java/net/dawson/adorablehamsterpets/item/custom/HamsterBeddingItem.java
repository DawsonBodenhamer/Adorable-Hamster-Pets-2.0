package net.dawson.adorablehamsterpets.item.custom;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HamsterBeddingItem extends Item {
    public HamsterBeddingItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            // Perform a raycast to see what the player is looking at
            BlockHitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.NONE);
            Vec3d particlePos;

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                // Player is looking at a block, spawn particles in the adjacent air block
                BlockPos adjacentPos = hitResult.getBlockPos().offset(hitResult.getSide());
                particlePos = Vec3d.ofCenter(adjacentPos);
            } else {
                // Player is looking at the air, spawn particles in front of them
                particlePos = user.getEyePos().add(user.getRotationVec(1.0f).multiply(1.5));
            }

            // Spawn a puff of leaf particles
            for (int i = 0; i < 100; i++) {
                double offsetX = world.random.nextGaussian() * 1.2;
                double offsetY = world.random.nextGaussian() * 1.2;
                double offsetZ = world.random.nextGaussian() * 1.2;
                world.addParticle(ModParticles.getForVariant(WoodVariant.OAK), // Use OAK as default
                        particlePos.x + offsetX, particlePos.y + offsetY, particlePos.z + offsetZ,
                        0, HamsterBeddingParticle.BEDDING_ITEM_FLAG, 0);
            }

            // Play leaf sound
            SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.random);
            if (rustleSound != null) {
                world.playSound(user, user.getBlockPos(), rustleSound, SoundCategory.PLAYERS, 0.2f, 1.5f);
            }
        }

        // Trigger advancement on server
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            ModCriteria.USED_HAMSTER_BEDDING.trigger(serverPlayer);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (Configs.AHP_UI.enableItemTooltips) {
            if (Screen.hasShiftDown()) {
                // --- Expanded Tooltip (Shift) ---
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bedding.hint1").formatted(Formatting.GOLD));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bedding.hint2").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bedding.hint3").formatted(Formatting.GRAY));
            } else {
                // --- Default Tooltip ---
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bedding.hint1").formatted(Formatting.GOLD));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.shift_for_info").formatted(Formatting.DARK_GRAY));
            }
        } else if (!Platform.isModLoaded("emi")) {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}