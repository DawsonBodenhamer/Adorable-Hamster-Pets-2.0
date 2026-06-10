package net.dawson.adorablehamsterpets.integration.jade;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.util.MiscUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum HamsterGeneticsComponentProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_genetics");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        NbtCompound serverData = accessor.getServerData();
        if (!serverData.contains("HamsterGenome", NbtElement.COMPOUND_TYPE)) return;

        PlayerEntity player = accessor.getPlayer();

        // --- Sneak Check ---
        if (Configs.AHP_UI.requireSneakForCustomJadeInfo && !player.isSneaking()) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sneak_for_info").formatted(Formatting.GRAY));
            return;
        }

        HamsterGenome genome = HamsterGenome.readFromNbt(serverData.getCompound("HamsterGenome"));

        // --- Formatted Age ---
        if (Configs.AHP_UI.showJadeAge) {
            long ageTicks = serverData.getLong("TotalAgeTicks");
            Text ageText = MiscUtil.TimeConversionUtil.formatAge(ageTicks);
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.genetics.age", ageText));
        }

        // --- Base Coat ---
        if (Configs.AHP_UI.showJadeBaseCoat) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.genetics.base", getPaletteText(genome.basePaletteId())));
        }

        // --- Wild Overlay ---
        if (Configs.AHP_UI.showJadeWildOverlay && genome.wildOverlayPattern() > 0 && genome.wildOverlayPaletteId() != null) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.genetics.wild",
                    getPatternText(genome.wildOverlayPattern()),
                    getPaletteText(genome.wildOverlayPaletteId())));
        }

        // --- Breeding Overlay ---
        if (Configs.AHP_UI.showJadeBreedingOverlay && genome.breedingOverlayPattern() > 0 && genome.breedingOverlayPaletteId() != null) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.genetics.breeding",
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
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.genetics.eyes", Text.translatable(eyeKey)));
        }
    }

    @Override
    public void appendServerData(NbtCompound data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof HamsterEntity hamster) {
            data.put("HamsterGenome", hamster.getGenome().saveToNbt());
            data.putLong("TotalAgeTicks", hamster.totalAgeTicks);
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
    private Text getPaletteText(String paletteId) {
        if (paletteId == null) return Text.empty();
        String key = "hamster.palette.adorablehamsterpets." + paletteId;

        if (Language.getInstance().hasTranslation(key)) {
            return Text.translatable(key);
        } else {
            return Text.literal(MiscUtil.formatHumanReadableName(paletteId));
        }
    }

    private Text getPatternText(int patternId) {
        if (patternId >= 0 && patternId < HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size()) {
            String patternName = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.get(patternId);
            return Text.translatable("hamster.pattern.adorablehamsterpets." + patternName);
        }
        return Text.translatable("hamster.pattern.adorablehamsterpets.unknown");
    }
}