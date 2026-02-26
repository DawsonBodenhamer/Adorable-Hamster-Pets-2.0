package net.dawson.adorablehamsterpets.util;

import dev.architectury.registry.menu.MenuRegistry;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.dawson.adorablehamsterpets.screen.HamsterScreenHandlerFactory;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Encapsulates the interaction logic for HamsterEntity.
 * Each method acts as a step in a processing pipeline. If a method handles the interaction,
 * it returns a consuming ActionResult. Otherwise, it returns PASS to continue down the chain.
 */
public final class HamsterInteractionUtil {

    private HamsterInteractionUtil() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *                          Global Interactions
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- 1. Debug Toggle ---
    public static ActionResult handleDebugToggle(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        if (player.isSneaking() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            if (!hamster.getWorld().isClient()) {
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

    // --- 2. Tag Game ---
    public static ActionResult handleTagGame(HamsterEntity hamster, PlayerEntity player) {
        if (hamster.isPlayingTag()) {
            if (hamster.isOwner(player) || AdorableHamsterPets.CONFIG.allowStrangerTag) {
                if (!hamster.getWorld().isClient()) {
                    hamster.concludeTagGame(player);
                }
                return ActionResult.success(hamster.getWorld().isClient());
            }
        }
        return ActionResult.PASS;
    }

    // --- 3. Taming ---
    public static ActionResult handleTaming(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        if (!hamster.isTamed() && player.isSneaking() && ConfigDataCache.isTamingFood(stack)) {
            if (!hamster.getWorld().isClient()) {
                hamster.tryTame(player, stack);
            }
            return ActionResult.success(hamster.getWorld().isClient());
        }
        return ActionResult.PASS;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                           Owner Interactions
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- 4. Bed Linking ---
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

    // --- 5. Armor Equipment ---
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

    // --- 6. State Restorations (Sleep, KO, Celebration, Sulk) ---
    public static ActionResult handleStateRestoration(HamsterEntity hamster, PlayerEntity player) {
        World world = hamster.getWorld();

        if (hamster.isSleeping()) {
            if (!world.isClient()) HamsterBedUtil.wakeUpFromBed(hamster, true); // Manual wakeup
            return ActionResult.success(world.isClient());
        }

        if (hamster.isKnockedOut()) {
            if (!world.isClient()) {
                SoundEvent wakeUpSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_WAKE_UP_SOUNDS, hamster.getRandom());
                if (wakeUpSound != null) world.playSound(null, hamster.getBlockPos(), wakeUpSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
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

    // --- 7. Mouth Item Return ---
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

    // --- 8. Accessory Application & Cycling ---
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

    // --- 9. Shearing (Armor & Accessory Removal) ---
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

    // --- 10. Shoulder Mount ---
    public static ActionResult handleShoulderMount(HamsterEntity hamster, PlayerEntity player, ItemStack stack, Hand hand) {
        if (ConfigDataCache.isLureItem(stack)) {
            if (!hamster.getWorld().isClient()) {
                hamster.tryShoulderMount(player, stack);
            } else {
                player.swingHand(hand);
            }
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    // --- 11. Inventory Open ---
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
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    // --- 12. Feeding ---
    public static ActionResult handleFeeding(HamsterEntity hamster, PlayerEntity player, ItemStack stack) {
        boolean isPotentialFood = ConfigDataCache.isStandardFood(stack) || ConfigDataCache.isBuffFood(stack) || ConfigDataCache.isPouchUnlockFood(stack);

        if (!hamster.getWorld().isClient() && !player.isSneaking() && isPotentialFood) {
            if (HamsterDietUtil.checkAndHandleRefusal(hamster, player, stack)) {
                return ActionResult.CONSUME;
            }

            if (HamsterDietUtil.tryFeeding(hamster, player, stack)) {
                hamster.setLastFoodItem(stack.copy());
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.PASS;
    }
}