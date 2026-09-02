package net.dawson.adorablehamsterpets.client.option;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Holds the mod's key mappings. */
public class ModKeyBindings {
    // --- Translation Keys ---
    public static final String KEY_CATEGORY_AHP = "key.categories.adorablehamsterpets.main";
    public static final String KEY_THROW_HAMSTER = "key.adorablehamsterpets.throw_hamster";
    public static final String KEY_TOGGLE_SUPPORTER_CROWN = "key.adorablehamsterpets.toggle_supporter_crown";
    public static final String KEY_DISMOUNT_HAMSTER = "key.adorablehamsterpets.dismount_hamster";
    public static final String KEY_PET_HAMSTER = "key.adorablehamsterpets.pet_hamster";
    public static final String KEY_FORCE_MOUNT_HAMSTER = "key.adorablehamsterpets.force_mount_hamster";
    public static final String KEY_RIDE_HAMSTER = "key.adorablehamsterpets.ride_hamster";
    public static final String KEY_GENETICS_VISUALIZER_VAR_UP = "key.adorablehamsterpets.genetics_visualization.variance_up";
    public static final String KEY_GENETICS_VISUALIZER_VAR_DOWN = "key.adorablehamsterpets.genetics_visualization.variance_down";
    public static final String KEY_GENETICS_VISUALIZER_MUT_UP = "key.adorablehamsterpets.genetics_visualization.mutation_rate_up";
    public static final String KEY_GENETICS_VISUALIZER_MUT_DOWN = "key.adorablehamsterpets.genetics_visualization.mutation_rate_down";
    public static final String KEY_TOGGLE_PERFORMANCE_MODE = "key.adorablehamsterpets.toggle_performance_mode";

    // --- KeyBinding Instances ---
    public static KeyMapping THROW_HAMSTER_KEY;
    public static KeyMapping TOGGLE_SUPPORTER_CROWN_KEY;
    public static KeyMapping DISMOUNT_HAMSTER_KEY;
    public static KeyMapping PET_HAMSTER_KEY;
    public static KeyMapping FORCE_MOUNT_HAMSTER_KEY;
    public static KeyMapping RIDE_HAMSTER_KEY;
    public static KeyMapping GENETICS_VISUALIZER_VAR_UP_KEY;
    public static KeyMapping GENETICS_VISUALIZER_VAR_DOWN_KEY;
    public static KeyMapping GENETICS_VISUALIZER_MUT_UP_KEY;
    public static KeyMapping GENETICS_VISUALIZER_MUT_DOWN_KEY;
    public static KeyMapping TOGGLE_PERFORMANCE_MODE_KEY;

    /**
     * Initializes the KeyBinding objects. This should be called during client setup
     * before the keys are registered by the platform-specific loader.
     */
    public static void init() {
        THROW_HAMSTER_KEY = new KeyMapping(
                KEY_THROW_HAMSTER,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G, // Default to 'G'
                KEY_CATEGORY_AHP
        );

        TOGGLE_SUPPORTER_CROWN_KEY = new KeyMapping(
                KEY_TOGGLE_SUPPORTER_CROWN,
                InputConstants.UNKNOWN.getValue(), // Unbound by default
                KEY_CATEGORY_AHP
        );

        DISMOUNT_HAMSTER_KEY = new KeyMapping(
                KEY_DISMOUNT_HAMSTER,
                InputConstants.UNKNOWN.getValue(), // Unbound by default
                KEY_CATEGORY_AHP
        );

        PET_HAMSTER_KEY = new KeyMapping(
                KEY_PET_HAMSTER,
                InputConstants.UNKNOWN.getValue(), // Unbound by default
                KEY_CATEGORY_AHP
        );

        FORCE_MOUNT_HAMSTER_KEY = new DynamicForceMountKeyBinding(
                KEY_FORCE_MOUNT_HAMSTER,
                InputConstants.UNKNOWN.getValue(), // Unbound by default
                KEY_CATEGORY_AHP
        );

        RIDE_HAMSTER_KEY = new DynamicRideKeyBinding(
                KEY_RIDE_HAMSTER,
                InputConstants.UNKNOWN.getValue(), // Unbound by default
                KEY_CATEGORY_AHP
        );

        GENETICS_VISUALIZER_VAR_UP_KEY = new KeyMapping(
                KEY_GENETICS_VISUALIZER_VAR_UP,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                KEY_CATEGORY_AHP
        );

        GENETICS_VISUALIZER_VAR_DOWN_KEY = new KeyMapping(
                KEY_GENETICS_VISUALIZER_VAR_DOWN,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                KEY_CATEGORY_AHP
        );

        GENETICS_VISUALIZER_MUT_UP_KEY = new KeyMapping(
                KEY_GENETICS_VISUALIZER_MUT_UP,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                KEY_CATEGORY_AHP
        );

        GENETICS_VISUALIZER_MUT_DOWN_KEY = new KeyMapping(
                KEY_GENETICS_VISUALIZER_MUT_DOWN,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                KEY_CATEGORY_AHP
        );

        TOGGLE_PERFORMANCE_MODE_KEY = new KeyMapping(
                KEY_TOGGLE_PERFORMANCE_MODE,
                InputConstants.UNKNOWN.getValue(), // Unbound by default
                KEY_CATEGORY_AHP
        );
    }
}