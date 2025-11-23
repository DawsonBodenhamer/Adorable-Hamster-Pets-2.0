package net.dawson.adorablehamsterpets.client.option;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.DismountTriggerType;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * A custom KeyBinding that dynamically changes its display name in the Controls menu
 * based on the current configuration settings. This prevents user confusion by clearly
 * indicating when the keybind is not active.
 */
public class DynamicDismountKeyBinding extends KeyBinding {

    private final String enabledTranslationKey;
    private final String disabledTranslationKey;


    /**
     * Constructs a new dynamic key binding.
     *
     * @param translationKey The base translation key for the keybind's name when it is enabled.
     * @param code           The default key code.
     * @param category       The translation key for the category this keybind belongs to.
     */
    public DynamicDismountKeyBinding(String translationKey, int code, String category) {
        super(translationKey, InputUtil.Type.KEYSYM, code, category);
        this.enabledTranslationKey = translationKey;
        this.disabledTranslationKey = translationKey + ".disabled";
    }

    /**
     * Overrides the default behavior to dynamically select a translation key.
     * This is called by the Controls screen when rendering the keybind's name.
     *
     * @return The appropriate translation key based on the current config setting.
     */
    @Override
    public String getTranslationKey() {
        // Check the live config value.
        if (Configs.AHP.dismountTriggerType == DismountTriggerType.CUSTOM_KEYBIND) {
            // If the custom keybind is enabled in the config, use the standard name.
            return this.enabledTranslationKey;
        } else {
            // Otherwise, use the special "disabled" name.
            return this.disabledTranslationKey;
        }
    }
}