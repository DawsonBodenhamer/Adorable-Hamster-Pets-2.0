package net.dawson.adorablehamsterpets.util;

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
import net.dawson.adorablehamsterpets.screen.HamsterScreenHandlerFactory;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
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
    public static ActionResult handleDebugToggle(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        if (player.isSneaking() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            if (hamster.getWorld().isClient()) {
                AhpConfig currentConfig = AdorableHamsterPets.CONFIG;
                boolean newSetting = !currentConfig.enableJadeHamsterDebugInfo;

                currentConfig.enableJadeHamsterDebugInfo = newSetting;
                currentConfig.save();

                Text message = Text.translatable(
                        newSetting ? "message.adorablehamsterpets.debug_overlay_enabled" : "message.adorablehamsterpets.debug_overlay_disabled"
                ).formatted(newSetting ? Formatting.WHITE : Formatting.RED);
                player.sendMessage(message, true);
            }
            return ActionResult.success(hamster.getWorld().isClient());
        }
        return ActionResult.PASS;
    }

    // --- Genetics Visualizer ---
    public static ActionResult handleGeneticsVisualizer(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
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
                return ActionResult.success(hamster.getWorld().isClient());
            }
        }
        return ActionResult.PASS;
    }

    // --- Tag Game ---
    public static ActionResult handleTagGame(HamsterEntity hamster, PlayerEntity player) {
        if (hamster.isPlayingTag()) {
            if (hamster.isOwner(player) || AdorableHamsterPets.CONFIG.allowStrangerTag) {
                if (!hamster.getWorld().isClient()) {
                    // 1. Stop Goal & Clear State
                    hamster.setPlayingTag(false);
                    hamster.setTaunting(false);
                    hamster.getNavigation().stop();

                    // Clear debug name
                    if (hamster.getActiveCustomGoalDebugName().equals(HamsterTagGoal.class.getSimpleName())) {
                        hamster.setActiveCustomGoalDebugName("None");
                    }

                    // 2. Set Cooldowns
                    // Hamster cooldown
                    hamster.tagGameCooldownEndTick = hamster.getWorld().getTime() + Configs.AHP.tagGameCooldown.get();
                    // Player daily limit increment
                    if (player instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$incrementTagGameCount();
                    }

                    // 3. Start Celebration Phase
                    // Store the player who interacted as the rotation target
                    hamster.setCelebrationTarget(player);
                    HamsterMovementUtil.faceEntity(hamster, player);

                    // Lock rotation to target (Owner or Stranger) for the duration of both animations
                    hamster.setCelebratingRetrieval(true);
                    hamster.setCelebrationRetrievalTicks(80);
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
                    hamster.triggerAnimOnServer("mainController", "anim_hamster_celebrate_chase");

                    // 4. Schedule Gifting Sequence
                    long baseTime = hamster.getWorld().getTime();
                    long giftSequenceStart = baseTime + 32;

                    hamster.scheduleTask(giftSequenceStart, "start_gift_anim", () -> {
                        Item giftItem = getRandomTagGameReward(hamster);
                        if (giftItem != net.minecraft.item.Items.AIR) {
                            ItemStack giftStack = new ItemStack(giftItem);

                            // Trigger Unload Animation
                            hamster.triggerAnimOnServer("mainController", "anim_hamster_cheek_unload");

                            // T+10 (relative to start of gift sequence): Hamster "moves item" from cheek to mouth
                            hamster.scheduleTask(giftSequenceStart + 10, "gift_appear", () -> {
                                hamster.setMouthItemStack(giftStack);
                                hamster.setHoldingMouthItem(true);
                                hamster.setGenericInteractionTimer(0);
                            });

                            // T+33 (relative to start of gift sequence): Hamster spits out the item
                            hamster.scheduleTask(giftSequenceStart + 33, "gift_spit", () -> {
                                if (hamster.isHoldingMouthItem() && !hamster.getMouthItemStack().isEmpty()) {
                                    Vec3d look = hamster.getRotationVec(1.0f);
                                    ItemEntity itemEntity = new ItemEntity(hamster.getWorld(),
                                            hamster.getX() + look.x * 0.5,
                                            hamster.getY() + 0.3,
                                            hamster.getZ() + look.z * 0.5,
                                            hamster.getMouthItemStack().copy()
                                    );
                                    // Forward velocity to item
                                    itemEntity.setVelocity(look.x * 0.2, 0.2, look.z * 0.2);
                                    hamster.getWorld().spawnEntity(itemEntity);
                                }
                                // Cleanup
                                hamster.setMouthItemStack(ItemStack.EMPTY);
                                hamster.setHoldingMouthItem(false);
                            });
                        }
                    });
                }
                return ActionResult.success(hamster.getWorld().isClient());
            }
        }
        return ActionResult.PASS;
    }

    // --- Taming ---
    public static ActionResult handleTaming(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        if (!hamster.isTamed() && player.isSneaking() && ConfigDataCache.isTamingFood(stack)) {

            // Block taming if it is an ai-disabled statue and config forbids it
            if (hamster.isAiDisabled() && !AdorableHamsterPets.CONFIG.allowTamingAiDisabled) {
                if (!hamster.getWorld().isClient()) {
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.taming_statue_refusal").formatted(Formatting.RED), true);
                }
                return ActionResult.success(hamster.getWorld().isClient());
            }

            if (!hamster.getWorld().isClient()) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }

                // Use config value for taming chance
                final AhpConfig config = AdorableHamsterPets.CONFIG;
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
                    }

                    // Play celebrate sound only on success
                    SoundEvent celebrateSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, hamster.getRandom());
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), celebrateSound, SoundCategory.NEUTRAL, 0.7F, 1.0F);

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        Criteria.TAME_ANIMAL.trigger(serverPlayer, hamster);
                        HamsterGeneticsAdvancementUtil.trackTamedHamster(serverPlayer, hamster);
                    }

                    // --- Baby Link Warning ---
                    if (Configs.AHP.enableTamedBabyWarningMessage && hamster.isBaby() && hamster.getParentUuid() != null) {
                        Text lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP.lureItems).copy().formatted(Formatting.GOLD, Formatting.BOLD);
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.tamed_baby_still_linked_warning", lureName).formatted(Formatting.WHITE), true);
                    }
                } else {
                    hamster.getWorld().sendEntityStatus(hamster, (byte) 6);
                }
            }
            return ActionResult.success(hamster.getWorld().isClient());
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
                UUID linkedUuid = stack.get(ModDataComponentTypes.LINKED_HAMSTER_UUID.get());
                Text nameToSet = hamster.hasCustomName() ? hamster.getName() : hamster.getDisplayName().copy().append(" " + hamster.getId());

                if (linkedUuid == null || !linkedUuid.equals(hamster.getUuid())) {
                    // Initial link or re-linking to a new hamster
                    ItemStack newStack = stack.copy();
                    newStack.set(ModDataComponentTypes.LINKED_HAMSTER_UUID.get(), hamster.getUuid());
                    newStack.set(ModDataComponentTypes.LINKED_HAMSTER_NAME.get(), nameToSet);
                    newStack.set(ModDataComponentTypes.WANDER_DISTANCE.get(), AdorableHamsterPets.CONFIG.defaultWanderDistance.get());

                    player.setStackInHand(hand, newStack);

                    // Feedback
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.BLOCK_BAMBOO_WOOD_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                    ParticleEffectsUtil.spawnParticles(hamster.getWorld(), new Vec3d(hamster.getX(), hamster.getBodyY(0.5), hamster.getZ()), ParticleTypes.HAPPY_VILLAGER, 10, new Vec3d(0.5, 0.5, 0.5), 0.0);
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.bed_linked", hamster.getName()), true);

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        ModCriteria.HAMSTER_BED_LINKED.get().trigger(serverPlayer);
                    }
                } else {
                    // Re-configuring distance of already linked bed
                    WanderDistance currentDistance = stack.getOrDefault(ModDataComponentTypes.WANDER_DISTANCE.get(), AdorableHamsterPets.CONFIG.defaultWanderDistance.get());
                    WanderDistance[] values = WanderDistance.values();
                    WanderDistance nextDistance = values[(currentDistance.ordinal() + 1) % values.length];
                    stack.set(ModDataComponentTypes.WANDER_DISTANCE.get(), nextDistance);

                    player.sendMessage(Text.translatable("message.adorablehamsterpets.wander_distance_set", hamster.getName(), nextDistance.asString()), true);
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
            }
            return ActionResult.success(hamster.getWorld().isClient());
        }
        return ActionResult.PASS;
    }

    // --- Armor Equipment ---
    public static ActionResult handleArmorEquip(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        if (!player.isSneaking() && stack.getItem() instanceof HamsterArmorItem) {
            if (!hamster.getWorld().isClient()) {
                ItemStack currentArmor = hamster.getArmorStack();
                ItemStack newArmor = stack.split(1);

                hamster.setArmorStack(newArmor);
                hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_WOLF.value(), SoundCategory.NEUTRAL, 0.6f, 1.2f);

                if (!currentArmor.isEmpty()) {
                    if (!player.getInventory().insertStack(currentArmor)) {
                        player.dropItem(currentArmor, false);
                    }
                }
            }
            return ActionResult.success(hamster.getWorld().isClient());
        }
        return ActionResult.PASS;
    }

    // --- State Restorations ---
    public static ActionResult handleStateRestoration(HamsterEntity hamster, PlayerEntity player) {
        World world = hamster.getWorld();

        if (hamster.isSleeping()) {
            if (!world.isClient()) HamsterBedUtil.wakeUpFromBed(hamster, true);
            return ActionResult.success(world.isClient());
        }

        if (hamster.isKnockedOut()) {
            if (!world.isClient()) {
                SoundEvent wakeUpSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_WAKE_UP_SOUNDS, hamster.getRandom());
                if (wakeUpSound != null)
                    world.playSound(null, hamster.getBlockPos(), wakeUpSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                hamster.setKnockedOut(false);
                hamster.setSitting(false, true);
                hamster.triggerAnimOnServer("mainController", "wakeup_from_ko");
            }
            return ActionResult.success(world.isClient());
        }

        if (hamster.isCelebratingDiamond()) {
            if (!world.isClient()) {
                hamster.setCelebratingDiamond(false);
                hamster.setSitting(false, true);
                SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
                world.playSound(null, hamster.getBlockPos(), affectionSound != null ? affectionSound : SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.NEUTRAL, affectionSound != null ? 1.0f : 0.5f, affectionSound != null ? hamster.getSoundPitch() : 1.5f);
            }
            return ActionResult.success(world.isClient());
        }

        if (hamster.isSulking()) {
            if (!world.isClient()) {
                hamster.setSulking(false);
                hamster.setSitting(false, true);
                SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom());
                world.playSound(null, hamster.getBlockPos(), affectionSound != null ? affectionSound : SoundEvents.ENTITY_CHICKEN_STEP, SoundCategory.NEUTRAL, affectionSound != null ? 1.0f : 0.5f, affectionSound != null ? hamster.getSoundPitch() : 1.5f);
            }
            return ActionResult.success(world.isClient());
        }

        return ActionResult.PASS;
    }

    // --- Mouth Item Return ---
    public static ActionResult handleMouthItemReturn(HamsterEntity hamster, PlayerEntity player) {
        if (hamster.isHoldingMouthItem()) {
            if (!hamster.getWorld().isClient()) {
                ItemStack retrievedStack = hamster.getMouthItemStack().copy();
                player.getInventory().offerOrDrop(hamster.getMouthItemStack().copy());

                hamster.setMouthItemStack(ItemStack.EMPTY);
                hamster.setGenericInteractionTimer(0);
                hamster.setHoldingMouthItem(false);

                hamster.setCelebratingRetrieval(true);
                hamster.setCelebrationTarget(player);
                hamster.setCelebrationRetrievalTicks(30);
                hamster.triggerAnimOnServer("mainController", "anim_hamster_celebrate_chase");

                hamster.getWorld().playSound(null, hamster.getBlockPos(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster.getRandom()), SoundCategory.NEUTRAL, 1.0f, hamster.getSoundPitch());
                if (!retrievedStack.isEmpty()) {
                    SoundEvent pounceSound = ModSounds.getDynamicItemSound(retrievedStack);
                    float volume = ModSounds.getDynamicSoundVolume(pounceSound);
                    hamster.getWorld().playSound(null, hamster.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7f);
                    ParticleEffectsUtil.spawnParticles(hamster.getWorld(), new Vec3d(hamster.getX(), hamster.getBodyY(0.5), hamster.getZ()), new ItemStackParticleEffect(ParticleTypes.ITEM, retrievedStack), 10, new Vec3d(0.2, 0.2, 0.2), 0.05);
                }
            }
            return ActionResult.success(hamster.getWorld().isClient());
        }
        return ActionResult.PASS;
    }

    // --- Accessory Application ---
    public static ActionResult handleAccessoryInteraction(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        if (hamster.isValid(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX, stack) && !player.isSneaking()) {
            if (!hamster.getWorld().isClient()) {
                ItemStack currentAccessory = hamster.getItems().get(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX);

                if (stack.isOf(Items.PINK_PETALS) && currentAccessory.isOf(Items.PINK_PETALS)) {
                    int currentPetalType = hamster.getDataTracker().get(HamsterEntity.PINK_PETAL_TYPE);
                    int nextPetalType = (currentPetalType % 3) + 1;
                    hamster.getDataTracker().set(HamsterEntity.PINK_PETAL_TYPE, nextPetalType);

                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.BLOCK_PINK_PETALS_PLACE, SoundCategory.PLAYERS, 0.7f, 1.0f + hamster.getRandom().nextFloat() * 0.2f);
                    ParticleEffectsUtil.spawnParticles(hamster.getWorld(), new Vec3d(hamster.getX(), hamster.getY() + hamster.getHeight() * 0.75, hamster.getZ()), ParticleTypes.FALLING_SPORE_BLOSSOM, 7, new Vec3d(hamster.getWidth() / 2.0, hamster.getHeight() / 2.0, hamster.getWidth() / 2.0), 0.0);
                } else {
                    ItemStack toEquip = stack.split(1);
                    ItemStack toReturn = currentAccessory.copy();

                    hamster.setStack(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX, toEquip);

                    if (!toReturn.isEmpty()) {
                        hamster.dropStack(toReturn);
                    }

                    hamster.getWorld().playSound(null, hamster.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                    ParticleEffectsUtil.spawnParticles(hamster.getWorld(), new Vec3d(hamster.getX(), hamster.getY() + hamster.getHeight() * 0.75, hamster.getZ()), new ItemStackParticleEffect(ParticleTypes.ITEM, toEquip), 7, new Vec3d(hamster.getWidth() / 2.0, hamster.getHeight() / 2.0, hamster.getWidth() / 2.0), 0.0);

                    if (toEquip.isOf(Items.PINK_PETALS) && player instanceof ServerPlayerEntity serverPlayer) {
                        ModCriteria.APPLIED_PINK_PETAL.get().trigger(serverPlayer, hamster);
                    }
                }
            }
            return ActionResult.success(hamster.getWorld().isClient());
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
                if (!world.isClient()) {
                    hamster.dropStack(armorStack);
                    hamster.setSilentInventoryUpdate(true);
                    hamster.setArmorStack(ItemStack.EMPTY);
                    hamster.setSilentInventoryUpdate(false);
                    hamster.playSound(SoundEvents.ITEM_ARMOR_UNEQUIP_WOLF, 0.8f, 1.5f);
                    if (!player.getAbilities().creativeMode) {
                        stack.damage(1, player, LivingEntity.getSlotForHand(hand));
                    }
                }
                actionTaken = true;
            }

            // Secondary: Remove Accessory
            ItemStack accessoryStack = hamster.getItems().get(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX);
            if (!actionTaken && !accessoryStack.isEmpty()) {
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
                        stack.damage(1, player, LivingEntity.getSlotForHand(hand));
                    }
                }
                actionTaken = true;
            }

            if (actionTaken) {
                return ActionResult.success(world.isClient());
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

                if (!player.getAbilities().creativeMode && Configs.AHP.consumeLureItem) {
                    stack.decrement(1);
                }
            }
            return ActionResult.success(hamster.getWorld().isClient());
        }
        return ActionResult.PASS;
    }

    // --- Shoulder Mounting ---
    public static ActionResult handleShoulderMount(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (ConfigDataCache.isLureItem(stack)) {
            if (!hamster.getWorld().isClient()) {
                executeShoulderMount(hamster, player, stack);
            }
            return ActionResult.success(hamster.getWorld().isClient());
        }
        return ActionResult.PASS;
    }

    // --- Inventory Open ---
    public static ActionResult handleInventoryOpen(HamsterEntity hamster, PlayerEntity player) {
        if (player.isSneaking()) {
            if (!hamster.getWorld().isClient()) {
                if (hamster.isCheekPouchUnlocked() || !AdorableHamsterPets.CONFIG.requireFoodMixToUnlockCheeks) {
                    MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, new HamsterScreenHandlerFactory(hamster));
                } else {
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.cheek_pouch_locked").formatted(Formatting.WHITE), true);
                    hamster.playRefusalAnimation();
                }
            }
            return ActionResult.success(hamster.getWorld().isClient());
        }
        return ActionResult.PASS;
    }

    // --- Feeding ---
    public static ActionResult handleFeeding(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        boolean isPotentialFood = ConfigDataCache.isStandardFood(stack) || ConfigDataCache.isBuffFood(stack) || ConfigDataCache.isPouchUnlockFood(stack);

        if (!player.isSneaking() && isPotentialFood) {
            if (!hamster.getWorld().isClient()) {
                if (HamsterDietUtil.checkAndHandleRefusal(hamster, player, stack)) {
                    return ActionResult.success(false);
                }

                int feedResult = HamsterDietUtil.tryFeeding(hamster, player, stack);

                if (feedResult == 1) {
                    // Fed successfully
                    hamster.setLastFoodItem(stack.copy());
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    return ActionResult.SUCCESS;
                } else if (feedResult == 2) {
                    // Handled but refused
                    return ActionResult.success(false); // Skip hand swing
                }
            } else {
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                           Public/Private Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Executes the logic to mount a hamster to a player's shoulder.
     * Accessible by both right-click interactions and force-mount keybinds.
     */
    public static void executeShoulderMount(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;

        // --- Capacity Check ---
        if (playerAccessor.adorablehamsterpets$getMountOrderQueue().size() >= Configs.AHP.maxShoulderHamsters.get()) {
            player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_occupied"), true);
            return;
        }

        // --- Mount Priority Logic ---
        ShoulderLocation availableSlot = null;
        MountPriority priority = Configs.AHP.mountPriority.get();

        if (priority == MountPriority.HEAD_FIRST) {
            // Check Head -> Right -> Left
            if (playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty()) {
                availableSlot = ShoulderLocation.HEAD;
            } else if (playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty()) {
                availableSlot = ShoulderLocation.RIGHT_SHOULDER;
            } else if (playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                availableSlot = ShoulderLocation.LEFT_SHOULDER;
            }
        } else {
            // Default: Shoulders -> Head
            if (playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty()) {
                availableSlot = ShoulderLocation.RIGHT_SHOULDER;
            } else if (playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                availableSlot = ShoulderLocation.LEFT_SHOULDER;
            } else if (playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty()) {
                availableSlot = ShoulderLocation.HEAD;
            }
        }

        // --- Mounting Logic ---
        if (availableSlot != null) {
            // Disable wander mode before saving
            hamster.setWanderModeActive(false);

            // Prevent shoulder hamster from being permanently stuck cleaning
            hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
            hamster.cleaningTimer = 0;

            // Save, set, and update queue
            HamsterState data = HamsterNbtUtil.saveToHamsterState(hamster);
            playerAccessor.setShoulderHamster(availableSlot, data.toNbt());
            playerAccessor.adorablehamsterpets$getMountOrderQueue().addLast(availableSlot);

            BlockPos hamsterPosForMountSound = hamster.getBlockPos();
            hamster.discard(); // Remove hamster from world

            // --- Universal Feedback ---
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ModCriteria.HAMSTER_ON_SHOULDER.get().trigger(serverPlayer);

                // Check for Hamster Tower Advancement
                if (!playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty() &&
                        !playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty() &&
                        !playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                    ModCriteria.MAX_SHOULDER_HAMSTERS.get().trigger(serverPlayer);
                }
            }
            player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_mount_success"), true);

            SoundEvent mountSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SHOULDER_MOUNT_SOUNDS, hamster.getRandom());
            if (mountSound != null) {
                hamster.getWorld().playSound(null, player.getBlockPos(), mountSound, SoundCategory.PLAYERS, 1.0f, hamster.getSoundPitch());
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

                if (!player.getAbilities().creativeMode && Configs.AHP.consumeLureItem) {
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
     * it pulls exclusively from a custom tag rewards list.
     */
    private static Item getRandomTagGameReward(HamsterEntity hamster) {
        if (!Configs.AHP.usePouchLootForTagRewards) {
            return ConfigDataCache.getRandomCustomTagReward(hamster.getRandom());
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