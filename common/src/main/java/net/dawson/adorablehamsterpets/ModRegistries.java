package net.dawson.adorablehamsterpets;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.payload.SpawnBeddingParticlesPayload;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Handles miscellaneous registrations that need to occur at specific lifecycle events.
 */
public class ModRegistries {

    /**
     * Registers items with the vanilla composter.
     * This is called directly during the common setup phase.
     */
    public static void registerCompostables() {
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.GREEN_BEANS.get(), 0.5f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.CUCUMBER.get(), 0.5f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.GREEN_BEAN_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.CUCUMBER_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.SUNFLOWER_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.HAMSTER_BEDDING.get(), 0.75f);
    }

    /**
     * Registers items with the vanilla dispenser.
     * This is called directly during the common setup phase.
     */
    public static void registerDispenserBehaviors() {
        DispenserBlock.registerBehavior(ModItems.HAMSTER_BEDDING.get(), new FallibleItemDispenserBehavior() {
            @Override
            protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                ServerWorld world = pointer.world();
                Direction direction = pointer.state().get(DispenserBlock.FACING);
                BlockPos pos = pointer.pos();

                // Find players in range to send packet and trigger advancement
                List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(p -> p.squaredDistanceTo(Vec3d.ofCenter(pos)) < 64 * 64);

                // Send custom packet with the default OAK variant
                NetworkManager.sendToPlayers(nearbyPlayers, new SpawnBeddingParticlesPayload(pos, direction, WoodVariant.OAK));

                // Trigger advancement for each nearby player
                for (ServerPlayerEntity player : nearbyPlayers) {
                    ModCriteria.DISPENSED_HAMSTER_BEDDING.get().trigger(player);
                }

                // Play leaf sound on server
                SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.random);
                if (rustleSound != null) {
                    world.playSound(null, pos, rustleSound, SoundCategory.BLOCKS, 0.15f, 1.2f);
                }

                stack.decrement(1);
                this.setSuccess(true);
                return stack;
            }
        });
    }
}