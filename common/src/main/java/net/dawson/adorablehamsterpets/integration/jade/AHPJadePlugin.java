package net.dawson.adorablehamsterpets.integration.jade;

import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WildCucumberBushBlock;
import net.dawson.adorablehamsterpets.block.custom.WildGreenBeanBushBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.util.Identifier;
import snownee.jade.api.*;

@WailaPlugin
public final class AHPJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // --- Block Components ---
        registration.registerBlockComponent(WildBushComponentProvider.INSTANCE, WildCucumberBushBlock.class);
        registration.registerBlockComponent(WildBushComponentProvider.INSTANCE, WildGreenBeanBushBlock.class);
        registration.registerBlockComponent(HamsterBedComponentProvider.INSTANCE, HamsterBedBlock.class);
        registration.usePickedResult(ModBlocks.HAMSTER_BED.get());

        // --- Entity Components ---
        registration.registerEntityComponent(HamsterGeneticsComponentProvider.INSTANCE, HamsterEntity.class);
        registration.registerEntityComponent(HamsterDebugComponentProvider.INSTANCE, HamsterEntity.class);

        // --- Selective Default Component Removal ---
        // Intercept final tooltip collection to purge default Jade elements if configured
        registration.addTooltipCollectedCallback((tooltip, accessor) -> {
            if (accessor instanceof snownee.jade.api.EntityAccessor entityAccessor && entityAccessor.getEntity() instanceof HamsterEntity) {
                boolean playerSneaking = entityAccessor.getPlayer().isSneaking();
                boolean hideDueToSneak = Configs.AHP.requireSneakForDefaultJadeInfo && !playerSneaking;

                if (!Configs.AHP.showJadeEntityName || hideDueToSneak) {
                    tooltip.remove(new Identifier("jade", "object_name"));
                }
                if (!Configs.AHP.showJadeEntityHealth || hideDueToSneak) {
                    tooltip.remove(new Identifier("minecraft", "entity_health"));
                }
                if (!Configs.AHP.showJadeGrowthTime || hideDueToSneak) {
                    tooltip.remove(new Identifier("minecraft", "mob_growth"));
                }
                if (!Configs.AHP.showJadeOwner || hideDueToSneak) {
                    tooltip.remove(new Identifier("minecraft", "animal_owner"));
                }
                if (!Configs.AHP.showJadeInventory || hideDueToSneak) {
                    tooltip.remove(new Identifier("minecraft", "item_storage"));
                }
            }
        });
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        // --- Server-Side Data Providers ---
        registration.registerBlockDataProvider(HamsterBedComponentProvider.INSTANCE, HamsterBedBlockEntity.class);
        registration.registerEntityDataProvider(HamsterGeneticsComponentProvider.INSTANCE, HamsterEntity.class);
        registration.registerEntityDataProvider(HamsterDebugComponentProvider.INSTANCE, HamsterEntity.class);
    }
}