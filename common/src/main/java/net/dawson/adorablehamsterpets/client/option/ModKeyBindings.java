package net.dawson.adorablehamsterpets.client.option;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Holds the mod's key mappings. */
public class ModKeyBindings {
    // --- Translation Keys ---
    public static final String KEY_CATEGORY_HAMSTERPETS = "key.categories.adorablehamsterpets.main";
    public static final String KEY_THROW_HAMSTER = "key.adorablehamsterpets.throw_hamster";
    public static final String KEY_DISMOUNT_HAMSTER = "key.adorablehamsterpets.dismount_hamster";
    public static final String KEY_FORCE_MOUNT_HAMSTER = "key.adorablehamsterpets.force_mount_hamster";

    // --- KeyBinding Instances ---
    public static KeyBinding THROW_HAMSTER_KEY;
    public static KeyBinding DISMOUNT_HAMSTER_KEY;
    public static KeyBinding FORCE_MOUNT_HAMSTER_KEY;

    /**
     * Initializes the KeyBinding objects. This should be called during client setup
     * before the keys are registered by the platform-specific loader.
     */
    public static void init() {
        THROW_HAMSTER_KEY = new KeyBinding(
                KEY_THROW_HAMSTER,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G, // Default to 'G'
                KEY_CATEGORY_HAMSTERPETS
        );

        DISMOUNT_HAMSTER_KEY = new DynamicDismountKeyBinding(
                KEY_DISMOUNT_HAMSTER,
                InputUtil.UNKNOWN_KEY.getCode(), // Unbound by default
                KEY_CATEGORY_HAMSTERPETS
        );

        FORCE_MOUNT_HAMSTER_KEY = new DynamicForceMountKeyBinding(
                KEY_FORCE_MOUNT_HAMSTER,
                InputUtil.UNKNOWN_KEY.getCode(), // Unbound by default
                KEY_CATEGORY_HAMSTERPETS
        );
    }
}