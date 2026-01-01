package net.dawson.adorablehamsterpets.accessor;

import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;

/**
 * Accessor interface to expose custom methods injected into PlayerEntity by PlayerEntityMixin.
 * This allows other parts of the mod to safely call these methods without illegally
 * referencing the mixin class directly.
 */
public interface PlayerEntityAccessor {
    NbtCompound getShoulderHamster(ShoulderLocation location);
    void setShoulderHamster(ShoulderLocation location, NbtCompound nbt);

    boolean hasAnyShoulderHamster();

    int ahp_getLastGoldMessageIndex();
    void ahp_setLastGoldMessageIndex(int index);

    void adorablehamsterpets$dismountShoulderHamster(boolean isThrow);

    default void adorablehamsterpets$dismountShoulderHamster() {
        adorablehamsterpets$dismountShoulderHamster(false);
    }

    void adorablehamsterpets$setRawShoulderData(net.minecraft.nbt.NbtCompound nbt);

    /**
     * Triggers the synchronization of shoulder data to the client.
     * This must be called AFTER the player has fully joined and the connection is established.
     */
    void adorablehamsterpets$syncShoulderData();

    ArrayDeque<ShoulderLocation> adorablehamsterpets$getMountOrderQueue();

    ClientShoulderHamsterData adorablehamsterpets$getClientShoulderData();

    /**
     * Registers a new tree heist for a specific Tree ID (Anchor position).
     */
    void ahp$registerTreeHeist(BlockPos treeId);

    /**
     * Calculates the profitability multiplier (0.0 to 1.0) for a specific Tree ID.
     */
    float ahp$getHeistProfitability(BlockPos treeId);

    /**
     * Clears all recorded tree heist history for this player.
     */
    void ahp$clearHeistHistory();
}