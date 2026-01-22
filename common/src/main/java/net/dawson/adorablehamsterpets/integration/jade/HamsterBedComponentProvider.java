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
            Text hamsterName = Text.Serializer.fromJson(serverData.getString("LinkedHamsterName"));
            if (hamsterName != null) {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.linked_to", hamsterName).formatted(Formatting.GREEN));
            }

            // Wander Mode Status and Distance
            boolean isWanderActive = serverData.getBoolean("WanderModeActive");
            Text wanderStatus = isWanderActive ? Text.literal("ACTIVE").formatted(Formatting.GREEN) : Text.literal("INACTIVE").formatted(Formatting.RED);
            WanderDistance distance = WanderDistance.valueOf(serverData.getString("WanderDistance").toUpperCase());
            int radius = switch (distance) {
                case NEAR -> Configs.AHP.wanderDistanceNear.get();
                case FAR -> Configs.AHP.wanderDistanceFar.get();
                default -> Configs.AHP.wanderDistanceMedium.get();
            };
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_status", wanderStatus, distance.asString(), radius));

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
                boolean isRespawnEnabled = serverData.getBoolean("RespawnEnabled");

                Text statusText;
                Text hintText;

                if (!isConfigRespawnEnabled) {
                    // State 1: Global config disabled
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.disabled_config");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.disabled_config");
                } else if (isRespawnEnabled) {
                    // State 2: Active
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.active");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.active");
                } else {
                    // State 3: Globally enabled, but inactive (Needs Tribute)
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.inactive");
                    // Dynamic Item Name Lookup
                    Text tributeName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP.resurrectionTributes);
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.inactive", tributeName.copy().formatted(Formatting.GOLD, Formatting.BOLD));
                }

                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.label", statusText));
                tooltip.add(hintText);

                // --- 3. Interaction Tips
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_controls1").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_controls2").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.lure_hint").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.repellent_hint").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.unlink_hint").formatted(Formatting.GRAY));
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
                    data.putString("LinkedHamsterName", Text.Serializer.toJson(nameToShow));
                }
            }

            data.putBoolean("WanderModeActive", bedEntity.isWanderModeActive());
            data.putString("WanderDistance", bedEntity.getWanderDistance().asString());
            data.putBoolean("AllowSleepInBed", bedEntity.isSleepingAllowed());
            data.putBoolean("RespawnEnabled", bedEntity.isRespawnEnabled());
            data.putBoolean("ConfigRespawnEnabled", Configs.AHP.enableRespawnInBed.get());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}