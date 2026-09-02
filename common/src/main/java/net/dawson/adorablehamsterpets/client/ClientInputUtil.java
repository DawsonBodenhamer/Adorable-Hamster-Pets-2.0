package net.dawson.adorablehamsterpets.client;

import net.dawson.adorablehamsterpets.client.ClientInputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** 26.2 port: ClientInputUtil.hasShiftDown() is gone; ask GLFW directly. Client-only. */
public final class ClientInputUtil {
    private ClientInputUtil() {}

    public static boolean hasShiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
