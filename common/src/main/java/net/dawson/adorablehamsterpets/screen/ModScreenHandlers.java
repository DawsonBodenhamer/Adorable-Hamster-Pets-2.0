package net.dawson.adorablehamsterpets.screen;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;

public class ModScreenHandlers {

    // --- 1. DeferredRegister for MenuTypes ---
    public static final DeferredRegister<ScreenHandlerType<?>> SCREEN_HANDLERS =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.SCREEN_HANDLER);

    // --- 2. Register the Extended Menu Type ---
    public static final RegistrySupplier<ScreenHandlerType<HamsterInventoryScreenHandler>> HAMSTER_INVENTORY_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("hamster_inventory", () ->
                    MenuRegistry.ofExtended((syncId, playerInventory, buf) -> {
                        // This is the CLIENT-SIDE factory. It reads the entity ID from the buffer.
                        final int entityId = buf.readInt();
                        final Entity entity = playerInventory.player.getWorld().getEntityById(entityId);
                        // We pass the found entity (or null) to the client-side constructor.
                        // The constructor itself will handle the case where the entity is not a hamster.
                        return new HamsterInventoryScreenHandler(syncId, playerInventory, (HamsterEntity) entity);
                    })
            );

    // --- 3. Main Registration Call ---
    public static void register() {
        SCREEN_HANDLERS.register();
    }
}