package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Isolates NBT serialization and deserialization logic for Hamsters.
 */
public final class HamsterNbtUtil {

    private HamsterNbtUtil() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *                        Core Data Serialization
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void writeCustomDataToNbt(HamsterEntity hamster, NbtCompound nbt) {
        // --- 1. Core Data & Flags ---
        nbt.put("HamsterGenome", hamster.getGenome().saveToNbt());
        nbt.putLong("TotalAgeTicks", hamster.totalAgeTicks);
        nbt.putInt("TimesBred", hamster.timesBred);
        // Write flags as individual booleans for backwards compat
        if (hamster.isTamed()) {
            nbt.putBoolean("Sitting", hamster.getHamsterFlag(HamsterEntity.SITTING_FLAG));
            nbt.putBoolean("IsSleeping", hamster.getHamsterFlag(HamsterEntity.SLEEPING_FLAG));
        } else {
            nbt.putBoolean("IsSleeping", false);
        }
        nbt.putBoolean("KnockedOut", hamster.getHamsterFlag(HamsterEntity.KNOCKED_OUT_FLAG));
        nbt.putBoolean("CheekPouchUnlocked", hamster.getHamsterFlag(HamsterEntity.CHEEK_POUCH_UNLOCKED_FLAG));
        nbt.putLong("ThrowCooldownEnd", hamster.throwCooldownEndTick);
        nbt.putLong("GreenBeanBuffDuration", hamster.getDataTracker().get(HamsterEntity.GREEN_BEAN_BUFF_DURATION));
        nbt.putInt("AutoEatCooldown", hamster.getAutoEatCooldownTicks());
        nbt.putInt("EjectionCheckCooldown", hamster.getEjectionCheckCooldown());
        nbt.putInt("PinkPetalType", hamster.getDataTracker().get(HamsterEntity.PINK_PETAL_TYPE));
        nbt.putInt("AnimationPersonalityId", hamster.getDataTracker().get(HamsterEntity.ANIMATION_PERSONALITY_ID));
        nbt.putBoolean("isGeneticsVisualizerMember", hamster.isGeneticsVisualizerMember());

        // --- 2. Parent Following ---
        if (hamster.getParentUuid() != null) {
            nbt.putUuid("ParentUuid", hamster.getParentUuid());
        }

        // --- 3. Sleep State ---
        nbt.putInt("DozingPhase", hamster.getDozingPhase().ordinal());
        nbt.putString("CurrentDeepSleepAnimId", hamster.getDataTracker().get(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID));
        nbt.putInt("QuiescentSitTimer", hamster.getQuiescentSitTimer());
        nbt.putInt("DriftingOffTimer", hamster.getDriftingOffTimer());
        nbt.putInt("SettleSleepCooldown", hamster.getSettleSleepCooldown());

        // --- 4. Inventory ---
        RegistryWrapper.WrapperLookup registries = hamster.getWorld().getRegistryManager();
        NbtCompound inventoryWrapperNbt = new NbtCompound();
        Inventories.writeNbt(inventoryWrapperNbt, hamster.getItems(), registries);
        nbt.put("Inventory", inventoryWrapperNbt);

        // --- 5. Ore Seeking ---
        nbt.putBoolean("IsPrimedToSeekDiamonds", hamster.isPrimedToSeekDiamonds);
        nbt.putLong("FoundOreCooldownEndTick", hamster.foundOreCooldownEndTick);
        if (hamster.currentOreTarget != null) {
            nbt.putInt("OreTargetX", hamster.currentOreTarget.getX());
            nbt.putInt("OreTargetY", hamster.currentOreTarget.getY());
            nbt.putInt("OreTargetZ", hamster.currentOreTarget.getZ());
        }
        nbt.putBoolean("IsSulking", hamster.getHamsterFlag(HamsterEntity.SULKING_FLAG));
        nbt.putBoolean("IsCelebratingDiamond", hamster.getHamsterFlag(HamsterEntity.CELEBRATING_DIAMOND_FLAG));

        // --- 6. Interaction & Mini-Game ---
        nbt.putLong("TagGameCooldownEnd", hamster.tagGameCooldownEndTick);
        nbt.putLong("StealingCooldownEnd", hamster.stealingCooldownEndTick);
        if (hamster.getGenericInteractionTimer() > 0) {
            nbt.putInt("GenericInteractionTimer", hamster.getGenericInteractionTimer());
        }
        if (hamster.isHoldingMouthItem()) {
            nbt.putBoolean("IsHoldingMouthItem", true);
            if (!hamster.getMouthItemStack().isEmpty()) {
                nbt.put("MouthItemStack", hamster.getMouthItemStack().encode(registries));
            }
        }

        // --- 7. Wander Mode ---
        nbt.putBoolean("IsWanderModeActive", hamster.isWanderModeActive());
        hamster.getLinkedBedPos().ifPresent(globalPos ->
                nbt.put("LinkedBedPos", GlobalPos.CODEC.encodeStart(hamster.getWorld().getRegistryManager().getOps(NbtOps.INSTANCE), globalPos).getOrThrow()));
        nbt.putBoolean("BypassNextSleepDelay", hamster.shouldBypassNextSleepDelay());
        nbt.putBoolean("StuckSearchingForBed", hamster.isStuckSearchingForBed());
        nbt.putBoolean("IsRescueSleeping", hamster.isRescueSleeping());
    }

    public static void readCustomDataFromNbt(HamsterEntity hamster, NbtCompound nbt) {
        // --- 1. Read Core Data ---
        hamster.setLoadingNbt(true); // Suppress sounds
        hamster.totalAgeTicks = nbt.getLong("TotalAgeTicks");
        hamster.timesBred = nbt.getInt("TimesBred");
        // Migrate legacy variant IDs to v3.6.0's Genome structure
        if (nbt.contains("HamsterGenome", NbtElement.COMPOUND_TYPE)) {
            hamster.setGenome(HamsterGenome.readFromNbt(nbt.getCompound("HamsterGenome")));
        } else if (nbt.contains("HamsterVariant", NbtElement.INT_TYPE)) {
            // Catch old integer IDs from pre 3.6.0
            int legacyId = nbt.getInt("HamsterVariant");
            hamster.setGenome(HamsterGeneticsUtil.getGenomeForLegacyId(legacyId));
        } else {
            hamster.setGenome(HamsterGenome.createDefault());
        }
        // Backwards compat: read individual booleans & set flags
        boolean wasSittingNbt = hamster.isTamed() && nbt.getBoolean("Sitting");
        hamster.setSitting(wasSittingNbt, true); // This will correctly set the SITTING_FLAG
        hamster.setHamsterFlag(HamsterEntity.KNOCKED_OUT_FLAG, nbt.getBoolean("KnockedOut"));
        hamster.setHamsterFlag(HamsterEntity.CHEEK_POUCH_UNLOCKED_FLAG, nbt.getBoolean("CheekPouchUnlocked"));
        hamster.setHamsterFlag(HamsterEntity.SULKING_FLAG, nbt.getBoolean("IsSulking"));
        hamster.setHamsterFlag(HamsterEntity.CELEBRATING_DIAMOND_FLAG, nbt.getBoolean("IsCelebratingDiamond"));
        boolean loadedSleeping = nbt.getBoolean("IsSleeping");
        if (!hamster.isTamed()) {
            loadedSleeping = false;
        }
        hamster.setHamsterFlag(HamsterEntity.SLEEPING_FLAG, loadedSleeping);
        hamster.throwCooldownEndTick = nbt.getLong("ThrowCooldownEnd");
        hamster.getDataTracker().set(HamsterEntity.GREEN_BEAN_BUFF_DURATION, nbt.getLong("GreenBeanBuffDuration"));
        hamster.setAutoEatCooldownTicks(nbt.getInt("AutoEatCooldown"));
        hamster.setEjectionCheckCooldown(nbt.contains("EjectionCheckCooldown", NbtElement.INT_TYPE) ? nbt.getInt("EjectionCheckCooldown") : 20);
        hamster.getDataTracker().set(HamsterEntity.PINK_PETAL_TYPE, nbt.getInt("PinkPetalType"));
        // Backwards compat: personality ID verification
        if (!nbt.contains("AnimationPersonalityId", NbtElement.INT_TYPE)) {
            int personalityId = hamster.getRandom().nextBetween(1, 3);
            hamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, personalityId);
            AdorableHamsterPets.LOGGER.debug("[NBT READ] Hamster ID {}: NBT had no personality, assigned new ID {}", hamster.getId(), personalityId);
        } else {
            hamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, nbt.getInt("AnimationPersonalityId"));
        }
        hamster.setGeneticsVisualizerMember(nbt.getBoolean("isGeneticsVisualizerMember"));

        // --- 2. Parent Following ---
        if (nbt.containsUuid("ParentUuid")) {
            hamster.setParentUuid(nbt.getUuid("ParentUuid"));
        }

        // --- 3. Sleep State ---
        if (nbt.contains("DozingPhase", NbtElement.INT_TYPE)) {
            int phaseOrdinal = nbt.getInt("DozingPhase");
            if (phaseOrdinal >= 0 && phaseOrdinal < HamsterEntity.DozingPhase.values().length) {
                HamsterEntity.DozingPhase phase = HamsterEntity.DozingPhase.values()[phaseOrdinal];
                hamster.setDozingPhase(phase);
                if (phase == HamsterEntity.DozingPhase.DEEP_SLEEP) {
                    hamster.setHamsterFlag(HamsterEntity.SLEEPING_FLAG, true);
                }
            } else {
                hamster.setDozingPhase(HamsterEntity.DozingPhase.NONE);
            }
        } else {
            hamster.setDozingPhase(HamsterEntity.DozingPhase.NONE);
        }
        hamster.getDataTracker().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, nbt.getString("CurrentDeepSleepAnimId"));
        hamster.setQuiescentSitTimer(nbt.getInt("QuiescentSitTimer"));
        hamster.setDriftingOffTimer(nbt.getInt("DriftingOffTimer"));
        hamster.setSettleSleepCooldown(nbt.getInt("SettleSleepCooldown"));

        // --- 4. Inventory ---
        hamster.getItems().clear();
        RegistryWrapper.WrapperLookup registries = hamster.getWorld().getRegistryManager();
        if (nbt.contains("Inventory", NbtElement.COMPOUND_TYPE)) {
            Inventories.readNbt(nbt.getCompound("Inventory"), hamster.getItems(), registries);
        }
        if (!hasInventoryData(nbt) && !hamster.isTamed()) {
            HamsterInventoryUtil.generateWildLoot(hamster, hamster.getRandom());
        }
        HamsterInventoryUtil.updateCheekStates(hamster);
        HamsterInventoryUtil.syncEquipmentTrackers(hamster);

        // --- 5. Ore Seeking ---
        hamster.isPrimedToSeekDiamonds = nbt.getBoolean("IsPrimedToSeekDiamonds");
        hamster.foundOreCooldownEndTick = nbt.getLong("FoundOreCooldownEndTick");
        if (nbt.contains("OreTargetX") && nbt.contains("OreTargetY") && nbt.contains("OreTargetZ")) {
            hamster.currentOreTarget = new BlockPos(nbt.getInt("OreTargetX"), nbt.getInt("OreTargetY"), nbt.getInt("OreTargetZ"));
        } else {
            hamster.currentOreTarget = null;
        }

        // --- 6. Interaction & Mini-Game ---
        hamster.tagGameCooldownEndTick = nbt.getLong("TagGameCooldownEnd");
        hamster.stealingCooldownEndTick = nbt.getLong("StealingCooldownEnd");
        hamster.setGenericInteractionTimer(nbt.getInt("GenericInteractionTimer"));

        boolean holding = nbt.getBoolean("IsHoldingMouthItem");
        hamster.setHoldingMouthItem(holding);

        if (holding) {
            if (nbt.contains("MouthItemStack", NbtElement.COMPOUND_TYPE)) {
                ItemStack.fromNbt(registries, nbt.getCompound("MouthItemStack")).ifPresent(hamster::setMouthItemStack);
            }
        } else {
            hamster.setMouthItemStack(ItemStack.EMPTY);
        }

        // --- 7. Wander Mode ---
        hamster.setWanderModeActive(nbt.getBoolean("IsWanderModeActive"));
        if (nbt.contains("LinkedBedPos")) {
            hamster.setLinkedBedPos(GlobalPos.CODEC.parse(hamster.getWorld().getRegistryManager().getOps(NbtOps.INSTANCE), nbt.get("LinkedBedPos")).result());
        } else {
            hamster.setLinkedBedPos(Optional.empty());
        }
        hamster.setBypassNextSleepDelay(nbt.getBoolean("BypassNextSleepDelay"));
        hamster.setStuckSearchingForBed(nbt.getBoolean("StuckSearchingForBed"));
        hamster.setRescueSleeping(nbt.getBoolean("IsRescueSleeping"));
        if (hamster.isRescueSleeping()) {
            hamster.setHamsterFlag(HamsterEntity.SLEEPING_FLAG, true);
        }

        // --- 8. Reconcile Accessory State ---
        hamster.updateAccessoryState();

        hamster.setLoadingNbt(false);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                       Shoulder Data Handlers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Takes a hamster's NBT data, deserializes it, sets the knocked-out flag,
     * and re-serializes it to a new NbtCompound.
     */
    public static NbtCompound setKnockedOutInNbt(NbtCompound originalNbt) {
        return HamsterState.fromNbt(originalNbt).map(data -> {
            int newFlags = data.hamsterFlags() | HamsterEntity.KNOCKED_OUT_FLAG;
            return data.withFlags(newFlags).toNbt();
        }).orElse(originalNbt); // Fallback
    }

    /**
     * Captures the current state of this hamster into a {@link HamsterState} record.
     */
    public static HamsterState saveToHamsterState(HamsterEntity hamster) {
        // --- 1. Update Trackers and Prepare NBT ---
        HamsterInventoryUtil.updateCheekStates(hamster);
        NbtCompound inventoryNbt = new NbtCompound();
        if (hamster.getWorld() instanceof ServerWorld serverWorld) {
            Inventories.writeNbt(inventoryNbt, hamster.getItems(), serverWorld.getRegistryManager());
        }

        // --- 2. Save Active Status Effects to NBT ---
        NbtCompound effectsNbt = new NbtCompound();
        if (!hamster.getStatusEffects().isEmpty()) {
            NbtList effectsList = new NbtList();
            for (StatusEffectInstance effectInstance : hamster.getStatusEffects()) {
                effectsList.add(effectInstance.writeNbt());
            }
            effectsNbt.put("active_effects", effectsList);
        }

        // --- 3. Get Custom Name ---
        Optional<String> nameOptional = Optional.ofNullable(hamster.getCustomName()).map(Text::getString);

        // --- 4. Create Inner Data Record Instances ---
        HamsterState.SeekingBehaviorData seekingData = new HamsterState.SeekingBehaviorData(
                hamster.isPrimedToSeekDiamonds,
                hamster.foundOreCooldownEndTick,
                Optional.ofNullable(hamster.currentOreTarget)
        );
        HamsterState.GreenBeanBuffData buffData = new HamsterState.GreenBeanBuffData(
                hamster.getGreenBeanBuffEndTick(),
                hamster.getDataTracker().get(HamsterEntity.GREEN_BEAN_BUFF_DURATION),
                effectsNbt
        );
        HamsterState.WanderModeData wanderData = new HamsterState.WanderModeData(
                hamster.getLinkedBedPos(),
                hamster.shouldBypassNextSleepDelay()
        );

        // --- 5. Create and Return the Main Data Record ---
        return new HamsterState(
                hamster.getUuid(),
                hamster.getGenome().saveToNbt(),
                hamster.getHealth(),
                inventoryNbt,
                hamster.getBreedingAge(),
                hamster.throwCooldownEndTick,
                buffData,
                hamster.getAutoEatCooldownTicks(),
                nameOptional,
                hamster.getDataTracker().get(HamsterEntity.PINK_PETAL_TYPE),
                hamster.getDataTracker().get(HamsterEntity.ANIMATION_PERSONALITY_ID),
                seekingData,
                wanderData,
                hamster.getDataTracker().get(HamsterEntity.HAMSTER_FLAGS),
                hamster.totalAgeTicks,
                hamster.timesBred
        );
    }

    /**
     * Creates a HamsterEntity instance from NBT data, typically from a player's shoulder.
     * Loads the hamster's variant, health, age, inventory, effects, and custom name.
     * Does NOT set the entity's position or spawn it in the world.
     */
    @Nullable
    public static HamsterEntity createFromNbt(ServerWorld world, @Nullable PlayerEntity player, NbtCompound nbt) {
        Optional<HamsterState> dataOpt = HamsterState.fromNbt(nbt);
        if (dataOpt.isEmpty()) {
            AdorableHamsterPets.LOGGER.error("Failed to deserialize HamsterState from NBT: {}", nbt);
            return null;
        }
        HamsterState data = dataOpt.get();

        HamsterEntity hamster = ModEntities.HAMSTER.get().create(world);

        if (hamster != null) {
            // --- 1. Load Core Data ---
            hamster.setUuid(data.entityUuid());
            hamster.setGenome(HamsterGenome.readFromNbt(data.genomeNbt()));
            hamster.setHealth(data.health());
            if (player != null) {hamster.setOwnerUuid(player.getUuid());}
            hamster.setTamed(true, true);
            hamster.setBreedingAge(data.breedingAge());
            hamster.throwCooldownEndTick = data.throwCooldownEndTick();
            hamster.setAutoEatCooldownTicks(data.autoEatCooldownTicks());
            hamster.getDataTracker().set(HamsterEntity.PINK_PETAL_TYPE, data.pinkPetalType());
            hamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, data.animationPersonalityId());
            hamster.getDataTracker().set(HamsterEntity.HAMSTER_FLAGS, data.hamsterFlags());
            hamster.totalAgeTicks = data.totalAgeTicks();
            hamster.timesBred = data.timesBred();

            // Explicitly clear the sitting flag to ensure the hamster always dismounts standing.
            hamster.setHamsterFlag(HamsterEntity.SITTING_FLAG, false);

            // --- 2. Load Custom Name ---
            data.customName().ifPresent(name -> {
                if (!name.isEmpty()) {
                    hamster.setCustomName(Text.literal(name));
                }
            });

            // --- 3. Load Inventory ---
            RegistryWrapper.WrapperLookup registries = world.getRegistryManager();
            if (!data.inventoryNbt().isEmpty()) {
                Inventories.readNbt(data.inventoryNbt(), hamster.getItems(), registries);
                HamsterInventoryUtil.updateCheekStates(hamster);
                HamsterInventoryUtil.syncEquipmentTrackers(hamster);
            }

            // --- 4. Load Green Bean Buff Data/Status Effects ---
            HamsterState.GreenBeanBuffData buffData = data.greenBeanBuffData();
            hamster.setGreenBeanBuffEndTick(buffData.greenBeanBuffEndTick());
            hamster.getDataTracker().set(HamsterEntity.GREEN_BEAN_BUFF_DURATION, buffData.greenBeanBuffDuration());
            NbtCompound effectsNbt = buffData.activeEffectsNbt();
            if (effectsNbt.contains("active_effects", NbtElement.LIST_TYPE)) {
                NbtList effectsList = effectsNbt.getList("active_effects", NbtElement.COMPOUND_TYPE);
                for (NbtElement effectElement : effectsList) {
                    if (effectElement instanceof NbtCompound effectInstanceNbt) {
                        StatusEffectInstance effectInstance = StatusEffectInstance.fromNbt(effectInstanceNbt);
                        if (effectInstance != null) {
                            hamster.addStatusEffect(effectInstance);
                        }
                    }
                }
            }

            // --- 5. Load Diamond Seeking Data ---
            HamsterState.SeekingBehaviorData seekingData = data.seekingBehaviorData();
            hamster.isPrimedToSeekDiamonds = seekingData.isPrimedToSeekDiamonds();
            hamster.foundOreCooldownEndTick = seekingData.foundOreCooldownEndTick();
            hamster.currentOreTarget = seekingData.currentOreTarget().orElse(null);

            // --- 6. Load Wander Mode/Bed Data ---
            HamsterState.WanderModeData wanderData = data.wanderModeData();
            hamster.setLinkedBedPos(wanderData.linkedBedPos());
            hamster.setBypassNextSleepDelay(wanderData.bypassNextSleepDelay());

            // --- 7. Reset Transient Action Flags ---
            hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
            hamster.setDozingPhase(HamsterEntity.DozingPhase.NONE);

            // --- 8. Reconcile Accessory State ---
            hamster.updateAccessoryState();
        }
        return hamster;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                               Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static boolean hasInventoryData(NbtCompound nbt) {
        return nbt.contains("Inventory", NbtElement.COMPOUND_TYPE);
    }
}