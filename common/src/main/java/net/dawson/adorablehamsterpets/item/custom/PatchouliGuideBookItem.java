package net.dawson.adorablehamsterpets.item.custom;

import net.minecraft.world.item.Item;

/**
 * 26.2 port: Patchouli has no 26.2 build. The item is kept so existing
 * inventories and recipes stay valid, but it no longer opens a guide book --
 * it behaves as a plain item until Patchouli ships for 26.x.
 */
public class PatchouliGuideBookItem extends Item {
    public PatchouliGuideBookItem(Properties properties) {
        super(properties);
    }
}
