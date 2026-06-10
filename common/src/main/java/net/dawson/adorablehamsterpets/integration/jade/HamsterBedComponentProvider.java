package net.dawson.adorablehamsterpets.integration.jade;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

public enum HamsterBedComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_bed_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        NbtCompound serverData = accessor.getServerData();
        RegistryWrapper.WrapperLookup registryLookup = accessor.getLevel().getRegistryManager();
        PlayerEntity player = accessor.getPlayer();

        if (serverData.contains("LinkedHamsterName")) {
            // --- Linked Bed Tooltip ---
            Text hamsterName = Text.Serialization.fromJson(serverData.getString("LinkedHamsterName"), registryLookup);
            if (hamsterName != null) {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.linked_to", hamsterName).formatted(Formatting.GREEN));
            }

            // --- Wander Mode Status and Distance ---
            boolean isWanderActive = serverData.getBoolean("WanderModeActive");

            Text wanderStatus = isWanderActive
                    ? Text.translatable("tooltip.adorablehamsterpets.jade.wander_status.active").formatted(Formatting.GREEN)
                    : Text.translatable("tooltip.adorablehamsterpets.jade.wander_status.inactive").formatted(Formatting.RED);

            WanderDistance distance = WanderDistance.valueOf(serverData.getString("WanderDistance").toUpperCase());
            int radius = switch (distance) {
                case NEAR -> Configs.AHP_MAIN.wanderDistanceNear.get();
                case FAR -> Configs.AHP_MAIN.wanderDistanceFar.get();
                default -> Configs.AHP_MAIN.wanderDistanceMedium.get();
            };

            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_status", wanderStatus, Text.translatable(distance.translationKey()), radius));

            if (player.isSneaking()) {
                // --- Expanded Tooltip (Sneaking) ---
                // --- 1. Sleep Status ---
                boolean allowSleep = serverData.getBoolean("AllowSleepInBed");
                Text sleepStatus = allowSleep
                        ? Text.translatable("tooltip.adorablehamsterpets.jade.sleep_status.allowed").formatted(Formatting.GREEN)
                        : Text.translatable("tooltip.adorablehamsterpets.jade.sleep_status.prevented").formatted(Formatting.RED);
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.sleep_status.label", sleepStatus));

                // --- 2. Respawn Status & Hint ---
                boolean isConfigRespawnEnabled = serverData.getBoolean("ConfigRespawnEnabled");
                boolean isFreeRespawns = serverData.getBoolean("FreeBedRespawns");
                boolean isRespawnEnabled = serverData.getBoolean("RespawnEnabled");

                Text statusText;
                Text hintText;

                if (!isConfigRespawnEnabled) {
                    // State 1: Global config disabled
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.disabled_config");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.disabled_config");
                } else if (isFreeRespawns) {
                    // State 2: Free Respawns Active
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.active");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.active_free");
                } else if (isRespawnEnabled) {
                    // State 3: Active (Tribute Required)
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.active");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.active");
                } else {
                    // State 4: Globally enabled, but inactive (Tribute Required)
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.inactive");
                    // Dynamic Item Name Lookup
                    Text tributeName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_MAIN.resurrectionTributes);
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.inactive", tributeName.copy().formatted(Formatting.GOLD, Formatting.BOLD));
                }

                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.label", statusText));
                tooltip.add(hintText);

                // --- 3. Static Interaction Tips ---
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_controls1").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_controls2").formatted(Formatting.GRAY));

                // --- 4. Dynamic Interaction Tips ---
                Text lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.lureItems).copy().formatted(Formatting.GOLD, Formatting.BOLD);
                Text repellentName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.bedAvoidanceFoods).copy().formatted(Formatting.RED, Formatting.BOLD);
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.lure_hint", lureName).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.repellent_hint", repellentName).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.unlink_hint", repellentName).formatted(Formatting.GRAY));
            } else {
                // --- Default (Condensed) Tooltip ---
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sneak_for_info").formatted(Formatting.GRAY));
            }

        } else {
            // --- Unlinked Bed Tooltip (shows regardless of sneak) ---
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.unlinked").formatted(Formatting.GOLD));
        }
    }

    @Override
    public void appendServerData(NbtCompound data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof HamsterBedBlockEntity bedEntity) {
            if (accessor.getPlayer() instanceof ServerPlayerEntity player) {
                ServerWorld serverWorld = player.getServerWorld();

                // Dynamic Name Resolution
                Optional<Text> liveName = bedEntity.getLinkedHamsterUuid()
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
                Text nameToShow = liveName.or(bedEntity::getLinkedHamsterName).orElse(null);

                if (nameToShow != null) {
                    data.putString("LinkedHamsterName", Text.Serialization.toJsonString(nameToShow, player.getRegistryManager()));
                }
            }

            data.putBoolean("WanderModeActive", bedEntity.isWanderModeActive());
            data.putString("WanderDistance", bedEntity.getWanderDistance().asString());
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