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

public enum HamsterBedComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_bed_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        HolderLookup.Provider registryLookup = accessor.getLevel().registryAccess();
        Player player = accessor.getPlayer();

        if (serverData.contains("LinkedHamsterName")) {
            // --- Linked Bed Tooltip ---
            Component hamsterName = serverData.read("LinkedHamsterName", ComponentSerialization.CODEC).orElse(null);
            if (hamsterName != null) {
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.hamster_bed.linked_to", hamsterName).withStyle(ChatFormatting.GREEN));
            }

            // --- Wander Mode Status and Distance ---
            boolean isWanderActive = serverData.getBooleanOr("WanderModeActive", false);

            Component wanderStatus = isWanderActive
                    ? Component.translatable("tooltip.adorablehamsterpets.jade.wander_status.active").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("tooltip.adorablehamsterpets.jade.wander_status.inactive").withStyle(ChatFormatting.RED);

            WanderDistance distance = WanderDistance.valueOf(serverData.getStringOr("WanderDistance", "").toUpperCase());
            int radius = switch (distance) {
                case NEAR -> Configs.AHP_MAIN.wanderDistanceNear.get();
                case FAR -> Configs.AHP_MAIN.wanderDistanceFar.get();
                default -> Configs.AHP_MAIN.wanderDistanceMedium.get();
            };

            tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.wander_status", wanderStatus, Component.translatable(distance.translationKey()), radius));

            if (player.isShiftKeyDown()) {
                // --- Expanded Tooltip (Sneaking) ---
                // --- 1. Sleep Status ---
                boolean allowSleep = serverData.getBooleanOr("AllowSleepInBed", false);
                Component sleepStatus = allowSleep
                        ? Component.translatable("tooltip.adorablehamsterpets.jade.sleep_status.allowed").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("tooltip.adorablehamsterpets.jade.sleep_status.prevented").withStyle(ChatFormatting.RED);
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.sleep_status.label", sleepStatus));

                // --- 2. Respawn Status & Hint ---
                boolean isConfigRespawnEnabled = serverData.getBooleanOr("ConfigRespawnEnabled", false);
                boolean isFreeRespawns = serverData.getBooleanOr("FreeBedRespawns", false);
                boolean isRespawnEnabled = serverData.getBooleanOr("RespawnEnabled", false);

                Component statusText;
                Component hintText;

                if (!isConfigRespawnEnabled) {
                    // State 1: Global config disabled
                    statusText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.disabled_config");
                    hintText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.disabled_config");
                } else if (isFreeRespawns) {
                    // State 2: Free Respawns Active
                    statusText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.active");
                    hintText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.active_free");
                } else if (isRespawnEnabled) {
                    // State 3: Active (Tribute Required)
                    statusText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.active");
                    hintText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.active");
                } else {
                    // State 4: Globally enabled, but inactive (Tribute Required)
                    statusText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.inactive");
                    // Dynamic Item Name Lookup
                    Component tributeName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_MAIN.resurrectionTributes);
                    hintText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.inactive", tributeName.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }

                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.label", statusText));
                tooltip.add(hintText);

                // --- 3. Static Interaction Tips ---
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.wander_controls1").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.wander_controls2").withStyle(ChatFormatting.GRAY));

                // --- 4. Dynamic Interaction Tips ---
                Component lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.lureItems).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                Component repellentName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.bedAvoidanceFoods).copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.lure_hint", lureName).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.repellent_hint", repellentName).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.unlink_hint", repellentName).withStyle(ChatFormatting.GRAY));
            } else {
                // --- Default (Condensed) Tooltip ---
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.sneak_for_info").withStyle(ChatFormatting.GRAY));
            }

        } else {
            // --- Unlinked Bed Tooltip (shows regardless of sneak) ---
            tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.unlinked").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}