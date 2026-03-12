package net.dawson.adorablehamsterpets.accessor;

import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.minecraft.entity.player.PlayerEntity;
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

    void adorablehamsterpets$setRawHamsterState(net.minecraft.nbt.NbtCompound nbt);

    /**
     * Triggers the synchronization of shoulder data to the client.
     * This must be called AFTER the player has fully joined and the connection is established.
     */
    void adorablehamsterpets$syncHamsterState();

    ArrayDeque<ShoulderLocation> adorablehamsterpets$getMountOrderQueue();

    ClientShoulderHamsterData adorablehamsterpets$getClientHamsterState();

    /**
     * Initiates a Precision Tree Heist, forcing the hamster to use the targeted leaf block
     * as its exit point.
     * @param leafPos The position of the targeted leaf block.
     */
    void adorablehamsterpets$startPrecisionTreeHeist(BlockPos leafPos);

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

    /**
     * Initializes the server-side guidebook possession cache for this player.
     * Call after join (or any time book is removed/granted via code) to seed the cached state
     * without triggering acquisition effects.
     */
    void ahp$initGuideBookTracking(boolean currentlyHasGuideBook);

    /**
     * Checks whether the given player currently has at least one Hamster Tips Guide Book anywhere in their inventory.
     * Intended for seeding and polling guidebook tracking logic.
     */
    boolean ahp$computeHasGuideBook(PlayerEntity player);

    /**
     * Checks if the player is eligible to start a game of tag based on the
     * "Max Games Per Day" config. Automatically resets the counter if a new
     * Minecraft day has started since the last check.
     */
    boolean ahp$canPlayTagGame();

    /**
     * Increments the daily tag game counter and updates the last played timestamp.
     */
    void ahp$incrementTagGameCount();
}