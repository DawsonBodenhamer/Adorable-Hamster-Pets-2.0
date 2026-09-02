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
        AHPJadeClientRegistrations.register(registration);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        // --- Server-Side Data Providers ---
        registration.registerBlockDataProvider(HamsterBedServerData.INSTANCE, HamsterBedBlockEntity.class);
        registration.registerEntityDataProvider(HamsterGeneticsServerData.INSTANCE, HamsterEntity.class);
        registration.registerEntityDataProvider(HamsterDebugServerData.INSTANCE, HamsterEntity.class);
    }
}