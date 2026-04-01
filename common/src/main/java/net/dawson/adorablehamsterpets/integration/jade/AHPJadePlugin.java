package net.dawson.adorablehamsterpets.integration.jade;

import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WildCucumberBushBlock;
import net.dawson.adorablehamsterpets.block.custom.WildGreenBeanBushBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin // This annotation marks this class as a Jade plugin
public final class AHPJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Block components
        registration.registerBlockComponent(WildBushComponentProvider.INSTANCE, WildCucumberBushBlock.class);
        registration.registerBlockComponent(WildBushComponentProvider.INSTANCE, WildGreenBeanBushBlock.class);
        registration.registerBlockComponent(HamsterBedComponentProvider.INSTANCE, HamsterBedBlock.class);
        registration.usePickedResult(ModBlocks.HAMSTER_BED.get());

        // Entity components
        registration.registerEntityComponent(HamsterGeneticsComponentProvider.INSTANCE, HamsterEntity.class);
        registration.registerEntityComponent(HamsterDebugComponentProvider.INSTANCE, HamsterEntity.class);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Server-side data providers
        registration.registerBlockDataProvider(HamsterBedComponentProvider.INSTANCE, HamsterBedBlockEntity.class);
        registration.registerEntityDataProvider(HamsterGeneticsComponentProvider.INSTANCE, HamsterEntity.class);
        registration.registerEntityDataProvider(HamsterDebugComponentProvider.INSTANCE, HamsterEntity.class);
    }
}