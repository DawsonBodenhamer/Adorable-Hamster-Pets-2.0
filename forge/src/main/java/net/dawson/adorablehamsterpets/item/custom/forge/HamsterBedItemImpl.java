package net.dawson.adorablehamsterpets.item.custom.forge;

import net.dawson.adorablehamsterpets.block.client.HamsterBedItemRenderer;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.block.Block;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * The Forge-specific implementation for creating {@link HamsterBedItem} instances.
 * <p>
 * This factory returns an anonymous subclass of {@code HamsterBedItem} that overrides
 * {@code initializeClient}. This is required on Forge 1.20.1 to register the GeckoLib
 * renderer via {@link IClientItemExtensions},
 * as the standard {@code createRenderer} method is ignored by the loader.
 */
public class HamsterBedItemImpl {
    public static HamsterBedItem create(Block block, WoodVariant variant, Item.Settings settings) {
        return new HamsterBedItem(block, variant, settings) {
            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
                consumer.accept(new IClientItemExtensions() {
                    private HamsterBedItemRenderer renderer;

                    @Override
                    public BuiltinModelItemRenderer getCustomRenderer() {
                        if (this.renderer == null) {
                            this.renderer = new HamsterBedItemRenderer();
                        }
                        return this.renderer;
                    }
                });
            }
        };
    }
}