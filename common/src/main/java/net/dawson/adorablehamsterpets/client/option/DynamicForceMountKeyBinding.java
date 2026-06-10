package net.dawson.adorablehamsterpets.client.option;

import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * A custom KeyBinding that dynamically changes its display name in the Controls menu
 * based on the "Enable Force-Mount Keybind" configuration setting.
 */
public class DynamicForceMountKeyBinding extends KeyBinding {

    private final String enabledTranslationKey;

    /**
     * Constructs a new dynamic key binding.
     */
    public DynamicForceMountKeyBinding(String translationKey, int code, String category) {
        super(translationKey, InputUtil.Type.KEYSYM, code, category);
        this.enabledTranslationKey = translationKey;
    }

    /**
     * Overrides the default behavior to dynamically select a translation key.
     * This is called by the Controls screen when rendering the keybind's name.
     *
     * @return The appropriate translation key based on the current config setting.
     */
    @Override
    public String getTranslationKey() {
        if (Configs.AHP_MAIN.enableShoulderMountKeybind) {
            return this.enabledTranslationKey;
        } else {
            return this.enabledTranslationKey + ".disabled";
        }
    }
}