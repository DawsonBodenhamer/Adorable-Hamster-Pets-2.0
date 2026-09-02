package net.dawson.adorablehamsterpets.integration.jade;

import dev.architectury.utils.Env;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WildCucumberBushBlock;
import net.dawson.adorablehamsterpets.block.custom.WildGreenBeanBushBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.resources.Identifier;
import snownee.jade.api.*;

@WailaPlugin
public final class AHPJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // --- Block Components ---
        registration.registerBlockComponent(WildBushComponentProvider.INSTANCE, WildCucumberBushBlock.class);
        registration.registerBlockComponent(WildBushComponentProvider.INSTANCE, WildGreenBeanBushBlock.class);
        registration.registerBlockComponent(HamsterBedComponentProvider.INSTANCE, HamsterBedBlock.class);

        // --- Entity Components ---
        registration.registerEntityComponent(HamsterGeneticsComponentProvider.INSTANCE, HamsterEntity.class);
        registration.registerEntityComponent(HamsterDebugComponentProvider.INSTANCE, HamsterEntity.class);

        // --- Selective Default Component Removal ---
        // Intercept final tooltip collection to purge default Jade elements if configured
        registration.addTooltipCollectedCallback((tooltipBox, accessor) -> {
            if (accessor instanceof snownee.jade.api.EntityAccessor entityAccessor && entityAccessor.getEntity() instanceof HamsterEntity) {
                ITooltip tooltip = tooltipBox.getTooltip();
                boolean playerSneaking = entityAccessor.getPlayer().isShiftKeyDown();
                boolean hideDueToSneak = Configs.AHP_UI.requireSneakForDefaultJadeInfo && !playerSneaking;

                if (!Configs.AHP_UI.showJadeEntityName || hideDueToSneak) {
                    tooltip.remove(Identifier.fromNamespaceAndPath("jade", "object_name"));
                }
                if (!Configs.AHP_UI.showJadeEntityHealth || hideDueToSneak) {
                    tooltip.remove(Identifier.fromNamespaceAndPath("minecraft", "entity_health"));
                }
                if (!Configs.AHP_UI.showJadeGrowthTime || hideDueToSneak) {
                    tooltip.remove(Identifier.fromNamespaceAndPath("minecraft", "mob_growth"));
                }
                if (!Configs.AHP_UI.showJadeOwner || hideDueToSneak) {
                    tooltip.remove(Identifier.fromNamespaceAndPath("minecraft", "animal_owner"));
                }
                if (!Configs.AHP_UI.showJadeInventory || hideDueToSneak) {
                    tooltip.remove(Identifier.fromNamespaceAndPath("minecraft", "item_storage"));
                }
            }
        });
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 26.2 port: the component providers implement Jade's client-side interfaces, so
        // touching them on a dedicated server would load client classes and crash.
        if (Platform.getEnvironment() != Env.CLIENT) {
            return;
        }
        // --- Server-Side Data Providers ---
        registration.registerBlockDataProvider(HamsterBedComponentProvider.INSTANCE, HamsterBedBlockEntity.class);
        registration.registerEntityDataProvider(HamsterGeneticsComponentProvider.INSTANCE, HamsterEntity.class);
        registration.registerEntityDataProvider(HamsterDebugComponentProvider.INSTANCE, HamsterEntity.class);
    }
}