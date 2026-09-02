package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.util.FlowerItemUtil;
import net.minecraft.core.UUIDUtil;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.AI.HamsterTagGoal;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.dawson.adorablehamsterpets.networking.payload.PlayShoulderMountSoundPayload;
import net.dawson.adorablehamsterpets.screen.HamsterScreenHandlerFactory;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Encapsulates the interaction logic for HamsterEntity.
 * Each method acts as a step in a processing pipeline. If a method handles the interaction,
 * it returns a consuming ActionResult. Otherwise, it returns PASS to continue down the chain.
 */
public final class HamsterInteractionUtil {

    private HamsterInteractionUtil() {
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                          Global Interactions
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Debug Toggle ---
    public static InteractionResult handleDebugToggle(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (player.isShiftKeyDown() && stack.is(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            if (hamster.level().isClientSide()) {
                AhpUiConfig currentConfig = AdorableHamsterPets.UI_CONFIG;
                boolean newSetting = !currentConfig.enableJadeHamsterDebugInfo;

                currentConfig.enableJadeHamsterDebugInfo = newSetting;
                currentConfig.save();

                Component message = Component.translatable(
                        newSetting ? "message.adorablehamsterpets.debug_overlay_enabled" : "message.adorablehamsterpets.debug_overlay_disabled"
                ).withStyle(newSetting ? ChatFormatting.WHITE : ChatFormatting.RED);
                player.sendOverlayMessage(message);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- Genetics Visualizer ---
    public static InteractionResult handleGeneticsVisualizer(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (!player.isShiftKeyDown() && stack.is(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            if (hamster.isGeneticsVisualizerMember()) {
                if (!hamster.level().isClientSide()) {
                    PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
                    UUID p1 = accessor.ahp$getGeneticParent1Uuid();
                    UUID p2 = accessor.ahp$getGeneticParent2Uuid();
                    UUID target = hamster.getUUID();

                    if (target.equals(p1) || target.equals(p2)) {
                        // Clicking an already selected parent clears visualization
                        accessor.ahp$setGeneticParent1Uuid(null);
                        accessor.ahp$setGeneticParent2Uuid(null);
                        player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.breeding.genetics_visualization.clear").withStyle(ChatFormatting.YELLOW));
                    } else if (p1 == null || (p1 != null && p2 != null)) {
                        // Start a new selection
                        accessor.ahp$setGeneticParent1Uuid(target);
                        accessor.ahp$setGeneticParent2Uuid(null);
                        player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.breeding.genetics_visualization.set_parent1").withStyle(ChatFormatting.WHITE));
                    } else {
                        // Set second parent
                        accessor.ahp$setGeneticParent2Uuid(target);
                        player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.breeding.genetics_visualization.set_parent2").withStyle(ChatFormatting.WHITE));
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    // --- Tag Game ---
    public static InteractionResult handleTagGame(HamsterEntity hamster, Player player, InteractionHand hand) {
        if (hamster.isPlayingTag()) {
            // Intercept Hamster-vs-Hamster Tag
            if (hamster.isInterHamsterTagActive) {
                if (!hamster.level().isClientSide()) {
                    // Cancel game for the clicked hamster
                    hamster.setPlayingTag(false);
                    hamster.isInterHamsterTagActive = false;
                    hamster.getNavigation().stop();

                    // Cancel game for the partner
                    if (hamster.tagGamePartner != null) {
                        hamster.tagGamePartner.setPlayingTag(false);
                        hamster.tagGamePartner.isInterHamsterTagActive = false;
                        hamster.tagGamePartner.getNavigation().stop();
                        hamster.tagGamePartner.tagGamePartner = null;
                    }
                    hamster.tagGamePartner = null;

                    // Feedback
                    player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.inter_hamster_tag_interrupted").withStyle(ChatFormatting.WHITE));
                }
                return InteractionResult.SUCCESS;
            }

            // Standard Player-vs-Hamster Tag
            if (hamster.isOwnedBy(player) || AdorableHamsterPets.MAIN_CONFIG.allowStrangerTag) {
                if (!hamster.level().isClientSide()) {
                    // 1. Stop Goal & Clear State
                    hamster.setPlayingTag(false);
                    hamster.setTaunting(false);
                    hamster.getNavigation().stop();

                    // Clear debug name
                    if (hamster.getActiveCustomGoalName().equals(HamsterTagGoal.class.getSimpleName())) {
                        hamster.setActiveCustomGoalName("None");
                    }

                    // 2. Set Cooldowns
                    // Hamster cooldown
                    hamster.tagGameCooldownEndTick = hamster.level().getGameTime() + Configs.AHP_MAIN.hamsterVersusPlayerTagCooldown.get();
                    // Player daily limit increment
                    if (player instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$incrementTagGameCount();
                    }

                    // 3. Start Celebration Phase
                    // Store the player who interacted as the rotation target
                    hamster.setCelebrationTarget(player);
                    HamsterMovementUtil.faceEntity(hamster, player);

                    // Lock rotation to target (Owner or Stranger) for the duration of both animations
                    hamster.setFrozenMovement(true);
                    hamster.setCelebrationTicks(80);
                    hamster.interactionCooldown = 80;

                    // Visuals & Audio
                    hamster.level().playSound(null, hamster.blockPosition(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, hamster.getRandom()), SoundSource.NEUTRAL, 1.0f, 1.0f);
                    ParticleEffectsUtil.spawnParticles(
                            hamster.level(),
                            new Vec3(hamster.getX(), hamster.getY(0.8), hamster.getZ()),
                            ParticleTypes.HEART,
                            3,
                            new Vec3(0.3, 0.2, 0.3),
                            0.2
                    );

                    // Trigger Celebration Animation
                    hamster.triggerAnimOnServer("mainController", "anim_hamster_quick_bounce");

                    // 4. Schedule Gifting Sequence
                    long baseTime = hamster.level().getGameTime();

                    hamster.scheduleTask(baseTime + 32, "start_gift_sequence", () -> {
                        Item giftItem = MinigameUtil.getRandomMiniGameReward(hamster);
                        if (giftItem != Items.AIR) {
                            MinigameUtil.executeGiftDeliverySequence(hamster, new ItemStack(giftItem), player);
                        }
                    });
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    // --- Taming ---
    public static InteractionResult handleTaming(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (!hamster.isTame()) {
            boolean isTamingFood = HamsterLureUtil.isTamingItem(stack);
            boolean isSneaking = player.isShiftKeyDown();

            // --- 1. Normal Taming Path ---
            if (isSneaking && isTamingFood) {
                // Block taming if it is an ai-disabled statue and config forbids it
                if (hamster.isNoAi() && !AdorableHamsterPets.MAIN_CONFIG.allowTamingAiDisabled) {
                    if (!hamster.level().isClientSide()) {
                        player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.taming_statue_refusal").withStyle(ChatFormatting.RED));
                    }
                    return InteractionResult.SUCCESS;
                }

                if (!hamster.level().isClientSide()) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }

                    // Use config value for taming chance
                    final AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;
                    int denominator = Math.max(1, config.tamingChanceDenominator.get()); // Ensure denominator is at least 1
                    if (hamster.getRandom().nextInt(denominator) == 0) {
                        hamster.setOwnerReference((player.getUUID()) == null ? null : net.minecraft.world.entity.EntityReference.of(player.getUUID()));
                        hamster.setTame(true, true);
                        hamster.getNavigation().stop();
                        hamster.setSitting(false, true);
                        hamster.setSleeping(false);
                        hamster.setTarget(null);
                        hamster.level().broadcastEntityEvent(hamster, (byte) 7);

                        // Re-awaken if AI was disabled and reset statue physics
                        if (hamster.isNoAi()) {
                            hamster.setNoAi(false);
                            hamster.setNoGravity(false);
                            hamster.setInvulnerable(false);
                        }

                        // Play celebrate sound only on success
                        SoundEvent celebrateSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, hamster.getRandom());
                        hamster.level().playSound(null, hamster.blockPosition(), celebrateSound, SoundSource.NEUTRAL, 0.7F, 1.0F);

                        if (player instanceof ServerPlayer serverPlayer) {
                            CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, hamster);
                            HamsterGeneticsAdvancementUtil.trackTamedHamster(serverPlayer, hamster);
                        }

                        // Baby link warning
                        if (Configs.AHP_UI.enableTamedBabyWarningMessage && hamster.isBaby() && hamster.getParentUuid() != null) {
                            Component lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.lureItems).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                            player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.tamed_baby_still_linked_warning", lureName).withStyle(ChatFormatting.WHITE));
                        }
                    } else {
                        hamster.level().broadcastEntityEvent(hamster, (byte) 6);
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // --- 2. Failure Feedback Path ---
            if (!hamster.level().isClientSide() && hand == InteractionHand.MAIN_HAND && hamster.interactionCooldown <= 0) {
                boolean isAnyFood = stack.has(DataComponents.FOOD) || ConfigDataCache.isDietaryItem(stack);
                boolean isFailure = false;
                String messageKey = null;

                if (isTamingFood && !isSneaking) {
                    messageKey = "message.adorablehamsterpets.taming_failure_sneaking";
                    isFailure = true;
                } else if (!isTamingFood && isAnyFood) {
                    messageKey = "message.adorablehamsterpets.taming_failure_food";
                    isFailure = true;
                }

                if (isFailure) {
                    hamster.interactionCooldown = 20; // Prevent spam

                    // Audio feedback
                    hamster.level().playSound(null, hamster.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.2f, 0.5f);

                    MutableComponent msg = Component.literal("\n").append(Component.translatable(messageKey).withStyle(ChatFormatting.RED));

                    // If player is also missing guidebook
                    if (!((PlayerEntityAccessor) player).ahp$computeHasGuideBook(player)) {
                        msg.append("\n\n").append(
                                Component.translatable("message.adorablehamsterpets.taming_failure_guidebook_link")
                                        .setStyle(Style.EMPTY
                                                .withColor(ChatFormatting.GREEN)
                                                .withBold(true)
                                                .withUnderlined(true)
                                                .withClickEvent(new ClickEvent.RunCommand("/ahp_open_config_screen"))
                                        )
                        ).append("\n");
                    } else {
                        msg.append("\n");
                    }

                    player.sendSystemMessage(msg);
                    hamster.playRefusalAnimation();
                    return InteractionResult.SUCCESS; // Consume interaction so player doesn't accidentally eat item
                }
            }
        }
        return InteractionResult.PASS;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                           Owner Interactions
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Naming ---
    public static boolean consumeNameTag(Player player, HamsterEntity hamster) {
        // 1. Check player inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.NAME_TAG)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return true;
            }
        }

        // 2. Check hamster cheek pouches (slots 0-5)
        for (int i = 0; i < HamsterInventoryUtil.CHEEK_POUCH_SIZE; i++) {
            ItemStack stack = hamster.getItems().get(i);
            if (stack.is(Items.NAME_TAG)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    hamster.setChanged();
                }
                return true;
            }
        }
        return false;
    }

    // --- Bed Linking ---
    public static InteractionResult handleBedLinking(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (stack.getItem() instanceof HamsterBedItem) {
            if (!hamster.level().isClientSide()) {
                UUID linkedUuid = stack.get(ModDataComponentTypes.LINKED_HAMSTER_UUID.get());
                Component nameToSet = hamster.hasCustomName() ? hamster.getName() : hamster.getDisplayName().copy().append(" " + hamster.getId());

                if (linkedUuid == null || !linkedUuid.equals(hamster.getUUID())) {
                    // Initial link or re-linking to a new hamster
                    ItemStack newStack = stack.copy();
                    newStack.set(ModDataComponentTypes.LINKED_HAMSTER_UUID.get(), hamster.getUUID());
                    newStack.set(ModDataComponentTypes.LINKED_HAMSTER_NAME.get(), nameToSet);
                    newStack.set(ModDataComponentTypes.WANDER_DISTANCE.get(), AdorableHamsterPets.MAIN_CONFIG.defaultWanderDistance.get());

                    player.setItemInHand(hand, newStack);

                    // Feedback
                    hamster.level().playSound(null, hamster.blockPosition(), SoundEvents.BAMBOO_WOOD_PLACE, SoundSource.PLAYERS, 1.0f, 1.2f);
                    ParticleEffectsUtil.spawnParticles(hamster.level(), new Vec3(hamster.getX(), hamster.getY(0.5), hamster.getZ()), ParticleTypes.HAPPY_VILLAGER, 10, new Vec3(0.5, 0.5, 0.5), 0.0);
                    player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.bed_linked", hamster.getName()));

                    if (player instanceof ServerPlayer serverPlayer) {
                        ModCriteria.HAMSTER_BED_LINKED.get().trigger(serverPlayer);
                    }
                } else {
                    // Re-configuring distance of already linked bed
                    WanderDistance currentDistance = stack.getOrDefault(ModDataComponentTypes.WANDER_DISTANCE.get(), AdorableHamsterPets.MAIN_CONFIG.defaultWanderDistance.get());
                    WanderDistance[] values = WanderDistance.values();
                    WanderDistance nextDistance = values[(currentDistance.ordinal() + 1) % values.length];
                    stack.set(ModDataComponentTypes.WANDER_DISTANCE.get(), nextDistance);

                    player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.wander_distance_set", hamster.getName(), nextDistance.getSerializedName()));
                    hamster.level().playSound(null, hamster.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- Armor Equipment ---
    public static InteractionResult handleArmorEquip(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (!player.isShiftKeyDown() && stack.getItem() instanceof HamsterArmorItem) {
            if (!hamster.level().isClientSide()) {
                ItemStack currentArmor = hamster.getArmorStack();
                ItemStack newArmor = stack.split(1);

                hamster.setArmorStack(newArmor);
                hamster.level().playSound(null, hamster.blockPosition(), SoundEvents.ARMOR_EQUIP_WOLF.value(), SoundSource.NEUTRAL, 0.6f, 1.2f);

                if (!currentArmor.isEmpty()) {
                    if (!player.getInventory().add(currentArmor)) {
                        player.drop(currentArmor, false);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- State Restorations ---
    public static InteractionResult handleStateRestoration(HamsterEntity hamster, Player player, InteractionHand hand) {
        Level world = hamster.level();

        if (hamster.isSleeping() || hamster.isKnockedOut() || hamster.isCelebratingDiamond() || hamster.isSulking()) {
            if (!world.isClientSide()) {
                if (hamster.isSleeping()) {
                    HamsterBedUtil.wakeUpFromBed(hamster, true);
                } else if (hamster.isKnockedOut()) {
                    SoundEvent wakeUpSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_WAKE_UP_SOUNDS, hamster.getRandom());
                    if (wakeUpSound != null)
                        world.playSound(null, hamster.blockPosition(), wakeUpSound, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    hamster.setKnockedOut(false);
                    hamster.setSitting(false, true);
                    hamster.triggerAnimOnServer("mainController", "wakeup_from_ko");
                } else if (hamster.isCelebratingDiamond()) {
                    hamster.setCelebratingDiamond(false);
                    hamster.setSitting(false, true);
                    SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
                    world.playSound(null, hamster.blockPosition(), affectionSound != null ? affectionSound : SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, affectionSound != null ? 1.0f : 0.5f, affectionSound != null ? hamster.getVoicePitch() : 1.5f);
                } else if (hamster.isSulking()) {
                    hamster.setSulking(false);
                    hamster.setSitting(false, true);
                    SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
                    world.playSound(null, hamster.blockPosition(), affectionSound != null ? affectionSound : SoundEvents.CHICKEN_STEP.value(), SoundSource.NEUTRAL, affectionSound != null ? 1.0f : 0.5f, affectionSound != null ? hamster.getVoicePitch() : 1.5f);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // --- Mouth Item Return ---
    public static InteractionResult handleMouthItemReturn(HamsterEntity hamster, Player player, InteractionHand hand) {
        if (hamster.isHoldingMouthItem()) {
            if (!hamster.level().isClientSide()) {
                ItemStack retrievedStack = hamster.getMouthItemStack().copy();
                player.getInventory().placeItemBackInInventory(hamster.getMouthItemStack().copy());

                hamster.setMouthItemStack(ItemStack.EMPTY);
                hamster.setGenericInteractionTimer(0);
                hamster.setHoldingMouthItem(false);

                hamster.setFrozenMovement(true);
                hamster.setCelebrationTarget(player);
                hamster.setCelebrationTicks(30);
                hamster.triggerAnimOnServer("mainController", "anim_hamster_quick_bounce");

                hamster.level().playSound(null, hamster.blockPosition(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom()), SoundSource.NEUTRAL, 1.0f, hamster.getVoicePitch());
                if (!retrievedStack.isEmpty()) {
                    SoundEvent pounceSound = ModSounds.getDynamicItemSound(retrievedStack);
                    float volume = ModSounds.getDynamicSoundVolume(pounceSound);
                    hamster.level().playSound(null, hamster.blockPosition(), pounceSound, SoundSource.NEUTRAL, volume, 1.7f);
                    ParticleEffectsUtil.spawnParticles(hamster.level(), new Vec3(hamster.getX(), hamster.getY(0.5), hamster.getZ()), new ItemParticleOption(ParticleTypes.ITEM, retrievedStack.getItem()), 10, new Vec3(0.2, 0.2, 0.2), 0.05);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- Accessory Application ---
    public static InteractionResult handleAccessoryInteraction(
            HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        boolean isFlower = FlowerItemUtil.isFlower(stack);
        if (hamster.canPlaceItem(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX, stack)
                && HamsterInteractionGestureUtil.isAccessoryEquipGesture(
                        player.isShiftKeyDown(), isFlower)) {
            if (!hamster.level().isClientSide()) {
                ItemStack currentAccessory =
                        hamster.getItems().get(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX);

                // If holding flower and hamster already has same flower, cycle position
                if (isFlower
                        && FlowerItemUtil.isFlower(currentAccessory)
                        && ItemStack.isSameItem(stack, currentAccessory)) {
                    int currentPos = hamster.getEntityData().get(HamsterEntity.FLOWER_POS);
                    int nextPos = (currentPos % 3) + 1;
                    hamster.getEntityData().set(HamsterEntity.FLOWER_POS, nextPos);

                    hamster.level()
                            .playSound(
                                    null,
                                    hamster.blockPosition(),
                                    SoundEvents.PINK_PETALS_PLACE,
                                    SoundSource.PLAYERS,
                                    0.7F,
                                    1.0F + hamster.getRandom().nextFloat() * 0.2F);
                    ParticleEffectsUtil.spawnParticles(
                            hamster.level(),
                            new Vec3(
                                    hamster.getX(),
                                    hamster.getY() + hamster.getBbHeight() * 0.75,
                                    hamster.getZ()),
                            ParticleTypes.FALLING_SPORE_BLOSSOM,
                            7,
                            new Vec3(
                                    hamster.getBbWidth() / 2.0,
                                    hamster.getBbHeight() / 2.0,
                                    hamster.getBbWidth() / 2.0),
                            0.0);
                } else {
                    ItemStack toEquip = stack.split(1);
                    ItemStack toReturn = currentAccessory.copy();

                    hamster.setItem(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX, toEquip);

                    if (!toReturn.isEmpty()) {
                        hamster.spawnAtLocation((net.minecraft.server.level.ServerLevel) hamster.level(), toReturn);
                    }

                    hamster.level()
                            .playSound(
                                    null,
                                    hamster.blockPosition(),
                                    SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                                    SoundSource.PLAYERS,
                                    1.0F,
                                    1.0F);
                    ParticleEffectsUtil.spawnParticles(
                            hamster.level(),
                            new Vec3(
                                    hamster.getX(),
                                    hamster.getY() + hamster.getBbHeight() * 0.75,
                                    hamster.getZ()),
                            new ItemParticleOption(ParticleTypes.ITEM, toEquip.getItem()),
                            7,
                            new Vec3(
                                    hamster.getBbWidth() / 2.0,
                                    hamster.getBbHeight() / 2.0,
                                    hamster.getBbWidth() / 2.0),
                            0.0);

                    if (FlowerItemUtil.isFlower(toEquip)
                            && player instanceof ServerPlayer serverPlayer) {
                        ModCriteria.APPLIED_FLOWER.get().trigger(serverPlayer, hamster);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- Shearing ---
    public static InteractionResult handleShearing(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (stack.is(Items.SHEARS) && !player.isShiftKeyDown()) {
            boolean actionTaken = false;
            Level world = hamster.level();

            // Priority: Remove Armor
            ItemStack armorStack = hamster.getArmorStack();
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem) {
                actionTaken = true;
                if (!world.isClientSide()) {
                    hamster.spawnAtLocation((net.minecraft.server.level.ServerLevel) hamster.level(), armorStack);
                    hamster.setSilentInventoryUpdate(true);
                    hamster.setArmorStack(ItemStack.EMPTY);
                    hamster.setSilentInventoryUpdate(false);
                    hamster.playSound(SoundEvents.ARMOR_UNEQUIP_WOLF, 0.8f, 1.5f);
                    if (!player.getAbilities().instabuild) {
                        stack.hurtAndBreak(1, player, (hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND));
                    }
                }
            }

            // Secondary: Remove Accessory
            ItemStack accessoryStack = hamster.getItems().get(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX);
            if (!actionTaken && !accessoryStack.isEmpty()) {
                actionTaken = true;
                if (!world.isClientSide()) {
                    ItemStack particleStack = accessoryStack.copy();
                    hamster.spawnAtLocation((net.minecraft.server.level.ServerLevel) hamster.level(), accessoryStack);

                    hamster.setSilentInventoryUpdate(true);
                    hamster.setItem(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX, ItemStack.EMPTY);
                    hamster.setSilentInventoryUpdate(false);

                    hamster.updateAccessoryState();

                    world.playSound(null, hamster.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.9f, 1.0f + hamster.getRandom().nextFloat() * 0.1f);
                    ParticleEffectsUtil.spawnParticles(world, new Vec3(hamster.getX(), hamster.getY() + hamster.getBbHeight() * 0.5, hamster.getZ()), new ItemParticleOption(ParticleTypes.ITEM, particleStack.getItem()), 5, new Vec3(hamster.getBbWidth() / 2.0, hamster.getBbHeight() / 2.0, hamster.getBbWidth() / 2.0), 0.05);

                    if (!player.getAbilities().instabuild) {
                        stack.hurtAndBreak(1, player, (hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND));
                    }
                }
            }

            if (actionTaken) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    // --- Baby Unlinking ---
    public static InteractionResult handleBabyUnlink(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (hamster.isBaby() && hamster.getParentUuid() != null && ConfigDataCache.isLureItem(stack)) {
            if (!hamster.level().isClientSide()) {
                hamster.setParentUuid(null);

                player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.baby_unlinked").withStyle(ChatFormatting.GREEN));
                hamster.level().playSound(null, hamster.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
                ParticleEffectsUtil.spawnParticlesOnEntity(hamster, ParticleTypes.HEART, 3, 0.5, 0.5, 0.0, 0.5);

                if (!player.getAbilities().instabuild && Configs.AHP_MAIN.consumeLureItem) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- Shoulder Mounting ---
    public static InteractionResult handleShoulderMount(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (HamsterLureUtil.isShoulderMountItem(stack)) {
            if (!hamster.level().isClientSide()) {
                executeShoulderMount(hamster, player, stack);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- Aggression Toggle ---
    public static InteractionResult handleAggressionToggle(
            HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        boolean isPacifistItem = ConfigDataCache.isPacifistItem(stack);
        boolean isStandardItem = ConfigDataCache.isStandardAggressionItem(stack);
        boolean isMenaceItem = ConfigDataCache.isMenaceItem(stack);
        if (!HamsterInteractionGestureUtil.isAggressionToggleGesture(
                player.isShiftKeyDown(), isPacifistItem, isStandardItem, isMenaceItem)) {
            return InteractionResult.PASS;
        }

        HamsterDietUtil.AggressionToggleResult toggleResult =
                HamsterDietUtil.tryAggressionToggle(hamster, player, stack);
        if (toggleResult.isAccepted()) {
            if (!hamster.level().isClientSide()
                    && toggleResult.consumesItem()
                    && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- Inventory Open ---
    public static InteractionResult handleInventoryOpen(HamsterEntity hamster, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!hamster.level().isClientSide()) {
                if (hamster.isCheekPouchUnlocked() || !AdorableHamsterPets.MAIN_CONFIG.requireFoodMixToUnlockCheeks) {
                    MenuRegistry.openExtendedMenu((ServerPlayer) player, new HamsterScreenHandlerFactory(hamster));
                } else {
                    player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.cheek_pouch_locked").withStyle(ChatFormatting.WHITE));
                    hamster.playRefusalAnimation();
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // --- Feeding ---
    public static InteractionResult handleFeeding(HamsterEntity hamster, Player player, ItemStack stack, InteractionHand hand) {
        if (!player.isShiftKeyDown() && ConfigDataCache.isDietaryItem(stack)) {
            boolean willRefuse = HamsterDietUtil.checkAndHandleRefusal(hamster, player, stack);

            if (willRefuse) {
                return InteractionResult.SUCCESS; // Handled: refuse, trigger headshake anim, player hand swing
            }

            int feedResult = HamsterDietUtil.tryFeeding(hamster, player, stack);

            if (feedResult == 1) {
                // Fed successfully
                if (!hamster.level().isClientSide()) {
                    hamster.setLastFoodItem(stack.copy());
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            } else if (feedResult == 2) {
                // Refused (e.g., limit reached, cooldown active)
                return InteractionResult.SUCCESS;
            }

            // If feedResult == 0, hamster is full and not interested
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                           Public/Private Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Calculates which slot the hamster will occupy based on current player state and config.
     * Exposed for external mods (like Punchy) to accurately predict mounting logic for client-side animations.
     *
     * @param player The player mounting the hamster.
     * @return The next available ShoulderLocation, or null if full.
     */
    @Nullable
    public static ShoulderLocation getNextAvailableSlot(Player player) {
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;
        MountPriority priority = Configs.AHP_MAIN.mountPriority.get();

        // --- Capacity Check ---
        if (playerAccessor.adorablehamsterpets$getMountOrderQueue().size() >= Configs.AHP_MAIN.maxShoulderHamsters.get()) {
            return null;
        }

        // --- Priority Logic ---
        if (priority == MountPriority.HEAD_FIRST) {
            // Check Head -> Right -> Left
            if (playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty()) return ShoulderLocation.HEAD;
            if (playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty()) return ShoulderLocation.RIGHT_SHOULDER;
            if (playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) return ShoulderLocation.LEFT_SHOULDER;
        } else {
            // Default: Shoulders -> Head
            if (playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty()) return ShoulderLocation.RIGHT_SHOULDER;
            if (playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) return ShoulderLocation.LEFT_SHOULDER;
            if (playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty()) return ShoulderLocation.HEAD;
        }

        return null;
    }

    /**
     * Identifies which shoulder slot will be dismounted/thrown next, based on the player's config
     * and the synced Mount Order Queue.
     *
     * @param player The player to check.
     * @return The ShoulderLocation that is next in line to be dismounted, or null if shoulders are empty.
     */
    @Nullable
    public static ShoulderLocation getNextSlotToDismount(Player player) {
        return getNextSlotToDismount(player, false);
    }

    /**
     * Identifies which shoulder slot will be processed next, optionally bypassing hamsters whose throw cooldown
     * is still active. If every hamster is cooling down, returns the normal first slot so existing feedback remains.
     */
    @Nullable
    public static ShoulderLocation getNextSlotToDismount(Player player, boolean skipThrowCooldown) {
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;

        ArrayDeque<ShoulderLocation> queue = playerAccessor.adorablehamsterpets$getMountOrderQueue();

        // Failsafe rebuild if queue is empty but data exists
        if (queue.isEmpty() && playerAccessor.hasAnyShoulderHamster()) {
            for (ShoulderLocation location : ShoulderLocation.values()) {
                if (!playerAccessor.getShoulderHamster(location).isEmpty()) {
                    queue.addLast(location);
                }
            }
        }

        if (queue.isEmpty()) {
            return null;
        }

        DismountOrder order = Configs.AHP_MAIN.dismountOrder.get();
        ShoulderLocation firstSlot = order == DismountOrder.LIFO ? queue.peekLast() : queue.peekFirst();
        if (!skipThrowCooldown) {
            return firstSlot;
        }

        Iterator<ShoulderLocation> iterator = order == DismountOrder.LIFO
                ? queue.descendingIterator()
                : queue.iterator();
        long currentTime = player.level().getGameTime();

        while (iterator.hasNext()) {
            ShoulderLocation location = iterator.next();
            CompoundTag hamsterData = playerAccessor.getShoulderHamster(location);
            if (!hamsterData.isEmpty()
                    && (!hamsterData.contains("throwCooldownEndTick")
                    || hamsterData.getLongOr("throwCooldownEndTick", 0L) <= currentTime)) {
                return location;
            }
        }

        return firstSlot;
    }

    /** Removes a processed shoulder slot without disturbing skipped hamsters elsewhere in the queue. */
    public static void removeSlotFromDismountQueue(Player player, ShoulderLocation location) {
        ((PlayerEntityAccessor) player).adorablehamsterpets$getMountOrderQueue().remove(location);
    }

    /**
     * Gets the NBT data of the hamster that is queued to be thrown or dismounted next.
     * Exposed for external mods (like Punchy) to accurately predict throwing logic
     * during client-side animation charge phases.
     *
     * @param player The player throwing the hamster.
     * @return The NBT data of the hamster, or null if no hamster is mounted.
     */
    @Nullable
    public static CompoundTag getNextHamsterToDismountData(Player player) {
        ShoulderLocation nextSlot = getNextSlotToDismount(player, true);
        if (nextSlot != null) {
            return ((PlayerEntityAccessor) player).getShoulderHamster(nextSlot);
        }
        return null;
    }

    /**
     * Executes the logic to mount a hamster to a player's shoulder.
     * Accessible by both right-click interactions and force-mount keybinds.
     */
    public static void executeShoulderMount(HamsterEntity hamster, Player player, ItemStack stack) {
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;

        // --- Mount Priority Logic ---
        ShoulderLocation availableSlot = getNextAvailableSlot(player);

        // --- Mounting Logic ---
        if (availableSlot != null) {
            // Disable wander mode before saving
            hamster.setWanderModeActive(false);

            // Prevent shoulder hamster from being permanently stuck cleaning
            hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
            hamster.ambientSittingTimer = 0;

            // Save, update queue, and set state
            HamsterState data = HamsterNbtUtil.saveToHamsterState(hamster);
            // Add to queue before setting state so sync packet captures it
            playerAccessor.adorablehamsterpets$getMountOrderQueue().addLast(availableSlot);
            playerAccessor.setShoulderHamster(availableSlot, data.toNbt());

            BlockPos hamsterPosForMountSound = hamster.blockPosition();
            hamster.discard(); // Remove hamster from world

            // --- Universal Feedback ---
            if (player instanceof ServerPlayer serverPlayer) {
                ModCriteria.HAMSTER_ON_SHOULDER.get().trigger(serverPlayer);

                // Check for Hamster Tower Advancement
                if (!playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty() &&
                        !playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty() &&
                        !playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                    ModCriteria.MAX_SHOULDER_HAMSTERS.get().trigger(serverPlayer);
                }
            }
            player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.shoulder_mount_success"));

            // Calculate randomized SFX pitch once for client and server
            float pitch = 1.0f + (hamster.getRandom().nextFloat() - hamster.getRandom().nextFloat()) * 0.2f;
            SoundEvent mountSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SHOULDER_MOUNT_SOUNDS, hamster.getRandom());

            if (mountSound != null) {
                if (player instanceof ServerPlayer serverPlayer) {
                    // Play immediately for everyone except mounting player
                    hamster.level().playSound(player, player.blockPosition(), mountSound, SoundSource.PLAYERS, 1.0f, pitch);

                    // Calculate delay based on destination
                    int soundDelay = (availableSlot == ShoulderLocation.RIGHT_SHOULDER) ? 23 : 39;

                    // Send packet to mounting player to handle their own sound timing dynamically
                    NetworkManager.sendToPlayer(serverPlayer, new PlayShoulderMountSoundPayload(mountSound.location(), pitch, soundDelay));
                } else {
                    // Fallback: Instant feedback for everyone
                    hamster.level().playSound(null, player.blockPosition(), mountSound, SoundSource.PLAYERS, 1.0f, pitch);
                }
            }

            // --- Item-Specific Feedback ---
            if (ConfigDataCache.isLureItem(stack)) {
                SoundEvent mountLureSound = ModSounds.getDynamicItemSound(stack);
                float volume = ModSounds.getDynamicSoundVolume(mountLureSound);
                hamster.level().playSound(null, hamsterPosForMountSound, mountLureSound, SoundSource.PLAYERS, volume, 1.0f);

                ParticleEffectsUtil.spawnParticles(
                        hamster.level(),
                        Vec3.atCenterOf(hamsterPosForMountSound),
                        new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()),
                        8,
                        new Vec3(0.25, 0.25, 0.25),
                        0.05
                );

                if (!player.getAbilities().instabuild && Configs.AHP_MAIN.consumeLureItem) {
                    stack.shrink(1);
                }
            }
        } else {
            player.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.shoulder_occupied"));
        }
    }

    /**
     * Selects a random item from the Default or Extra cheek pouch loot lists.
     * Prioritizes lists that actually contain items. If configured,
     * it pulls exclusively from a custom mini game rewards list.
     */
    private static Item getRandomMiniGameReward(HamsterEntity hamster) {
        if (!Configs.AHP_MAIN.usePouchLootForMiniGameRewards) {
            return ConfigDataCache.getRandomCustomMiniGameReward(hamster.getRandom());
        }

        List<Integer> validPools = new ArrayList<>();
        validPools.add(0); // Default is always valid

        // Check if Extra Loot list has entries
        if (!Configs.AHP_WORLDGEN.extraCheekLootList.isEmpty()) {
            validPools.add(1);
        }

        int selectedPool = validPools.get(hamster.getRandom().nextInt(validPools.size()));

        return (selectedPool == 1)
                ? ConfigDataCache.getRandomCustomLootItem(hamster.getRandom())
                : ConfigDataCache.getRandomDefaultLootItem(hamster.getRandom());
    }
}
