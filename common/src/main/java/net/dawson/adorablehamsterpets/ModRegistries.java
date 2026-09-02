package net.dawson.adorablehamsterpets;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.fuel.FuelRegistry;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.payload.SpawnBeddingParticlesPayload;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
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
        ComposterBlock.COMPOSTABLES.put(ModItems.GREEN_BEANS.get(), 0.5f);
        ComposterBlock.COMPOSTABLES.put(ModItems.CUCUMBER.get(), 0.5f);
        ComposterBlock.COMPOSTABLES.put(ModItems.GREEN_BEAN_SEEDS.get(), 0.25f);
        ComposterBlock.COMPOSTABLES.put(ModItems.CUCUMBER_SEEDS.get(), 0.25f);
        ComposterBlock.COMPOSTABLES.put(ModItems.SUNFLOWER_SEEDS.get(), 0.25f);
        ComposterBlock.COMPOSTABLES.put(ModItems.SUNFLOWER_BLOCK_ITEM.get(), 0.65f);
        ComposterBlock.COMPOSTABLES.put(ModItems.HAMSTER_BEDDING.get(), 0.75f);
        ComposterBlock.COMPOSTABLES.put(ModItems.ACORN.get(), 0.3f);
    }

    /**
     * Registers items with the vanilla dispenser.
     * This is called directly during the common setup phase.
     */
    public static void registerDispenserBehaviors() {
        DispenserBlock.registerBehavior(ModItems.HAMSTER_BEDDING.get(), new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                ServerLevel world = pointer.level();
                Direction direction = pointer.state().getValue(DispenserBlock.FACING);
                BlockPos pos = pointer.pos();

                // Find players in range to send packet and trigger advancement
                List<ServerPlayer> nearbyPlayers = world.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(pos)) < 64 * 64);

                // Send custom packet with the default OAK variant
                NetworkManager.sendToPlayers(nearbyPlayers, new SpawnBeddingParticlesPayload(pos, direction, WoodVariant.OAK));

                // Trigger advancement for each nearby player
                for (ServerPlayer player : nearbyPlayers) {
                    ModCriteria.DISPENSED_HAMSTER_BEDDING.get().trigger(player);
                }

                // Play leaf sound on server
                SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.random);
                if (rustleSound != null) {
                    world.playSound(null, pos, rustleSound, SoundSource.BLOCKS, 0.15f, 1.2f);
                }

                stack.shrink(1);
                this.setSuccess(true);
                return stack;
            }
        });
    }

    /**
     * Registers burn times for items used as furnace fuel.
     * This is called directly during the common setup phase.
     */
    public static void registerFuels() {
        // 100 ticks = 0.5 items smelted (same as vanilla stick)
        FuelRegistry.register(100, ModItems.ACORN.get());
    }
}