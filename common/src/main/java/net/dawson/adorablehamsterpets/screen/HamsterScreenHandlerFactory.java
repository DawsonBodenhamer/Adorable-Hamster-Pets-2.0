package net.dawson.adorablehamsterpets.screen;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * A dedicated factory for creating the Hamster's inventory screen handler.
 * This class separates the screen-opening logic from the HamsterEntity itself,
 * resolving the getDisplayName() conflict between the entity's name and the inventory's title.
 */
public class HamsterScreenHandlerFactory implements ExtendedMenuProvider {
    private final HamsterEntity hamster;

    public HamsterScreenHandlerFactory(HamsterEntity hamster) {
        this.hamster = hamster;
    }

    /**
     * Provides the title for the inventory screen.
     * This is the text that will appear at the top of the GUI.
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.adorablehamsterpets.hamster.inventory_title");
    }

    /**
     * Creates the server-side instance of the screen handler.
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new HamsterInventoryScreenHandler(syncId, playerInventory, this.hamster);
    }

    /**
     * Writes the hamster's entity ID to the network buffer so the client can find it.
     */
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeInt(this.hamster.getId());
    }
}