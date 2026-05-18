package net.dawson.adorablehamsterpets.accessor;

import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

/**
 * Accessor interface to expose custom methods injected into PlayerEntity by PlayerEntityMixin.
 * This allows other parts of the mod to safely call these methods without illegally
 * referencing the mixin class directly.
 */
public interface PlayerEntityAccessor {
    int ahp$getLastRandomMessageIndex(String context);
    void ahp$setLastRandomMessageIndex(String context, int index);

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
    void adorablehamsterpets$syncHamsterState();
    ArrayDeque<ShoulderLocation> adorablehamsterpets$getMountOrderQueue();
    ClientShoulderHamsterData adorablehamsterpets$getClientHamsterState();

    void adorablehamsterpets$startPrecisionTreeHeist(BlockPos leafPos);
    void ahp$registerTreeHeist(BlockPos treeId);
    float ahp$getHeistProfitability(BlockPos treeId);
    void ahp$clearHeistHistory();

    void ahp$initGuideBookTracking(boolean currentlyHasGuideBook);
    boolean ahp$computeHasGuideBook(PlayerEntity player);

    boolean ahp$canPlayTagGame();
    void ahp$incrementTagGameCount();

    boolean ahp$canBreedHamsters();
    void ahp$incrementHamstersFedForBreeding();
    void ahp$resetBreedingHistory();
    boolean ahp$addTamedGenome(int hash);
    boolean ahp$addBredGenome(int hash);
    int ahp$getTamedGenomeCount();
    int ahp$getBredGenomeCount();
    UUID ahp$getGeneticParent1Uuid();
    void ahp$setGeneticParent1Uuid(UUID uuid);
    UUID ahp$getGeneticParent2Uuid();
    void ahp$setGeneticParent2Uuid(UUID uuid);

    int ahp$getSupporterCrownTheme();
    void ahp$setSupporterCrownTheme(int theme);
    int ahp$getSupporterCrownAudioTimer();
    void ahp$setSupporterCrownAudioTimer(int timer);
    boolean ahp$hasUsedSupporterCrownTrial();
    void ahp$setHasUsedSupporterCrownTrial(boolean used);
    int ahp$getSupporterCrownTrialTicks();
    void ahp$setSupporterCrownTrialTicks(int ticks);

    List<NbtCompound> ahp$getInTransitHamsters();
    int ahp$getTransitTimer();
    void ahp$setTransitTimer(int timer);

    void ahp$startPettingHamster(int entityId);
    void ahp$cancelPettingHamster();

    boolean ahp$getWasSneaking();
    void ahp$setWasSneaking(boolean sneaking);
    int ahp$getSneakToggleCount();
    void ahp$setSneakToggleCount(int count);
    int ahp$getSneakToggleTimer();
    void ahp$setSneakToggleTimer(int timer);
}