package net.dawson.adorablehamsterpets.integration.jade;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum HamsterGeneticsComponentProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_genetics");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains("HamsterGenome")) return;

        Player player = accessor.getPlayer();

        // --- Sneak Check ---
        if (Configs.AHP_UI.requireSneakForCustomJadeInfo && !player.isShiftKeyDown()) {
            tooltip.add(Component.translatable("tooltip.adorablehamsterpets.sneak_for_info").withStyle(ChatFormatting.GRAY));
            return;
        }

        HamsterGenome genome = HamsterGenome.readFromNbt(serverData.getCompoundOrEmpty("HamsterGenome"));

        // --- Formatted Age ---
        if (Configs.AHP_UI.showJadeAge) {
            long ageTicks = serverData.getLongOr("TotalAgeTicks", 0L);
            Component ageText = MiscUtil.TimeConversionUtil.formatAge(ageTicks);
            tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.genetics.age", ageText));
        }

        // --- Base Coat ---
        if (Configs.AHP_UI.showJadeBaseCoat) {
            tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.genetics.base", getPaletteText(genome.basePaletteId())));
        }

        // --- Wild Overlay ---
        if (Configs.AHP_UI.showJadeWildOverlay && genome.wildOverlayPattern() > 0 && genome.wildOverlayPaletteId() != null) {
            tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.genetics.wild",
                    getPatternText(genome.wildOverlayPattern()),
                    getPaletteText(genome.wildOverlayPaletteId())));
        }

        // --- Breeding Overlay ---
        if (Configs.AHP_UI.showJadeBreedingOverlay && genome.breedingOverlayPattern() > 0 && genome.breedingOverlayPaletteId() != null) {
            tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.genetics.breeding",
                    getPatternText(genome.breedingOverlayPattern()),
                    getPaletteText(genome.breedingOverlayPaletteId())));
        }

        // --- Eye Genetics ---
        if (Configs.AHP_UI.showJadeEyeColor) {
            String eyeKey = switch (genome.eyeGenotype()) {
                case 1 -> "tooltip.adorablehamsterpets.jade.genetics.eyes.carrier";
                case 2 -> "tooltip.adorablehamsterpets.jade.genetics.eyes.red";
                default -> "tooltip.adorablehamsterpets.jade.genetics.eyes.black";
            };
            tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.genetics.eyes", Component.translatable(eyeKey)));
        }

        // --- Aggression State ---
        if (Configs.AHP_UI.showJadeAggressionState && serverData.contains("AggressionState")) {
            int stateOrdinal = serverData.getIntOr("AggressionState", 0);

            // Only show if not "Standard"
            if (stateOrdinal != 0) {
                String stateKey = switch (stateOrdinal) {
                    case 1 -> "tooltip.adorablehamsterpets.jade.genetics.aggression.pacifist";
                    case 2 -> "tooltip.adorablehamsterpets.jade.genetics.aggression.menace";
                    default -> "tooltip.adorablehamsterpets.jade.genetics.aggression.standard";
                };
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.jade.genetics.aggression", Component.translatable(stateKey)));
            }
        }

        // --- Redstone Fever ---
        if (Configs.AHP_UI.showJadeRedstoneFeverRecovery && serverData.getBooleanOr("RedstoneFevered", false)) {
            String stateKey = switch (serverData.getIntOr("RedstoneFeverRecoveryStage", 0)) {
                case 2 -> "tooltip.adorablehamsterpets.jade.redstone_fever.nearly_cured";
                case 1 -> "tooltip.adorablehamsterpets.jade.redstone_fever.recovering";
                default -> "tooltip.adorablehamsterpets.jade.redstone_fever.severe";
            };
            tooltip.add(Component.translatable(
                    "tooltip.adorablehamsterpets.jade.redstone_fever",
                    Component.translatable(stateKey)));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof HamsterEntity hamster) {
            data.put("HamsterGenome", hamster.getGenome().saveToNbt());
            data.putLong("TotalAgeTicks", hamster.totalAgeTicks);
            data.putInt("AggressionState", hamster.getAggressionState().ordinal());
            data.putBoolean("RedstoneFevered", hamster.hasRedstoneFever());
            data.putInt("RedstoneFeverRecoveryStage", hamster.getRedstoneFeverRecoveryStage());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    /**
     * Resolves the palette name. If a translation key exists (for core palettes), it uses it.
     * Otherwise, it dynamically formats the ID (e.g., "cheesecake_mocha" -> "Cheesecake Mocha").
     */
    private Component getPaletteText(String paletteId) {
        if (paletteId == null) return Component.empty();
        String key = "hamster.palette.adorablehamsterpets." + paletteId;

        if (Language.getInstance().has(key)) {
            return Component.translatable(key);
        } else {
            return Component.literal(MiscUtil.formatHumanReadableName(paletteId));
        }
    }

    private Component getPatternText(int patternId) {
        if (patternId >= 0 && patternId < HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size()) {
            String patternName = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.get(patternId);
            return Component.translatable("hamster.pattern.adorablehamsterpets." + patternName);
        }
        return Component.translatable("hamster.pattern.adorablehamsterpets.unknown");
    }
}
