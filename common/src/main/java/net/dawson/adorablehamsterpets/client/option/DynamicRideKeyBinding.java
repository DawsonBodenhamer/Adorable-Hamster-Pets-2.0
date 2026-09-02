package net.dawson.adorablehamsterpets.client.option;

import com.mojang.blaze3d.platform.InputConstants;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.client.KeyMapping;

/**
 * A custom KeyBinding that dynamically changes its display name in the Controls menu
 * based on the "Enable Hamster Riding" configuration setting.
 */
public class DynamicRideKeyBinding extends KeyMapping {

    private final String enabledTranslationKey;

    /**
     * Constructs a new dynamic key binding.
     */
    public DynamicRideKeyBinding(String translationKey, int code, String category) {
        super(translationKey, InputConstants.Type.KEYSYM, code, category);
        this.enabledTranslationKey = translationKey;
    }

    /**
     * Overrides the default behavior to dynamically select a translation key.
     * This is called by the Controls screen when rendering the keybind's name.
     *
     * @return The appropriate translation key based on the current config setting.
     */
    @Override
    public String getName() {
        if (Configs.AHP_MAIN.enableMountableHamsters.get()) {
            return this.enabledTranslationKey;
        } else {
            return this.enabledTranslationKey + ".disabled";
        }
    }
}