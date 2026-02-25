package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.component.HamsterShoulderData;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
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
        // --- 1. Write Core Data & Flags ---
        nbt.putInt("HamsterVariant", hamster.getVariant());

        // For backward compatibility, write the flags out as individual booleans.
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

        // --- 2. Write Sleep State Data ---
        nbt.putInt("DozingPhase", hamster.getDozingPhase().ordinal());
        nbt.putString("CurrentDeepSleepAnimId", hamster.getDataTracker().get(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID));
        nbt.putInt("QuiescentSitTimer", hamster.getQuiescentSitTimer());
        nbt.putInt("DriftingOffTimer", hamster.getDriftingOffTimer());
        nbt.putInt("SettleSleepCooldown", hamster.getSettleSleepCooldown());

        // --- 3. Write Inventory ---
        RegistryWrapper.WrapperLookup registries = hamster.getWorld().getRegistryManager();
        NbtCompound inventoryWrapperNbt = new NbtCompound();
        Inventories.writeNbt(inventoryWrapperNbt, hamster.getItems(), registries);
        nbt.put("Inventory", inventoryWrapperNbt);

        // --- 4. Write Seeking and Sulking Data ---
        nbt.putBoolean("IsPrimedToSeekDiamonds", hamster.isPrimedToSeekDiamonds);
        nbt.putLong("FoundOreCooldownEndTick", hamster.foundOreCooldownEndTick);
        if (hamster.currentOreTarget != null) {
            nbt.putInt("OreTargetX", hamster.currentOreTarget.getX());
            nbt.putInt("OreTargetY", hamster.currentOreTarget.getY());
            nbt.putInt("OreTargetZ", hamster.currentOreTarget.getZ());
        }
        nbt.putBoolean("IsSulking", hamster.getHamsterFlag(HamsterEntity.SULKING_FLAG));
        nbt.putBoolean("IsCelebratingDiamond", hamster.getHamsterFlag(HamsterEntity.CELEBRATING_DIAMOND_FLAG));

        // --- 5. Write Interaction & Mini-Game Data ---
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

        // --- 7. Write Wander Mode Data ---
        nbt.putBoolean("IsWanderModeActive", hamster.isWanderModeActive());
        hamster.getLinkedBedPos().ifPresent(globalPos ->
                nbt.put("LinkedBedPos", GlobalPos.CODEC.encodeStart(hamster.getWorld().getRegistryManager().getOps(NbtOps.INSTANCE), globalPos).getOrThrow()));
        nbt.putBoolean("BypassNextSleepDelay", hamster.shouldBypassNextSleepDelay());
        nbt.putBoolean("StuckSearchingForBed", hamster.isStuckSearchingForBed());
        nbt.putBoolean("IsRescueSleeping", hamster.isRescueSleeping());

        // --- 8. Write Flight Data ---
        nbt.putBoolean("HasPlayedIncomingSound", hamster.hasPlayedIncomingSound());
    }

    public static void readCustomDataFromNbt(HamsterEntity hamster, NbtCompound nbt) {
        hamster.setLoadingNbt(true); // Suppress sounds

        // --- 1. Read Core Data ---
        hamster.setVariant(nbt.getInt("HamsterVariant"));

        // --- Read individual booleans and set flags for backward compatibility ---
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

        // Personality ID verification for backwards compat
        if (!nbt.contains("AnimationPersonalityId", NbtElement.INT_TYPE)) {
            int personalityId = hamster.getRandom().nextBetween(1, 3);
            hamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, personalityId);
            AdorableHamsterPets.LOGGER.debug("[NBT READ] Hamster ID {}: NBT had no personality, assigned new ID {}", hamster.getId(), personalityId);
        } else {
            hamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, nbt.getInt("AnimationPersonalityId"));
        }

        // --- 2. Read Sleep State Data ---
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

        // --- 3. Read Inventory ---
        hamster.getItems().clear();
        RegistryWrapper.WrapperLookup registries = hamster.getWorld().getRegistryManager();
        if (nbt.contains("Inventory", NbtElement.COMPOUND_TYPE)) {
            Inventories.readNbt(nbt.getCompound("Inventory"), hamster.getItems(), registries);
        }

        // If the NBT from a command or save file doesn't specify wild loot, generate it.
        if (!hasInventoryData(nbt) && !hamster.isTamed()) {
            HamsterInventoryUtil.generateWildLoot(hamster, hamster.getRandom());
        }
        HamsterInventoryUtil.updateCheekStates(hamster);
        HamsterInventoryUtil.syncEquipmentTrackers(hamster);

        // --- 4. Read Seeking Data ---
        hamster.isPrimedToSeekDiamonds = nbt.getBoolean("IsPrimedToSeekDiamonds");
        hamster.foundOreCooldownEndTick = nbt.getLong("FoundOreCooldownEndTick");
        if (nbt.contains("OreTargetX") && nbt.contains("OreTargetY") && nbt.contains("OreTargetZ")) {
            hamster.currentOreTarget = new BlockPos(nbt.getInt("OreTargetX"), nbt.getInt("OreTargetY"), nbt.getInt("OreTargetZ"));
        } else {
            hamster.currentOreTarget = null;
        }

        // --- 5. Read Interaction & Mini-Game Data ---
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

        // --- 7. Read Wander Mode Data ---
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

        // --- 8. Read Flight Data ---
        hamster.setHasPlayedIncomingSound(nbt.getBoolean("HasPlayedIncomingSound"));

        hamster.setLoadingNbt(false);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                       Shoulder Data Handlers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Captures the current state of this hamster into a {@link HamsterShoulderData} record.
     */
    public static HamsterShoulderData saveToShoulderData(HamsterEntity hamster) {
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
        HamsterShoulderData.SeekingBehaviorData seekingData = new HamsterShoulderData.SeekingBehaviorData(
                hamster.isPrimedToSeekDiamonds,
                hamster.foundOreCooldownEndTick,
                Optional.ofNullable(hamster.currentOreTarget)
        );
        HamsterShoulderData.GreenBeanBuffData buffData = new HamsterShoulderData.GreenBeanBuffData(
                hamster.getGreenBeanBuffEndTick(),
                hamster.getDataTracker().get(HamsterEntity.GREEN_BEAN_BUFF_DURATION),
                effectsNbt
        );
        HamsterShoulderData.WanderModeData wanderData = new HamsterShoulderData.WanderModeData(
                hamster.getLinkedBedPos(),
                hamster.shouldBypassNextSleepDelay()
        );

        // --- 5. Create and Return the Main Data Record ---
        return new HamsterShoulderData(
                hamster.getUuid(),
                hamster.getVariant(),
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
                hamster.getDataTracker().get(HamsterEntity.HAMSTER_FLAGS)
        );
    }

    /**
     * Creates a HamsterEntity instance from NBT data, typically from a player's shoulder.
     * Loads the hamster's variant, health, age, inventory, effects, and custom name.
     * Does NOT set the entity's position or spawn it in the world.
     */
    @Nullable
    public static HamsterEntity createFromNbt(ServerWorld world, PlayerEntity player, NbtCompound nbt) {
        Optional<HamsterShoulderData> dataOpt = HamsterShoulderData.fromNbt(nbt);
        if (dataOpt.isEmpty()) {
            AdorableHamsterPets.LOGGER.error("Failed to deserialize HamsterShoulderData from NBT: {}", nbt);
            return null;
        }
        HamsterShoulderData data = dataOpt.get();

        AdorableHamsterPets.LOGGER.debug("[HamsterNbtUtil] createFromNbt called for player {} with data: {}", player.getName().getString(), data);
        HamsterEntity hamster = ModEntities.HAMSTER.get().create(world);

        if (hamster != null) {
            // --- 1. Load Core Data ---
            hamster.setUuid(data.entityUuid());
            hamster.setVariant(data.variantId());
            hamster.setHealth(data.health());
            hamster.setOwnerUuid(player.getUuid());
            hamster.setTamed(true, true);
            hamster.setBreedingAge(data.breedingAge());
            hamster.throwCooldownEndTick = data.throwCooldownEndTick();
            hamster.setAutoEatCooldownTicks(data.autoEatCooldownTicks());
            hamster.getDataTracker().set(HamsterEntity.PINK_PETAL_TYPE, data.pinkPetalType());
            hamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, data.animationPersonalityId());
            hamster.getDataTracker().set(HamsterEntity.HAMSTER_FLAGS, data.hamsterFlags());

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
            HamsterShoulderData.GreenBeanBuffData buffData = data.greenBeanBuffData();
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
            HamsterShoulderData.SeekingBehaviorData seekingData = data.seekingBehaviorData();
            hamster.isPrimedToSeekDiamonds = seekingData.isPrimedToSeekDiamonds();
            hamster.foundOreCooldownEndTick = seekingData.foundOreCooldownEndTick();
            hamster.currentOreTarget = seekingData.currentOreTarget().orElse(null);

            // --- 6. Load Wander Mode/Bed Data ---
            HamsterShoulderData.WanderModeData wanderData = data.wanderModeData();
            hamster.setLinkedBedPos(wanderData.linkedBedPos());
            hamster.setBypassNextSleepDelay(wanderData.bypassNextSleepDelay());

            // --- 7. Reset Transient Action Flags ---
            hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
            hamster.setDozingPhase(HamsterEntity.DozingPhase.NONE);
        }
        return hamster;
    }

    // --- Private Helpers ---
    private static boolean hasInventoryData(NbtCompound nbt) {
        return nbt.contains("Inventory", NbtElement.COMPOUND_TYPE);
    }
}