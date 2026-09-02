package net.dawson.adorablehamsterpets.integration.jade;

import net.minecraft.network.chat.ComponentSerialization;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

public enum HamsterBedServerData implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_bed_info");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof HamsterBedBlockEntity bedEntity) {
            if (accessor.getPlayer() instanceof ServerPlayer player) {
                ServerLevel serverWorld = ((ServerLevel) player.level());

                // Dynamic Name Resolution
                Optional<Component> liveName = bedEntity.getLinkedHamsterUuid()
                        .map(serverWorld::getEntity)
                        .filter(e -> e instanceof HamsterEntity)
                        .map(entity -> {
                            HamsterEntity hamster = (HamsterEntity) entity;
                            if (hamster.hasCustomName()) {
                                return hamster.getName();
                            } else {
                                // Use getDisplayName() to respect the "Hampter" config and append the ID
                                return hamster.getDisplayName().copy().append(" " + hamster.getId());
                            }
                        });

                // Use the live name if found; otherwise, fall back to the name stored in the BlockEntity.
                Component nameToShow = liveName.or(bedEntity::getLinkedHamsterName).orElse(null);

                if (nameToShow != null) {
                    data.store("LinkedHamsterName", ComponentSerialization.CODEC, nameToShow);
                }
            }

            data.putBoolean("WanderModeActive", bedEntity.isWanderModeActive());
            data.putString("WanderDistance", bedEntity.getWanderDistance().getSerializedName());
            data.putBoolean("AllowSleepInBed", bedEntity.isSleepingAllowed());
            data.putBoolean("RespawnEnabled", bedEntity.isRespawnEnabled());
            data.putBoolean("ConfigRespawnEnabled", Configs.AHP_MAIN.enableRespawnInBed.get());
            data.putBoolean("FreeBedRespawns", Configs.AHP_MAIN.freeBedRespawns.get());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
