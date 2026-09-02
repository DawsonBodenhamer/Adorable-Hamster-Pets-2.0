package net.dawson.adorablehamsterpets.integration.patchouli;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

import java.text.NumberFormat;

/**
 * A Patchouli component processor designed to dynamically inject runtime
 * values directly into localized text.
 */
public class DynamicStringProcessor implements IComponentProcessor {

    private String processedText = "";

    @Override
    public void setup(Level level, IVariableProvider variables) {
        // Just modifying main body text
        if (variables.has("text")) {
            // Retrieve raw translation key provided by JSON entry
            String rawText = variables.get("text", level.registryAccess()).asString();

            // Patchouli normally translates text automatically right
            // before rendering, so manually translate it here first.
            if (I18n.exists(rawText)) {
                rawText = I18n.get(rawText);
            }

            // --- Genetics Token Injections ---
            if (rawText.contains("{BASE_PALETTES}")) {
                long basePalettes = HamsterPaletteManager.PALETTE_REGISTRY.size();
                String formattedNum = NumberFormat.getIntegerInstance().format(basePalettes);
                rawText = rawText.replace("{BASE_PALETTES}", formattedNum);
            }

            if (rawText.contains("{WILD_OVERLAYS}")) {
                long overlayPatterns = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size() - 1;
                long wildAllowedPalettesCount = HamsterPaletteManager.PALETTE_REGISTRY.values().stream()
                        .filter(p -> ConfigDataCache.getAllowedWildOverlayZones().contains(p.zone()))
                        .count();
                long totalWildOverlays = wildAllowedPalettesCount * overlayPatterns;
                String formattedNum = NumberFormat.getIntegerInstance().format(totalWildOverlays);
                rawText = rawText.replace("{WILD_OVERLAYS}", formattedNum);
            }

            if (rawText.contains("{TOTAL_WILD_VARIANTS}")) {
                long totalWildVariants = HamsterPaletteManager.calculateTotalWildVariants();
                String formattedNum = NumberFormat.getIntegerInstance().format(totalWildVariants);
                rawText = rawText.replace("{TOTAL_WILD_VARIANTS}", formattedNum);
            }

            if (rawText.contains("{TOTAL_WILD_PLUS_BREEDING_SAMPLE_VARIANTS}")) {
                long totalSampleVariants = HamsterPaletteManager.calculateTotalWildVariants() * 3L;
                String formattedNum = NumberFormat.getIntegerInstance().format(totalSampleVariants);
                rawText = rawText.replace("{TOTAL_WILD_PLUS_BREEDING_SAMPLE_VARIANTS}", formattedNum);
            }

            if (rawText.contains("{TOTAL_POSSIBLE_VARIANTS}")) {
                long totalBredVariants = HamsterPaletteManager.calculateTotalPossibleVariants();
                String formattedNum = NumberFormat.getIntegerInstance().format(totalBredVariants);
                rawText = rawText.replace("{TOTAL_POSSIBLE_VARIANTS}", formattedNum);
            }

            // --- Interaction Token Injections ---
            if (rawText.contains("{LURE_ITEM}")) {
                Component lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.lureItems);
                rawText = rawText.replace("{LURE_ITEM}", lureName.getString());
            }

            if (rawText.contains("{PACIFIST_ITEM}")) {
                Component itemName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_MAIN.becomePacifistItems);
                rawText = rawText.replace("{PACIFIST_ITEM}", itemName.getString());
            }

            if (rawText.contains("{NEUTRAL_ITEM}")) {
                Component itemName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_MAIN.becomeNeutralItems);
                rawText = rawText.replace("{NEUTRAL_ITEM}", itemName.getString());
            }

            if (rawText.contains("{MENACE_ITEM}")) {
                Component itemName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_MAIN.becomeMenaceItems);
                rawText = rawText.replace("{MENACE_ITEM}", itemName.getString());
            }

            if (rawText.contains("{RESURRECTION_TRIBUTE}")) {
                Component itemName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_MAIN.resurrectionTributes);
                rawText = rawText.replace("{RESURRECTION_TRIBUTE}", itemName.getString());
            }

            this.processedText = rawText;
        }
    }

    @Override
    public IVariable process(Level level, String key) {
        // Hand it my string when Patchouli asks for text variable
        if ("text".equals(key)) {
            return IVariable.wrap(this.processedText, level.registryAccess());
        }
        return null;
    }
}