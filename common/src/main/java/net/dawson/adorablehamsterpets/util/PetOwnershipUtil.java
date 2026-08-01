package net.dawson.adorablehamsterpets.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;

/** Resolves conventional vanilla and modded pet ownership without mod-specific shims. */
public final class PetOwnershipUtil {

    private static final ClassValue<OwnerAccessors> OWNER_ACCESSORS =
            new ClassValue<>() {
                @Override
                protected OwnerAccessors computeValue(Class<?> type) {
                    return new OwnerAccessors(
                            findMethod(type, "getOwnerUuid"),
                            findMethod(type, "getOwnerUUID"),
                            findMethod(type, "getOwner"));
                }
            };

    @Nullable
    public static UUID resolveOwnerUuid(Entity entity) {
        if (entity instanceof Tameable tameable) {
            UUID ownerUuid = tameable.getOwnerUuid();
            if (ownerUuid != null) {
                return ownerUuid;
            }
        }

        if (entity instanceof Ownable ownable) {
            Entity owner = ownable.getOwner();
            if (owner != null) {
                return owner.getUuid();
            }
        }

        OwnerAccessors accessors = OWNER_ACCESSORS.get(entity.getClass());
        UUID ownerUuid = invokeUuid(accessors.ownerUuid(), entity);
        if (ownerUuid == null) {
            ownerUuid = invokeUuid(accessors.ownerUUID(), entity);
        }
        if (ownerUuid != null) {
            return ownerUuid;
        }

        Entity owner = invokeEntity(accessors.owner(), entity);
        return owner == null ? null : owner.getUuid();
    }

    @Nullable
    public static UUID resolveTargetOwnerUuid(Entity target) {
        return target instanceof PlayerEntity ? target.getUuid() : resolveOwnerUuid(target);
    }

    @Nullable
    public static ServerPlayerEntity resolveOnlineOwner(ServerWorld world, UUID ownerUuid) {
        return world.getServer().getPlayerManager().getPlayer(ownerUuid);
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name) {
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(name))
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static UUID invokeUuid(@Nullable Method method, Entity target) {
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(target);
            return result instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Entity invokeEntity(@Nullable Method method, Entity target) {
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(target);
            return result instanceof Entity entity ? entity : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private record OwnerAccessors(
            @Nullable Method ownerUuid, @Nullable Method ownerUUID, @Nullable Method owner) {}

    private PetOwnershipUtil() {}
}
