package net.dawson.adorablehamsterpets.util;

import dev.architectury.registry.menu.MenuRegistry;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.AI.HamsterTagGoal;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.screen.HamsterScreenHandlerFactory;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    public static ActionResult handleDebugToggle(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (player.isSneaking() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            if (hamster.getWorld().isClient()) {
                AhpUiConfig currentConfig = AdorableHamsterPets.UI_CONFIG;
                boolean newSetting = !currentConfig.enableJadeHamsterDebugInfo;

                currentConfig.enableJadeHamsterDebugInfo = newSetting;
                currentConfig.save();

                Text message = Text.translatable(
                        newSetting ? "message.adorablehamsterpets.debug_overlay_enabled" : "message.adorablehamsterpets.debug_overlay_disabled"
                ).formatted(newSetting ? Formatting.WHITE : Formatting.RED);
                player.sendMessage(message, true);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // --- Genetics Visualizer ---
    public static ActionResult handleGeneticsVisualizer(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (!player.isSneaking() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            if (hamster.isGeneticsVisualizerMember()) {
                if (!hamster.getWorld().isClient()) {
                    PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
                    UUID p1 = accessor.ahp$getGeneticParent1Uuid();
                    UUID p2 = accessor.ahp$getGeneticParent2Uuid();
                    UUID target = hamster.getUuid();

                    if (target.equals(p1) || target.equals(p2)) {
                        // Clicking an already selected parent clears visualization
                        accessor.ahp$setGeneticParent1Uuid(null);
                        accessor.ahp$setGeneticParent2Uuid(null);
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.clear").formatted(Formatting.YELLOW), true);
                    } else if (p1 == null || (p1 != null && p2 != null)) {
                        // Start a new selection
                        accessor.ahp$setGeneticParent1Uuid(target);
                        accessor.ahp$setGeneticParent2Uuid(null);
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.set_parent1").formatted(Formatting.WHITE), true);
                    } else {
                        // Set second parent
                        accessor.ahp$setGeneticParent2Uuid(target);
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.set_parent2").formatted(Formatting.WHITE), true);
                    }
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    // --- Tag Game ---
    public static ActionResult handleTagGame(HamsterEntity hamster, PlayerEntity player, Hand hand) {
        if (hamster.isPlayingTag()) {
            // Intercept Hamster-vs-Hamster Tag
            if (hamster.isInterHamsterTagActive) {
                if (!hamster.getWorld().isClient()) {
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
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.inter_hamster_tag_interrupted").formatted(Formatting.WHITE), true);
                }
                return ActionResult.SUCCESS;
            }

            // Standard Player-vs-Hamster Tag
            if (hamster.isOwner(player) || AdorableHamsterPets.MAIN_CONFIG.allowStrangerTag) {
                if (!hamster.getWorld().isClient()) {
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
                    hamster.tagGameCooldownEndTick = hamster.getWorld().getTime() + Configs.AHP_MAIN.hamsterVersusPlayerTagCooldown.get();
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
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, hamster.getRandom()), SoundCategory.NEUTRAL, 1.0f, 1.0f);
                    ParticleEffectsUtil.spawnParticles(
                            hamster.getWorld(),
                            new Vec3d(hamster.getX(), hamster.getBodyY(0.8), hamster.getZ()),
                            ParticleTypes.HEART,
                            3,
                            new Vec3d(0.3, 0.2, 0.3),
                            0.2
                    );

                    // Trigger Celebration Animation
                    hamster.triggerAnimOnServer("mainController", "anim_hamster_quick_bounce");

                    // 4. Schedule Gifting Sequence
                    long baseTime = hamster.getWorld().getTime();

                    hamster.scheduleTask(baseTime + 32, "start_gift_sequence", () -> {
                        Item giftItem = MinigameUtil.getRandomMiniGameReward(hamster);
                        if (giftItem != Items.AIR) {
                            MinigameUtil.executeGiftDeliverySequence(hamster, new ItemStack(giftItem), player);
                        }
                    });
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    // --- Taming ---
    public static ActionResult handleTaming(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (!hamster.isTamed()) {
            boolean isTamingFood = ConfigDataCache.isTamingFood(stack);
            boolean isSneaking = player.isSneaking();

            // --- 1. Normal Taming Path ---
            if (isSneaking && isTamingFood) {
                // Block taming if it is an ai-disabled statue and config forbids it
                if (hamster.isAiDisabled() && !AdorableHamsterPets.MAIN_CONFIG.allowTamingAiDisabled) {
                    if (!hamster.getWorld().isClient()) {
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.taming_statue_refusal").formatted(Formatting.RED), true);
                    }
                    return ActionResult.SUCCESS;
                }

                if (!hamster.getWorld().isClient()) {
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }

                    // Use config value for taming chance
                    final AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;
                    int denominator = Math.max(1, config.tamingChanceDenominator.get()); // Ensure denominator is at least 1
                    if (hamster.getRandom().nextInt(denominator) == 0) {
                        hamster.setOwnerUuid(player.getUuid());
                        hamster.setTamed(true, true);
                        hamster.getNavigation().stop();
                        hamster.setSitting(false, true);
                        hamster.setSleeping(false);
                        hamster.setTarget(null);
                        hamster.getWorld().sendEntityStatus(hamster, (byte) 7);

                        // Re-awaken if AI was disabled and reset statue physics
                        if (hamster.isAiDisabled()) {
                            hamster.setAiDisabled(false);
                            hamster.setNoGravity(false);
                            hamster.setInvulnerable(false);
                        }

                        // Play celebrate sound only on success
                        SoundEvent celebrateSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, hamster.getRandom());
                        hamster.getWorld().playSound(null, hamster.getBlockPos(), celebrateSound, SoundCategory.NEUTRAL, 0.7F, 1.0F);

                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            Criteria.TAME_ANIMAL.trigger(serverPlayer, hamster);
                            HamsterGeneticsAdvancementUtil.trackTamedHamster(serverPlayer, hamster);
                        }

                        // Baby link warning
                        if (Configs.AHP_UI.enableTamedBabyWarningMessage && hamster.isBaby() && hamster.getParentUuid() != null) {
                            Text lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.lureItems).copy().formatted(Formatting.GOLD, Formatting.BOLD);
                            player.sendMessage(Text.translatable("message.adorablehamsterpets.tamed_baby_still_linked_warning", lureName).formatted(Formatting.WHITE), true);
                        }
                    } else {
                        hamster.getWorld().sendEntityStatus(hamster, (byte) 6);
                    }
                }
                return ActionResult.SUCCESS;
            }

            // --- 2. Failure Feedback Path ---
            if (!hamster.getWorld().isClient() && hand == Hand.MAIN_HAND && hamster.interactionCooldown <= 0) {
                // 1.20.1: use food component directly
                boolean isAnyFood = stack.getItem().isFood() || ConfigDataCache.isDietaryItem(stack);
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
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.2f, 0.5f);

                    MutableText msg = Text.literal("\n").append(Text.translatable(messageKey).formatted(Formatting.RED));

                    // If player is also missing guidebook
                    if (!((PlayerEntityAccessor) player).ahp$computeHasGuideBook(player)) {
                        msg.append("\n\n").append(
                                Text.translatable("message.adorablehamsterpets.taming_failure_guidebook_link")
                                        .setStyle(Style.EMPTY
                                                .withColor(Formatting.GREEN)
                                                .withBold(true)
                                                .withUnderline(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ahp_open_config_screen"))
                                        )
                        ).append("\n");
                    } else {
                        msg.append("\n");
                    }

                    player.sendMessage(msg, false);
                    hamster.playRefusalAnimation();
                    return ActionResult.SUCCESS; // Consume interaction so player doesn't accidentally eat item
                }
            }
        }
        return ActionResult.PASS;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                           Owner Interactions
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Naming ---
    public static boolean consumeNameTag(PlayerEntity player, HamsterEntity hamster) {
        // 1. Check player inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.NAME_TAG)) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                return true;
            }
        }

        // 2. Check hamster cheek pouches (slots 0-5)
        for (int i = 0; i < HamsterInventoryUtil.CHEEK_POUCH_SIZE; i++) {
            ItemStack stack = hamster.getItems().get(i);
            if (stack.isOf(Items.NAME_TAG)) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                    hamster.markDirty();
                }
                return true;
            }
        }
        return false;
    }

    // --- Bed Linking ---
    public static ActionResult handleBedLinking(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (stack.getItem() instanceof HamsterBedItem) {
            if (!hamster.getWorld().isClient()) {
                UUID linkedUuid = null;
                // 1.20.1: parse existing nbt
                if (stack.hasNbt() && stack.getNbt().contains(ModNbtKeys.LINKED_HAMSTER_UUID)) {
                    linkedUuid = stack.getNbt().getUuid(ModNbtKeys.LINKED_HAMSTER_UUID);
                }

                Text nameToSet = hamster.hasCustomName() ? hamster.getName() : hamster.getDisplayName().copy().append(" " + hamster.getId());
                String nameJson = Text.Serializer.toJson(nameToSet);

                if (linkedUuid == null || !linkedUuid.equals(hamster.getUuid())) {
                    // 1.20.1: setup new stack with fresh nbt
                    ItemStack newStack = stack.copy();
                    NbtCompound nbt = newStack.getOrCreateNbt();

                    nbt.putUuid(ModNbtKeys.LINKED_HAMSTER_UUID, hamster.getUuid());
                    nbt.putString(ModNbtKeys.LINKED_HAMSTER_NAME, nameJson);
                    nbt.putString(ModNbtKeys.WANDER_DISTANCE, AdorableHamsterPets.MAIN_CONFIG.defaultWanderDistance.get().name());

                    player.setStackInHand(hand, newStack);

                    // Feedback
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.BLOCK_BAMBOO_WOOD_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                    ParticleEffectsUtil.spawnParticles(hamster.getWorld(), new Vec3d(hamster.getX(), hamster.getBodyY(0.5), hamster.getZ()), ParticleTypes.HAPPY_VILLAGER, 10, new Vec3d(0.5, 0.5, 0.5), 0.0);
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.bed_linked", hamster.getName()), true);

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        ModCriteria.HAMSTER_BED_LINKED.trigger(serverPlayer);
                    }
                } else {
                    // Re-configuring distance of already linked bed
                    WanderDistance currentDistance = AdorableHamsterPets.MAIN_CONFIG.defaultWanderDistance.get();
                    NbtCompound stackNbt = stack.getOrCreateNbt();

                    if (stackNbt.contains(ModNbtKeys.WANDER_DISTANCE)) {
                        try {
                            currentDistance = WanderDistance.valueOf(stackNbt.getString(ModNbtKeys.WANDER_DISTANCE).toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    WanderDistance[] values = WanderDistance.values();
                    WanderDistance nextDistance = values[(currentDistance.ordinal() + 1) % values.length];

                    stackNbt.putString(ModNbtKeys.WANDER_DISTANCE, nextDistance.name());

                    player.sendMessage(Text.translatable("message.adorablehamsterpets.wander_distance_set", hamster.getName(), nextDistance.asString()), true);
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // --- Armor Equipment ---
    public static ActionResult handleArmorEquip(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (!player.isSneaking() && stack.getItem() instanceof HamsterArmorItem) {
            if (!hamster.getWorld().isClient()) {
                ItemStack currentArmor = hamster.getArmorStack();
                ItemStack newArmor = stack.split(1);

                hamster.setArmorStack(newArmor);
                hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.NEUTRAL, 0.6f, 1.2f);

                if (!currentArmor.isEmpty()) {
                    if (!player.getInventory().insertStack(currentArmor)) {
                        player.dropItem(currentArmor, false);
                    }
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // --- State Restorations ---
    public static ActionResult handleStateRestoration(HamsterEntity hamster, PlayerEntity player, Hand hand) {
        World world = hamster.getWorld();

        if (hamster.isSleeping() || hamster.isKnockedOut() || hamster.isCelebratingDiamond() || hamster.isSulking()) {
            if (!world.isClient()) {
                if (hamster.isSleeping()) {
                    HamsterBedUtil.wakeUpFromBed(hamster, true);
                } else if (hamster.isKnockedOut()) {
                    SoundEvent wakeUpSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_WAKE_UP_SOUNDS, hamster.getRandom());
                    if (wakeUpSound != null)
                        world.playSound(null, hamster.getBlockPos(), wakeUpSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    hamster.setKnockedOut(false);
                    hamster.setSitting(false, true);
                    hamster.triggerAnimOnServer("mainController", "wakeup_from_ko");
                } else if (hamster.isCelebratingDiamond()) {
                    hamster.setCelebratingDiamond(false);
                    hamster.setSitting(false, true);
                    SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
                    world.playSound(null, hamster.getBlockPos(), affectionSound != null ? affectionSound : SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.NEUTRAL, affectionSound != null ? 1.0f : 0.5f, affectionSound != null ? hamster.getSoundPitch() : 1.5f);
                } else if (hamster.isSulking()) {
                    hamster.setSulking(false);
                    hamster.setSitting(false, true);
                    SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
                    world.playSound(null, hamster.getBlockPos(), affectionSound != null ? affectionSound : SoundEvents.ENTITY_CHICKEN_STEP, SoundCategory.NEUTRAL, affectionSound != null ? 1.0f : 0.5f, affectionSound != null ? hamster.getSoundPitch() : 1.5f);
                }
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    // --- Mouth Item Return ---
    public static ActionResult handleMouthItemReturn(HamsterEntity hamster, PlayerEntity player, Hand hand) {
        if (hamster.isHoldingMouthItem()) {
            if (!hamster.getWorld().isClient()) {
                ItemStack retrievedStack = hamster.getMouthItemStack().copy();
                player.getInventory().offerOrDrop(hamster.getMouthItemStack().copy());

                hamster.setMouthItemStack(ItemStack.EMPTY);
                hamster.setGenericInteractionTimer(0);
                hamster.setHoldingMouthItem(false);

                hamster.setFrozenMovement(true);
                hamster.setCelebrationTarget(player);
                hamster.setCelebrationTicks(30);
                hamster.triggerAnimOnServer("mainController", "anim_hamster_quick_bounce");

                hamster.getWorld().playSound(null, hamster.getBlockPos(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom()), SoundCategory.NEUTRAL, 1.0f, hamster.getSoundPitch());
                if (!retrievedStack.isEmpty()) {
                    SoundEvent pounceSound = ModSounds.getDynamicItemSound(retrievedStack);
                    float volume = ModSounds.getDynamicSoundVolume(pounceSound);
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7f);
                    ParticleEffectsUtil.spawnParticles(hamster.getWorld(), new Vec3d(hamster.getX(), hamster.getBodyY(0.5), hamster.getZ()), new ItemStackParticleEffect(ParticleTypes.ITEM, retrievedStack), 10, new Vec3d(0.2, 0.2, 0.2), 0.05);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // --- Accessory Application ---
    public static ActionResult handleAccessoryInteraction(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (hamster.isValid(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX, stack) && !player.isSneaking()) {
            if (!hamster.getWorld().isClient()) {
                ItemStack currentAccessory = hamster.getItems().get(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX);

                // If holding flower and hamster already has same flower, cycle position
                if (stack.isIn(ItemTags.FLOWERS) && currentAccessory.isIn(ItemTags.FLOWERS) && ItemStack.areItemsEqual(stack, currentAccessory)) {
                    int currentPos = hamster.getDataTracker().get(HamsterEntity.FLOWER_POS);
                    int nextPos = (currentPos % 3) + 1;
                    hamster.getDataTracker().set(HamsterEntity.FLOWER_POS, nextPos);

                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.BLOCK_PINK_PETALS_PLACE, SoundCategory.PLAYERS, 0.7f, 1.0f + hamster.getRandom().nextFloat() * 0.2f);
                    ParticleEffectsUtil.spawnParticles(hamster.getWorld(), new Vec3d(hamster.getX(), hamster.getY() + hamster.getHeight() * 0.75, hamster.getZ()), ParticleTypes.FALLING_SPORE_BLOSSOM, 7, new Vec3d(hamster.getWidth() / 2.0, hamster.getHeight() / 2.0, hamster.getWidth() / 2.0), 0.0);
                } else {
                    ItemStack toEquip = stack.split(1);
                    ItemStack toReturn = currentAccessory.copy();

                    hamster.setStack(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX, toEquip);

                    if (!toReturn.isEmpty()) {
                        hamster.dropStack(toReturn);
                    }

                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    ParticleEffectsUtil.spawnParticles(hamster.getWorld(), new Vec3d(hamster.getX(), hamster.getY() + hamster.getHeight() * 0.75, hamster.getZ()), new ItemStackParticleEffect(ParticleTypes.ITEM, toEquip), 7, new Vec3d(hamster.getWidth() / 2.0, hamster.getHeight() / 2.0, hamster.getWidth() / 2.0), 0.0);

                    if (toEquip.isIn(ItemTags.FLOWERS) && player instanceof ServerPlayerEntity serverPlayer) {
                        ModCriteria.APPLIED_FLOWER.trigger(serverPlayer, hamster);
                    }
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // --- Shearing ---
    public static ActionResult handleShearing(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (stack.isOf(Items.SHEARS) && !player.isSneaking()) {
            boolean actionTaken = false;
            World world = hamster.getWorld();

            // Priority: Remove Armor
            ItemStack armorStack = hamster.getArmorStack();
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem) {
                actionTaken = true;
                if (!world.isClient()) {
                    hamster.dropStack(armorStack);
                    hamster.setSilentInventoryUpdate(true);
                    hamster.setArmorStack(ItemStack.EMPTY);
                    hamster.setSilentInventoryUpdate(false);
                    hamster.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8f, 1.5f);
                    if (!player.getAbilities().creativeMode) {
                        stack.damage(1, player, (p) -> p.sendEquipmentBreakStatus(hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                    }
                }
            }

            // Secondary: Remove Accessory
            ItemStack accessoryStack = hamster.getItems().get(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX);
            if (!actionTaken && !accessoryStack.isEmpty()) {
                actionTaken = true;
                if (!world.isClient()) {
                    ItemStack particleStack = accessoryStack.copy();
                    hamster.dropStack(accessoryStack);

                    hamster.setSilentInventoryUpdate(true);
                    hamster.setStack(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX, ItemStack.EMPTY);
                    hamster.setSilentInventoryUpdate(false);

                    hamster.updateAccessoryState();

                    world.playSound(null, hamster.getBlockPos(), SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 0.9f, 1.0f + hamster.getRandom().nextFloat() * 0.1f);
                    ParticleEffectsUtil.spawnParticles(world, new Vec3d(hamster.getX(), hamster.getY() + hamster.getHeight() * 0.5, hamster.getZ()), new ItemStackParticleEffect(ParticleTypes.ITEM, particleStack), 5, new Vec3d(hamster.getWidth() / 2.0, hamster.getHeight() / 2.0, hamster.getWidth() / 2.0), 0.05);

                    if (!player.getAbilities().creativeMode) {
                        stack.damage(1, player, (p) -> p.sendEquipmentBreakStatus(hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                    }
                }
            }

            if (actionTaken) {
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    // --- Baby Unlinking ---
    public static ActionResult handleBabyUnlink(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (hamster.isBaby() && hamster.getParentUuid() != null && ConfigDataCache.isLureItem(stack)) {
            if (!hamster.getWorld().isClient()) {
                hamster.setParentUuid(null);

                player.sendMessage(Text.translatable("message.adorablehamsterpets.baby_unlinked").formatted(Formatting.GREEN), true);
                hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.2f);
                ParticleEffectsUtil.spawnParticlesOnEntity(hamster, ParticleTypes.HEART, 3, 0.5, 0.5, 0.0, 0.5);

                if (!player.getAbilities().creativeMode && Configs.AHP_MAIN.consumeLureItem) {
                    stack.decrement(1);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // --- Shoulder Mounting ---
    public static ActionResult handleShoulderMount(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (ConfigDataCache.isLureItem(stack)) {
            if (!hamster.getWorld().isClient()) {
                executeShoulderMount(hamster, player, stack);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // --- Aggression Toggle ---
    public static ActionResult handleAggressionToggle(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (player.isSneaking()) {
            boolean isAggressionItem = ConfigDataCache.isPacifistItem(stack)
                    || ConfigDataCache.isStandardAggressionItem(stack)
                    || ConfigDataCache.isMenaceItem(stack);

            if (isAggressionItem) {
                int toggleResult = HamsterDietUtil.tryAggressionToggle(hamster, player, stack);

                if (toggleResult == 1) {
                    if (!hamster.getWorld().isClient()) {
                        if (!player.getAbilities().creativeMode) {
                            stack.decrement(1);
                        }
                    }
                    return ActionResult.SUCCESS;
                }

                // If toggleResult == 0, hamster already in that state so fall through to opening inventory
            }
        }
        return ActionResult.PASS;
    }

    // --- Inventory Open ---
    public static ActionResult handleInventoryOpen(HamsterEntity hamster, PlayerEntity player, Hand hand) {
        if (player.isSneaking()) {
            if (!hamster.getWorld().isClient()) {
                if (hamster.isCheekPouchUnlocked() || !AdorableHamsterPets.MAIN_CONFIG.requireFoodMixToUnlockCheeks) {
                    MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, new HamsterScreenHandlerFactory(hamster));
                } else {
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.cheek_pouch_locked").formatted(Formatting.WHITE), true);
                    hamster.playRefusalAnimation();
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // --- Feeding ---
    public static ActionResult handleFeeding(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (!player.isSneaking() && ConfigDataCache.isDietaryItem(stack)) {
            boolean willRefuse = HamsterDietUtil.checkAndHandleRefusal(hamster, player, stack);

            if (willRefuse) {
                return ActionResult.SUCCESS; // Handled: refuse, trigger headshake anim, player hand swing
            }

            int feedResult = HamsterDietUtil.tryFeeding(hamster, player, stack);

            if (feedResult == 1) {
                // Fed successfully
                if (!hamster.getWorld().isClient()) {
                    hamster.setLastFoodItem(stack.copy());
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                }
                return ActionResult.SUCCESS;
            } else if (feedResult == 2) {
                // Refused (e.g., limit reached, cooldown active)
                return ActionResult.SUCCESS;
            }

            // If feedResult == 0, hamster is full and not interested
            return ActionResult.PASS;
        }
        return ActionResult.PASS;
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
    public static ShoulderLocation getNextAvailableSlot(PlayerEntity player) {
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
    public static ShoulderLocation getNextSlotToDismount(PlayerEntity player) {
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
        return order == DismountOrder.LIFO ? queue.peekLast() : queue.peekFirst();
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
    public static NbtCompound getNextHamsterToDismountData(PlayerEntity player) {
        ShoulderLocation nextSlot = getNextSlotToDismount(player);
        if (nextSlot != null) {
            return ((PlayerEntityAccessor) player).getShoulderHamster(nextSlot);
        }
        return null;
    }

    /**
     * Executes the logic to mount a hamster to a player's shoulder.
     * Accessible by both right-click interactions and force-mount keybinds.
     */
    public static void executeShoulderMount(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
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

            BlockPos hamsterPosForMountSound = hamster.getBlockPos();
            hamster.discard(); // Remove hamster from world

            // --- Universal Feedback ---
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ModCriteria.HAMSTER_ON_SHOULDER.trigger(serverPlayer);

                // Check for Hamster Tower Advancement
                if (!playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty() &&
                        !playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty() &&
                        !playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                    ModCriteria.MAX_SHOULDER_HAMSTERS.trigger(serverPlayer);
                }
            }
            player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_mount_success"), true);

            // Calculate randomized SFX pitch once for client and server
            float pitch = 1.0f + (hamster.getRandom().nextFloat() - hamster.getRandom().nextFloat()) * 0.2f;
            SoundEvent mountSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SHOULDER_MOUNT_SOUNDS, hamster.getRandom());

            if (mountSound != null) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // Play immediately for everyone except mounting player
                    hamster.getWorld().playSound(player, player.getBlockPos(), mountSound, SoundCategory.PLAYERS, 1.0f, pitch);

                    // Calculate delay based on destination
                    int soundDelay = (availableSlot == ShoulderLocation.RIGHT_SHOULDER) ? 23 : 39;

                    // Send typed packet to mounting player to handle their own sound timing dynamically
                    ModPackets.CHANNEL.sendToPlayer(serverPlayer, new ModPackets.PlayMountSoundS2CPacket(mountSound.getId(), pitch, soundDelay));
                } else {
                    // Fallback: Instant feedback for everyone
                    hamster.getWorld().playSound(null, player.getBlockPos(), mountSound, SoundCategory.PLAYERS, 1.0f, pitch);
                }
            }

            // --- Item-Specific Feedback ---
            if (ConfigDataCache.isLureItem(stack)) {
                SoundEvent mountLureSound = ModSounds.getDynamicItemSound(stack);
                float volume = ModSounds.getDynamicSoundVolume(mountLureSound);
                hamster.getWorld().playSound(null, hamsterPosForMountSound, mountLureSound, SoundCategory.PLAYERS, volume, 1.0f);

                ParticleEffectsUtil.spawnParticles(
                        hamster.getWorld(),
                        Vec3d.ofCenter(hamsterPosForMountSound),
                        new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                        8,
                        new Vec3d(0.25, 0.25, 0.25),
                        0.05
                );

                if (!player.getAbilities().creativeMode && Configs.AHP_MAIN.consumeLureItem) {
                    stack.decrement(1);
                }
            }
        } else {
            player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_occupied"), true);
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