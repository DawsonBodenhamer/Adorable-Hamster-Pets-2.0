package net.dawson.adorablehamsterpets.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 26.2 port: {@code MenuScreens.register} is private and its
 * {@code ScreenConstructor} interface is package-private, and Fabric API no
 * longer exposes a screen registry. Fabric's menu API reaches the same map
 * through an access widener; from common code we get there with reflection and
 * a dynamic proxy for the constructor interface.
 */
public final class ClientScreenRegistration {
    private ClientScreenRegistration() {}

    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, Inventory inventory, Component title);
    }

    @SuppressWarnings("unchecked")
    public static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type, ScreenFactory<M, U> factory) {
        try {
            Class<?> constructorType = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Object constructor = Proxy.newProxyInstance(MenuScreens.class.getClassLoader(), new Class<?>[]{constructorType}, (proxy, method, args) -> {
                if (method.getName().equals("create") && args != null && args.length == 3) {
                    return factory.create((M) args[0], (Inventory) args[1], (Component) args[2]);
                }
                if (method.isDefault()) {
                    return java.lang.invoke.MethodHandles.privateLookupIn(constructorType, java.lang.invoke.MethodHandles.lookup())
                            .unreflectSpecial(method, constructorType).bindTo(proxy).invokeWithArguments(args == null ? new Object[0] : args);
                }
                return switch (method.getName()) {
                    case "toString" -> "AHP ScreenConstructor proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            });
            Method register = MenuScreens.class.getDeclaredMethod("register", MenuType.class, constructorType);
            register.setAccessible(true);
            register.invoke(null, type, constructor);
        } catch (ReflectiveOperationException e) {
            AdorableHamsterPets.LOGGER.error("Could not register hamster inventory screen", e);
        }
    }
}
