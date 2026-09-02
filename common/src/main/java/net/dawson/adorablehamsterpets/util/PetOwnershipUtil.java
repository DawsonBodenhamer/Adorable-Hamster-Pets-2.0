package net.dawson.adorablehamsterpets.util;

import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;

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
        if (entity instanceof OwnableEntity tameable) {
            UUID ownerUuid = (tameable.getOwnerReference() == null ? null : tameable.getOwnerReference().getUUID());
            if (ownerUuid != null) {
                return ownerUuid;
            }
        }

        if (entity instanceof TraceableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner != null) {
                return owner.getUUID();
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
        return owner == null ? null : owner.getUUID();
    }

    @Nullable
    public static UUID resolveTargetOwnerUuid(Entity target) {
        return target instanceof Player ? target.getUUID() : resolveOwnerUuid(target);
    }

    @Nullable
    public static ServerPlayer resolveOnlineOwner(ServerLevel world, UUID ownerUuid) {
        return world.getServer().getPlayerList().getPlayer(ownerUuid);
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
