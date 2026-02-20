package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.advancement.Advancement;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HamsterSniffForOreGoal extends Goal {

    private final HamsterEntity hamster;
    private final World world;
    private BlockPos targetOrePos; // The specific ore block being targeted
    private boolean isSeekingDisappointingOre; // True if the current target is gold ore

    private enum SeekingState {
        IDLE,
        SCANNING,
        MOVING_TO_ORE,
        WAITING_FOR_PATH,
        CELEBRATING_DIAMOND,
        SULKING_AT_GOLD
    }

    private SeekingState currentState = SeekingState.IDLE;
    private int pathingTickTimer;
    private int soundTimer;
    @Nullable private Path path;

    private static final int PATHING_RECHECK_INTERVAL = 20; // Ticks (1 second)
    private static final int SNIFF_SOUND_INTERVAL_MOVING = 30; // Less than 2 seconds
    private static final int SNIFF_SOUND_INTERVAL_WAITING = 160; // Approx 8 seconds

    public HamsterSniffForOreGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (world.isClient || !Configs.AHP.enableIndependentDiamondSeeking) {
            return false;
        }
        // Check the isPrimedToSeekDiamonds flag directly
        if (!this.hamster.isPrimedToSeekDiamonds) {
            return false;
        }
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isCelebratingRetrieval()|| this.hamster.isSulking()) {
            return false;
        }
        if (this.hamster.getTarget() != null) { // In combat
            return false;
        }
        if (Configs.AHP.enableIndependentDiamondSeekCooldown &&
                this.hamster.foundOreCooldownEndTick > this.world.getTime()) {
            return false;
        }
        // Attempt to find a target only if all above conditions pass
        return findNewTargetOreAndSetState();
    }

    private boolean findNewTargetOreAndSetState() {
        this.targetOrePos = null; // Reset before scan
        this.isSeekingDisappointingOre = false;
        this.hamster.currentOreTarget = null; // Clear entity's direct target tracker initially

        List<BlockPos> exposedCelebrationOres = new ArrayList<>();
        List<BlockPos> buriedCelebrationOres = new ArrayList<>();
        List<BlockPos> buriedSulkingOres = new ArrayList<>(); // Only track buried "bad" ores for the mistake logic
        int radius = Configs.AHP.diamondSeekRadius.get();

        for (BlockPos pos : BlockPos.iterateOutwards(hamster.getBlockPos(), radius, radius, radius)) {
            BlockState state = world.getBlockState(pos);

            // Check if it is a "Desirable" ore (e.g. Diamond)
            if (ConfigDataCache.isCelebrationOre(state)) {
                if (isOreExposed(pos, this.world)) {
                    exposedCelebrationOres.add(pos.toImmutable());
                } else {
                    buriedCelebrationOres.add(pos.toImmutable());
                }
            }
            // Check if it is a "Disappointing" ore (e.g. Gold), and not exposed
            else if (ConfigDataCache.isSulkingOre(state)) {
                if (!isOreExposed(pos, this.world)) {
                    buriedSulkingOres.add(pos.toImmutable());
                }
            }
        }

        // --- Prioritized Target Selection ---
        boolean targetIsSulkingOre = !buriedSulkingOres.isEmpty() && this.world.random.nextFloat() < Configs.AHP.goldMistakeChance.get();

        if (targetIsSulkingOre) {
            buriedSulkingOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(hamster.getPos())));
            this.targetOrePos = buriedSulkingOres.get(0);
            this.isSeekingDisappointingOre = true;
        } else {
            if (!exposedCelebrationOres.isEmpty()) {
                exposedCelebrationOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(hamster.getPos())));
                this.targetOrePos = exposedCelebrationOres.get(0);
            } else if (!buriedCelebrationOres.isEmpty()) {
                buriedCelebrationOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(hamster.getPos())));
                this.targetOrePos = buriedCelebrationOres.get(0);
            }
        }

        if (this.targetOrePos != null) {
            this.hamster.currentOreTarget = this.targetOrePos;
            this.currentState = SeekingState.SCANNING;
            return true; // A target was selected
        }

        return false; // No valid target found
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (isSeekingDisappointingOre ? "_Gold" : "_Diamond"));
        this.pathingTickTimer = 0;
        this.soundTimer = 0;
        // currentState is already SCANNING from canStart/findNewTargetOreAndSetState
        attemptPathToTarget();
    }

    private void attemptPathToTarget() {
        if (this.targetOrePos == null) {
            this.currentState = SeekingState.IDLE;
            return;
        }
        // --- Store the Path ---
        this.path = this.hamster.getNavigation().findPathTo(
                this.targetOrePos.getX() + 0.5,
                this.targetOrePos.getY(),
                this.targetOrePos.getZ() + 0.5,
                0
        );

        if (this.path != null) {
            this.hamster.getNavigation().startMovingAlong(this.path, 0.5D);
            this.currentState = SeekingState.MOVING_TO_ORE;
            this.soundTimer = SNIFF_SOUND_INTERVAL_MOVING / 2;
        } else {
            this.currentState = SeekingState.WAITING_FOR_PATH;
            this.pathingTickTimer = PATHING_RECHECK_INTERVAL;
            this.soundTimer = SNIFF_SOUND_INTERVAL_WAITING / 2;
        }
    }

    @Override
    public boolean shouldContinue() {
        // Terminal states for this goal instance
        if (this.currentState == SeekingState.IDLE || this.currentState == SeekingState.CELEBRATING_DIAMOND || this.currentState == SeekingState.SULKING_AT_GOLD) {
            return false;
        }
        // Interruptions
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isSulking()) {
            return false;
        }
        if (this.hamster.getTarget() != null) { // Combat
            return false;
        }
        // Target validity
        if (this.targetOrePos == null) return false;

        BlockState targetState = world.getBlockState(this.targetOrePos);
        boolean isTargetCelebration = ConfigDataCache.isCelebrationOre(targetState);
        boolean isTargetSulking = ConfigDataCache.isSulkingOre(targetState);

        if (this.isSeekingDisappointingOre) {
            return isTargetSulking; // Target "bad" ore was broken or changed
        } else {
            return isTargetCelebration; // Target "good" ore was broken or changed
        }
    }

    @Override
    public void tick() {
        if (this.targetOrePos == null) {
            stop();
            return;
        }

        // Fast turn speed
        HamsterMovementUtil.facePosition(
                this.hamster,
                this.targetOrePos.getX() + 0.5,
                this.targetOrePos.getY() + 0.5,
                this.targetOrePos.getZ() + 0.5
        );

        if (this.soundTimer > 0) {
            this.soundTimer--;
        }

        switch (this.currentState) {
            case MOVING_TO_ORE:

                // Particle Breadcrumb Logic
                if (!this.world.isClient()) {
                    ParticleEffectsUtil.spawnBreadcrumbs((ServerWorld) this.world, this.path);
                }

                if (this.hamster.getNavigation().isIdle() || this.hamster.getBlockPos().isWithinDistance(this.targetOrePos, 1.5)) {
                    if (this.hamster.getBlockPos().isWithinDistance(this.targetOrePos, 1.5)) {
                        onOreReached();
                    } else {
                        this.path = null; // Clear old path
                        this.currentState = SeekingState.WAITING_FOR_PATH;
                        this.pathingTickTimer = PATHING_RECHECK_INTERVAL;
                        this.soundTimer = SNIFF_SOUND_INTERVAL_WAITING / 2;
                    }
                } else {
                    if (this.soundTimer <= 0) {
                        playSniffSound();
                        this.soundTimer = SNIFF_SOUND_INTERVAL_MOVING;
                    }
                }
                break;
            case WAITING_FOR_PATH:
                if (this.pathingTickTimer > 0) {
                    this.pathingTickTimer--;
                } else {
                    attemptPathToTarget();
                }
                if (this.soundTimer <= 0) {
                    playSniffSound();
                    this.soundTimer = SNIFF_SOUND_INTERVAL_WAITING;
                }
                break;
        }
    }

    private void onOreReached() {
        this.hamster.getNavigation().stop();
        this.hamster.isPrimedToSeekDiamonds = false;

        if (Configs.AHP.enableIndependentDiamondSeekCooldown) {
            this.hamster.foundOreCooldownEndTick = this.world.getTime() + Configs.AHP.independentOreSeekCooldownTicks.get();
        }

        if (this.isSeekingDisappointingOre) {
            this.currentState = SeekingState.SULKING_AT_GOLD;
            if (this.hamster.getOwner() instanceof ServerPlayerEntity owner) {
                if (this.hamster.squaredDistanceTo(owner) < 36.0) {
                    HamsterMovementUtil.faceEntity(this.hamster, owner);
                }
                // Send message to the owner
                sendMessageToOwner(owner);
            }

            // --- Startled Jump & Sound Logic ---
            // Calculate a vector pointing away from the target ore
            Vec3d awayFromOre = this.hamster.getPos().subtract(Vec3d.ofCenter(this.targetOrePos)).normalize();
            // Apply a small backward and upward velocity
            this.hamster.setVelocity(awayFromOre.x * 0.1, 0.5, awayFromOre.z * 0.1);
            this.hamster.velocityDirty = true; // Mark velocity for client sync
            // Play a random bounce sound at the hamster's location
            SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, this.hamster.getRandom());
            if (bounceSound != null) {
                this.world.playSound(null, this.hamster.getBlockPos(), bounceSound, SoundCategory.NEUTRAL, 0.6f, this.hamster.getSoundPitch());
            }

            this.hamster.setSulking(true);
            this.hamster.triggerAnimOnServer("mainController", "anim_hamster_sulk");

        } else {
            this.currentState = SeekingState.CELEBRATING_DIAMOND;
            this.hamster.setCelebratingDiamond(true); // Triggers begging animation
            AdorableHamsterPets.LOGGER.trace("Hamster {} reached CELEBRATING_DIAMOND state for ore at {}", this.hamster.getId(), this.targetOrePos);

            if (this.hamster.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
                ModCriteria.HAMSTER_LED_TO_DIAMOND.trigger(serverPlayerOwner, this.hamster, this.targetOrePos);
            }
        }
    }

    private void playSniffSound() {
        SoundEvent sniffSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, this.hamster.getRandom());
        if (sniffSound != null) {
            this.world.playSound(null, this.hamster.getBlockPos(), sniffSound, SoundCategory.NEUTRAL, 3.0F, this.hamster.getSoundPitch());
        }
    }

    @Override
    public void stop() {
        this.path = null; // Clear the path when the goal stops
        this.hamster.getNavigation().stop();
        boolean targetOreStillExists = false;

        if (this.targetOrePos != null) {
            BlockState targetState = world.getBlockState(this.targetOrePos);
            boolean isTargetCelebration = ConfigDataCache.isCelebrationOre(targetState);
            boolean isTargetSulking = ConfigDataCache.isSulkingOre(targetState);

            if (this.isSeekingDisappointingOre && isTargetSulking) targetOreStillExists = true;
            if (!this.isSeekingDisappointingOre && isTargetCelebration) targetOreStillExists = true;
        }

        // Only clear the "primed" flag if it didn't finish successfully
        if (this.currentState != SeekingState.CELEBRATING_DIAMOND && this.currentState != SeekingState.SULKING_AT_GOLD && !targetOreStillExists) {
            this.hamster.isPrimedToSeekDiamonds = false;
        }

        if (this.hamster.isCelebratingDiamond() && (this.currentState != SeekingState.CELEBRATING_DIAMOND || !targetOreStillExists)) {
            this.hamster.setCelebratingDiamond(false);
        }

        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
        this.currentState = SeekingState.IDLE;
        this.targetOrePos = null;
    }

    /**
     * Selects and sends a humorous message to the hamster's owner about finding gold.
     * <p>
     * This method implements specific logic to enhance the player experience:
     * <ul>
     *     <li><b>First-Time Experience:</b> It checks if the player has the
     *     {@code adorablehamsterpets:technical/hamster_found_gold_first_time} advancement.
     *     If not, it sends a specific, predetermined message (index 0) and grants the
     *     advancement to ensure this "first-time" message is only seen once per player.</li>
     *     <li><b>Subsequent Experiences:</b> For all subsequent times, it retrieves the index of the
     *     last message shown from the player's persistent NBT data (via the {@link PlayerEntityAccessor}).
     *     It then randomly selects a new message from the available pool, guaranteeing it will not be the
     *     same as the one shown immediately prior.</li>
     *     <li><b>State Persistence:</b> The index of the newly displayed message is saved back to the
     *     player's NBT data, ensuring the "don't repeat" logic works across game sessions.</li>
     * </ul>
     * The method also triggers the {@link ModCriteria#HAMSTER_FOUND_GOLD} criterion on every execution.
     *
     * @param owner The player who owns the hamster and will receive the message.
     */
    private void sendMessageToOwner(ServerPlayerEntity owner) {
        PlayerAdvancementTracker tracker = owner.getAdvancementTracker();
        Identifier advId = Identifier.of(AdorableHamsterPets.MOD_ID, "technical/hamster_found_gold_first_time");
        Advancement advancement = owner.server.getAdvancementLoader().get(advId);

        if (advancement == null) {
            AdorableHamsterPets.LOGGER.error("[GoldMessage] CRITICAL: Could not find advancement '{}'. Message will not be sent. Check file path and JSON validity.", advId);
            return;
        }

        AdvancementProgress progress = tracker.getProgress(advancement);
        int messageIndex;

        if (!progress.isDone()) {
            // First time ever for this player
            messageIndex = 0;
            // Grant the criterion using the Advancement object so this block doesn't run again
            for (String criterion : advancement.getCriteria().keySet()) {
                tracker.grantCriterion(advancement, criterion);
            }
        } else {
            // Subsequent times
            PlayerEntityAccessor accessor = (PlayerEntityAccessor) owner;
            int lastIndex = accessor.ahp_getLastGoldMessageIndex();

            List<Integer> possibleIndices = IntStream.range(0, 7).boxed().collect(Collectors.toList());
            if (lastIndex >= 0 && lastIndex < 7) {
                possibleIndices.remove(Integer.valueOf(lastIndex));
            }

            messageIndex = possibleIndices.get(this.world.random.nextInt(possibleIndices.size()));
        }

        // Save the new index and send the message
        ((PlayerEntityAccessor) owner).ahp_setLastGoldMessageIndex(messageIndex);
        String messageKey = "message.adorablehamsterpets.found_gold_mistake." + (messageIndex + 1);
        owner.sendMessage(Text.translatable(messageKey).formatted(Formatting.GOLD), true);

        // Trigger the criterion for any other potential uses
        ModCriteria.HAMSTER_FOUND_GOLD.trigger(owner);
    }

    /**
     * Checks if an ore block is "exposed" by having at least one adjacent air-like block.
     *
     * @param orePos The position of the ore block.
     * @return True if the ore is exposed, false otherwise.
     */
    public static boolean isOreExposed(BlockPos orePos, World world) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = orePos.offset(direction);
            // A block is considered "exposed" if the adjacent block has no collision shape (e.g., air, water, grass).
            if (world.getBlockState(adjacentPos).getCollisionShape(world, adjacentPos, ShapeContext.absent()).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}