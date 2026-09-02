package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    public static void writeCustomDataToNbt(HamsterEntity hamster, CompoundTag nbt) {
        // --- 1. Core Data & Flags ---
        nbt.put("HamsterGenome", hamster.getGenome().saveToNbt());
        nbt.putLong("TotalAgeTicks", hamster.totalAgeTicks);
        nbt.putInt("TimesBred", hamster.timesBred);
        // Write flags as individual booleans for backwards compat
        if (hamster.isTame()) {
            nbt.putBoolean("Sitting", hamster.getHamsterFlag(HamsterEntity.SITTING_FLAG));
            nbt.putBoolean("IsSleeping", hamster.getHamsterFlag(HamsterEntity.SLEEPING_FLAG));
        } else {
            nbt.putBoolean("IsSleeping", false);
        }
        nbt.putBoolean("KnockedOut", hamster.getHamsterFlag(HamsterEntity.KNOCKED_OUT_FLAG));
        nbt.putBoolean("CheekPouchUnlocked", hamster.getHamsterFlag(HamsterEntity.CHEEK_POUCH_UNLOCKED_FLAG));
        nbt.putLong("ThrowCooldownEnd", hamster.throwCooldownEndTick);
        nbt.putLong("GreenBeanBuffDuration", hamster.getEntityData().get(HamsterEntity.GREEN_BEAN_BUFF_DURATION));
        nbt.putInt("AutoEatCooldown", hamster.getAutoEatCooldownTicks());
        nbt.putInt("EjectionCheckCooldown", hamster.getEjectionCheckCooldown());
        nbt.putInt("FlowerPosition", hamster.getEntityData().get(HamsterEntity.FLOWER_POS));
        nbt.putInt("AnimationPersonalityId", hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID));
        nbt.putBoolean("ArmorVisible", hamster.isArmorVisible());
        nbt.putBoolean("isGeneticsVisualizerMember", hamster.isGeneticsVisualizerMember());
        nbt.putInt("AggressionState", hamster.getAggressionState().ordinal());
        hamster.getRedstoneFeverState().writeNbt(nbt);

        // --- 2. Parent Following ---
        if (hamster.getParentUuid() != null) {
            nbt.putUUID("ParentUuid", hamster.getParentUuid());
        }

        // --- 3. Sleep State ---
        nbt.putInt("DozingPhase", hamster.getDozingPhase().ordinal());
        nbt.putString("CurrentDeepSleepAnimId", hamster.getEntityData().get(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID));
        nbt.putInt("QuiescentSitTimer", hamster.getQuiescentSitTimer());
        nbt.putInt("DriftingOffTimer", hamster.getDriftingOffTimer());
        nbt.putInt("SettleSleepCooldown", hamster.getSettleSleepCooldown());

        // --- 4. Inventory ---
        HolderLookup.Provider registries = hamster.level().registryAccess();
        CompoundTag inventoryWrapperNbt = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryWrapperNbt, hamster.getItems(), registries);
        nbt.put("Inventory", inventoryWrapperNbt);

        // --- 5. Ore Seeking ---
        nbt.putBoolean("IsPrimedToSeekDiamonds", hamster.isPrimedToSeekDiamonds);
        nbt.putLong("FoundOreCooldownEndTick", hamster.foundOreCooldownEndTick);
        if (hamster.currentOreTarget != null) {
            nbt.putInt("OreTargetX", hamster.currentOreTarget.getX());
            nbt.putInt("OreTargetY", hamster.currentOreTarget.getY());
            nbt.putInt("OreTargetZ", hamster.currentOreTarget.getZ());
        }
        nbt.putBoolean("IsCelebratingDiamond", hamster.getHamsterFlag(HamsterEntity.CELEBRATING_DIAMOND_FLAG));

        // --- 6. Interaction & Mini-Game ---
        nbt.putBoolean("IsSulking", hamster.getHamsterFlag(HamsterEntity.SULKING_FLAG));
        nbt.putInt("SulkTimer", hamster.sulkTimer);
        nbt.putLong("TagGameCooldownEnd", hamster.tagGameCooldownEndTick);
        nbt.putLong("StealingCooldownEnd", hamster.stealingCooldownEndTick);
        nbt.putLong("CropSnackCooldownEnd", hamster.cropSnackCooldownEndTick);
        nbt.putLong("HideAndSeekCooldownEnd", hamster.hideAndSeekCooldownEndTick);
        if (hamster.getGenericInteractionTimer() > 0) {
            nbt.putInt("GenericInteractionTimer", hamster.getGenericInteractionTimer());
        }
        if (hamster.isHoldingMouthItem()) {
            nbt.putBoolean("IsHoldingMouthItem", true);
            if (!hamster.getMouthItemStack().isEmpty()) {
                nbt.put("MouthItemStack", hamster.getMouthItemStack().save(registries));
            }
        }

        // --- 7. Wander Mode ---
        nbt.putBoolean("IsWanderModeActive", hamster.isWanderModeActive());
        hamster.getLinkedBedPos().ifPresent(globalPos ->
                nbt.put("LinkedBedPos", GlobalPos.CODEC.encodeStart(hamster.level().registryAccess().createSerializationContext(NbtOps.INSTANCE), globalPos).getOrThrow()));
        nbt.putBoolean("BypassNextSleepDelay", hamster.shouldBypassNextSleepDelay());
        nbt.putBoolean("StuckSearchingForBed", hamster.isStuckSearchingForBed());
        nbt.putBoolean("IsRescueSleeping", hamster.isRescueSleeping());
    }

    public static void readCustomDataFromNbt(HamsterEntity hamster, CompoundTag nbt) {
        // --- 1. Read Core Data ---
        hamster.setLoadingNbt(true); // Suppress sounds
        hamster.totalAgeTicks = nbt.getLong("TotalAgeTicks");
        hamster.timesBred = nbt.getInt("TimesBred");
        // Migrate legacy variant IDs to v3.6.0's Genome structure
        if (nbt.contains("HamsterGenome", Tag.TAG_COMPOUND)) {
            hamster.setGenome(HamsterGenome.readFromNbt(nbt.getCompound("HamsterGenome")));
        } else if (nbt.contains("HamsterVariant", Tag.TAG_INT)) {
            // Catch old integer IDs from pre 3.6.0
            int legacyId = nbt.getInt("HamsterVariant");
            hamster.setGenome(HamsterGeneticsUtil.getGenomeForLegacyId(legacyId));
        } else {
            hamster.setGenome(HamsterGenome.createDefault());
        }
        // Backwards compat: read individual booleans & set flags
        boolean wasSittingNbt = hamster.isTame() && nbt.getBoolean("Sitting");
        hamster.setSitting(wasSittingNbt, true); // This will correctly set the SITTING_FLAG
        hamster.setHamsterFlag(HamsterEntity.KNOCKED_OUT_FLAG, nbt.getBoolean("KnockedOut"));
        hamster.setHamsterFlag(HamsterEntity.CHEEK_POUCH_UNLOCKED_FLAG, nbt.getBoolean("CheekPouchUnlocked"));
        hamster.setHamsterFlag(HamsterEntity.SULKING_FLAG, nbt.getBoolean("IsSulking"));
        if (nbt.contains("SulkTimer", Tag.TAG_INT)) {
            hamster.sulkTimer = nbt.getInt("SulkTimer");
        } else if (hamster.isSulking()) {
            // Backwards compat: if older save has them sulking, assign timer
            hamster.sulkTimer = 160 + hamster.getRandom().nextInt(80);
        }

        hamster.setHamsterFlag(HamsterEntity.CELEBRATING_DIAMOND_FLAG, nbt.getBoolean("IsCelebratingDiamond"));
        boolean loadedSleeping = nbt.getBoolean("IsSleeping");
        if (!hamster.isTame()) {
            loadedSleeping = false;
        }
        hamster.setHamsterFlag(HamsterEntity.SLEEPING_FLAG, loadedSleeping);
        hamster.throwCooldownEndTick = nbt.getLong("ThrowCooldownEnd");
        hamster.setHamsterFlag(HamsterEntity.THROW_COOLDOWN_FLAG, hamster.throwCooldownEndTick > hamster.level().getGameTime());
        hamster.getEntityData().set(HamsterEntity.GREEN_BEAN_BUFF_DURATION, nbt.getLong("GreenBeanBuffDuration"));
        hamster.setAutoEatCooldownTicks(nbt.getInt("AutoEatCooldown"));
        hamster.setEjectionCheckCooldown(nbt.contains("EjectionCheckCooldown", Tag.TAG_INT) ? nbt.getInt("EjectionCheckCooldown") : 20);
        // Backwards compat for old Pink Petals
        if (nbt.contains("FlowerPosition", Tag.TAG_INT)) {
            hamster.getEntityData().set(HamsterEntity.FLOWER_POS, nbt.getInt("FlowerPosition"));
        } else if (nbt.contains("PinkPetalType", Tag.TAG_INT)) {
            hamster.getEntityData().set(HamsterEntity.FLOWER_POS, nbt.getInt("PinkPetalType"));
        }
        // Backwards compat: personality ID verification
        if (!nbt.contains("AnimationPersonalityId", Tag.TAG_INT)) {
            int personalityId = hamster.getRandom().nextIntBetweenInclusive(1, 3);
            hamster.getEntityData().set(HamsterEntity.ANIMATION_PERSONALITY_ID, personalityId);
            AdorableHamsterPets.LOGGER.debug("[NBT READ] Hamster ID {}: NBT had no personality, assigned new ID {}", hamster.getId(), personalityId);
        } else {
            hamster.getEntityData().set(HamsterEntity.ANIMATION_PERSONALITY_ID, nbt.getInt("AnimationPersonalityId"));
        }
        hamster.setArmorVisible(!nbt.contains("ArmorVisible", Tag.TAG_BYTE) || nbt.getBoolean("ArmorVisible"));
        hamster.setGeneticsVisualizerMember(nbt.getBoolean("isGeneticsVisualizerMember"));
        if (nbt.contains("AggressionState", Tag.TAG_INT)) {
            int stateOrdinal = nbt.getInt("AggressionState");
            if (stateOrdinal >= 0 && stateOrdinal < HamsterEntity.AggressionState.values().length) {
                hamster.setAggressionState(HamsterEntity.AggressionState.values()[stateOrdinal]);
            }
        }
        hamster.getRedstoneFeverState().readNbt(nbt);
        RedstoneFeverUtil.normalizeDisabledState(hamster);
        hamster.synchronizeRedstoneFeverVisualState();

        // --- 2. Parent Following ---
        if (nbt.hasUUID("ParentUuid")) {
            hamster.setParentUuid(nbt.getUUID("ParentUuid"));
        }

        // --- 3. Sleep State ---
        if (nbt.contains("DozingPhase", Tag.TAG_INT)) {
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
        hamster.getEntityData().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, nbt.getString("CurrentDeepSleepAnimId"));
        hamster.setQuiescentSitTimer(nbt.getInt("QuiescentSitTimer"));
        hamster.setDriftingOffTimer(nbt.getInt("DriftingOffTimer"));
        hamster.setSettleSleepCooldown(nbt.getInt("SettleSleepCooldown"));

        // --- 4. Inventory ---
        HolderLookup.Provider registries = hamster.level().registryAccess();
        if (nbt.contains("Inventory", Tag.TAG_COMPOUND)) {
            hamster.getItems().clear();
            ContainerHelper.loadAllItems(nbt.getCompound("Inventory"), hamster.getItems(), registries);
            HamsterInventoryUtil.updateCheekStates(hamster);
            HamsterInventoryUtil.syncEquipmentTrackers(hamster);
        } else if (!hamster.level().isClientSide() && !hamster.isTame()) {
            HamsterInventoryUtil.generateWildLoot(hamster, hamster.getRandom());
            HamsterInventoryUtil.updateCheekStates(hamster);
            HamsterInventoryUtil.syncEquipmentTrackers(hamster);
        }

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
        hamster.cropSnackCooldownEndTick = nbt.getLong("CropSnackCooldownEnd");
        hamster.hideAndSeekCooldownEndTick = nbt.getLong("HideAndSeekCooldownEnd");
        hamster.setGenericInteractionTimer(nbt.getInt("GenericInteractionTimer"));

        boolean holding = nbt.getBoolean("IsHoldingMouthItem");
        hamster.setHoldingMouthItem(holding);

        if (holding) {
            if (nbt.contains("MouthItemStack", Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, nbt.getCompound("MouthItemStack")).ifPresent(hamster::setMouthItemStack);
            }
        } else {
            hamster.setMouthItemStack(ItemStack.EMPTY);
        }

        // --- 7. Wander Mode ---
        hamster.setWanderModeActive(nbt.getBoolean("IsWanderModeActive"));
        if (nbt.contains("LinkedBedPos")) {
            hamster.setLinkedBedPos(GlobalPos.CODEC.parse(hamster.level().registryAccess().createSerializationContext(NbtOps.INSTANCE), nbt.get("LinkedBedPos")).result());
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
    public static CompoundTag setKnockedOutInNbt(CompoundTag originalNbt) {
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
        CompoundTag inventoryNbt = new CompoundTag();
        if (hamster.level() instanceof ServerLevel serverWorld) {
            ContainerHelper.saveAllItems(inventoryNbt, hamster.getItems(), serverWorld.registryAccess());
        }

        // --- 2. Save Active Status Effects to NBT ---
        CompoundTag effectsNbt = new CompoundTag();
        if (!hamster.getActiveEffects().isEmpty()) {
            ListTag effectsList = new ListTag();
            for (MobEffectInstance effectInstance : hamster.getActiveEffects()) {
                effectsList.add(effectInstance.save());
            }
            effectsNbt.put("active_effects", effectsList);
        }

        // --- 3. Get Custom Name ---
        Optional<String> nameOptional = Optional.ofNullable(hamster.getCustomName()).map(Component::getString);

        // --- 4. Create Domain Transfer Records ---
        HamsterState.MiniGameBehaviorData seekingData = new HamsterState.MiniGameBehaviorData(
                hamster.isPrimedToSeekDiamonds,
                hamster.foundOreCooldownEndTick,
                hamster.cropSnackCooldownEndTick,
                hamster.hideAndSeekCooldownEndTick,
                Optional.ofNullable(hamster.currentOreTarget)
        );
        HamsterState.GreenBeanBuffData buffData = new HamsterState.GreenBeanBuffData(
                hamster.getGreenBeanBuffEndTick(),
                hamster.getEntityData().get(HamsterEntity.GREEN_BEAN_BUFF_DURATION),
                effectsNbt
        );
        HamsterState.WanderModeData wanderData = new HamsterState.WanderModeData(
                hamster.getLinkedBedPos(),
                hamster.shouldBypassNextSleepDelay()
        );

        HamsterState.IdentityData identityData = new HamsterState.IdentityData(
                hamster.getUUID(),
                hamster.getGenome().saveToNbt(),
                nameOptional
        );
        HamsterState.LifeHistoryData lifeHistoryData = new HamsterState.LifeHistoryData(
                hamster.getAge(),
                hamster.totalAgeTicks,
                hamster.timesBred
        );
        HamsterState.StatusData statusData = new HamsterState.StatusData(
                hamster.throwCooldownEndTick,
                buffData,
                hamster.getAutoEatCooldownTicks()
        );
        HamsterState.AppearanceData appearanceData = new HamsterState.AppearanceData(
                hamster.getEntityData().get(HamsterEntity.FLOWER_POS),
                hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID),
                hamster.isArmorVisible()
        );
        HamsterState.BehaviorData behaviorData = new HamsterState.BehaviorData(
                seekingData,
                wanderData,
                hamster.getEntityData().get(HamsterEntity.HAMSTER_FLAGS)
        );
        HamsterState.HamsterConditionData conditionData =
                HamsterState.HamsterConditionData.capture(hamster.getRedstoneFeverState());

        // --- 5. Create and Return Main Data Record ---
        return new HamsterState(
                identityData,
                hamster.getHealth(),
                inventoryNbt,
                lifeHistoryData,
                statusData,
                appearanceData,
                behaviorData,
                conditionData
        );
    }

    /**
     * Creates a HamsterEntity instance from NBT data, typically from a player's shoulder.
     * Loads the hamster's variant, health, age, inventory, effects, and custom name.
     * Does NOT set the entity's position or spawn it in the world.
     */
    @Nullable
    public static HamsterEntity createFromNbt(ServerLevel world, @Nullable Player player, CompoundTag nbt) {
        Optional<HamsterState> dataOpt = HamsterState.fromNbt(nbt);
        if (dataOpt.isEmpty()) {
            AdorableHamsterPets.LOGGER.error("Failed to deserialize HamsterState from NBT: {}", nbt);
            return null;
        }
        HamsterState data = dataOpt.get();

        HamsterEntity hamster = ModEntities.HAMSTER.get().create(world);

        if (hamster != null) {
            // --- 1. Load Core Data ---
            hamster.setUUID(data.entityUuid());
            hamster.setGenome(HamsterGenome.readFromNbt(data.genomeNbt()));
            hamster.setHealth(data.health());
            if (player != null) {hamster.setOwnerUUID(player.getUUID());}
            data.conditionData().applyTo(hamster.getRedstoneFeverState());
            RedstoneFeverUtil.normalizeDisabledState(hamster);
            hamster.synchronizeRedstoneFeverVisualState();
            hamster.setTame(true, true);
            hamster.setAge(data.breedingAge());
            hamster.throwCooldownEndTick = data.throwCooldownEndTick();
            hamster.setAutoEatCooldownTicks(data.autoEatCooldownTicks());
            hamster.getEntityData().set(HamsterEntity.FLOWER_POS, data.flowerPosition());
            hamster.getEntityData().set(HamsterEntity.ANIMATION_PERSONALITY_ID, data.animationPersonalityId());
            hamster.getEntityData().set(HamsterEntity.HAMSTER_FLAGS, data.hamsterFlags());
            hamster.setArmorVisible(data.armorVisible());
            hamster.totalAgeTicks = data.totalAgeTicks();
            hamster.timesBred = data.timesBred();

            // Sync vanilla sitting pose with restored flag
            hamster.setInSittingPose(hamster.getHamsterFlag(HamsterEntity.SITTING_FLAG));

            // --- 2. Load Custom Name ---
            data.customName().ifPresent(name -> {
                if (!name.isEmpty()) {
                    hamster.setCustomName(Component.literal(name));
                }
            });

            // --- 3. Load Inventory ---
            HolderLookup.Provider registries = world.registryAccess();
            if (!data.inventoryNbt().isEmpty()) {
                ContainerHelper.loadAllItems(data.inventoryNbt(), hamster.getItems(), registries);
                HamsterInventoryUtil.updateCheekStates(hamster);
                HamsterInventoryUtil.syncEquipmentTrackers(hamster);
            }

            // --- 4. Load Green Bean Buff Data/Status Effects ---
            HamsterState.GreenBeanBuffData buffData = data.greenBeanBuffData();
            hamster.setGreenBeanBuffEndTick(buffData.greenBeanBuffEndTick());
            hamster.getEntityData().set(HamsterEntity.GREEN_BEAN_BUFF_DURATION, buffData.greenBeanBuffDuration());
            CompoundTag effectsNbt = buffData.activeEffectsNbt();
            if (effectsNbt.contains("active_effects", Tag.TAG_LIST)) {
                ListTag effectsList = effectsNbt.getList("active_effects", Tag.TAG_COMPOUND);
                for (Tag effectElement : effectsList) {
                    if (effectElement instanceof CompoundTag effectInstanceNbt) {
                        MobEffectInstance effectInstance = MobEffectInstance.load(effectInstanceNbt);
                        if (effectInstance != null) {
                            hamster.addEffect(effectInstance);
                        }
                    }
                }
            }

            // --- 5. Load Diamond Seeking Data ---
            HamsterState.MiniGameBehaviorData seekingData = data.seekingBehaviorData();
            hamster.isPrimedToSeekDiamonds = seekingData.isPrimedToSeekDiamonds();
            hamster.foundOreCooldownEndTick = seekingData.foundOreCooldownEndTick();
            hamster.cropSnackCooldownEndTick = seekingData.cropSnackCooldownEndTick();
            hamster.hideAndSeekCooldownEndTick = seekingData.hideAndSeekCooldownEndTick();
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

    private static boolean hasInventoryData(CompoundTag nbt) {
        return nbt.contains("Inventory", Tag.TAG_COMPOUND);
    }
}
