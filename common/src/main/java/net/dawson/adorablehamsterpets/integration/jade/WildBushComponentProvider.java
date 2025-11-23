package net.dawson.adorablehamsterpets.integration.jade;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WildCucumberBushBlock;
import net.dawson.adorablehamsterpets.block.custom.WildGreenBeanBushBlock;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum WildBushComponentProvider implements IBlockComponentProvider {
    INSTANCE; // Singleton instance

    // A unique identifier for this tooltip provider.
    // Used by Jade for configuration and internal tracking.
    private static final Identifier UID = Identifier.of(AdorableHamsterPets.MOD_ID, "wild_bush_tooltips");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // This method is called by Jade when the player looks at a block
        // that this provider is registered for.

        // Check if the block being looked at is a WildCucumberBushBlock
        if (accessor.getBlock() instanceof WildCucumberBushBlock) {
            // Add the same tooltip lines as your item
            tooltip.add(Text.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint1").formatted(Formatting.YELLOW));
            tooltip.add(Text.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint2").formatted(Formatting.GRAY));
        }
        // Check if the block being looked at is a WildGreenBeanBushBlock
        else if (accessor.getBlock() instanceof WildGreenBeanBushBlock) {
            // Add the same tooltip lines as your item
            tooltip.add(Text.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint1").formatted(Formatting.YELLOW));
            tooltip.add(Text.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint2").formatted(Formatting.GRAY));
        }
    }

    @Override
    public Identifier getUid() {
        return UID; // Return the unique ID for this provider
    }
}