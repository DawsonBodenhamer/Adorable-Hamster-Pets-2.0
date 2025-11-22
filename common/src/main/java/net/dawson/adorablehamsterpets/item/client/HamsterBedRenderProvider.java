package net.dawson.adorablehamsterpets.item.client;

import net.dawson.adorablehamsterpets.block.client.HamsterBedItemRenderer;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import software.bernie.geckolib.animatable.client.RenderProvider;

/**
 * A client-only helper class used to lazily instantiate the {@link RenderProvider}
 * for the Hamster Bed item.
 * <p>
 * This logic had to be isolated into a separate class on 1.20.1 to prevent {@link NoClassDefFoundError} on the server
 * by ensuring that client-only GeckoLib classes are not loaded during the item's common initialization.
 */
public class HamsterBedRenderProvider {
    public static RenderProvider create() {
        return new RenderProvider() {
            private HamsterBedItemRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new HamsterBedItemRenderer();
                return this.renderer;
            }
        };
    }
}