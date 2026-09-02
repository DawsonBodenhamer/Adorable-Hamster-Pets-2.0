package net.dawson.adorablehamsterpets.item.custom;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.InteractionResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.List;

public class PatchouliGuideBookItem extends Item {
    public PatchouliGuideBookItem(Properties settings) {
        super(settings);
    }

    /**
     * Called when the player right-clicks with this item.
     * This opens the Patchouli book screen for the player.
     */
    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user instanceof ServerPlayer serverPlayer) {
            PatchouliAPI.get().openBookGUI(serverPlayer, Identifier.fromNamespaceAndPath("adorablehamsterpets", "hamster_tips_guide_book"));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Appends the custom tooltip, including a context-aware check to prevent
     * duplicating the mod name when another mod (like Jade) would also add it.
     * This method is annotated with @Environment to be stripped from dedicated servers.
     */
    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        // --- 1. Add the primary hint text unconditionally ---
        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_guide_book.hint").withStyle(ChatFormatting.GRAY));

//        Note: The logic below does not seem to work and I'm tired of trying to figure it out, and it's not crucial so here we are.
//        // --- 2. Get Contextual Information ---
//        boolean isJadeLoaded = Platform.isModLoaded("jade");
//        boolean isEMILoaded = Platform.isModLoaded("emi");
//        boolean isREILoaded = Platform.isModLoaded("rei");
//        boolean isJEILoaded = Platform.isModLoaded("jei");
//        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
//
//        // --- 3. Determine screen context ---
//        // A tooltip is needed anywhere Jade does NOT add its own tooltip.
//        boolean needsToolTip = (currentScreen == null || currentScreen.getClass() == CreativeInventoryScreen.class);
//
//        // --- 4. Add the mod name line if needed ---
//        // Add line if EITHER Jade/EMI/REI/JEI are not installed OR we are in a screen that needs a tooltip.
//        if (!isJadeLoaded || !isEMILoaded || !isREILoaded || !isJEILoaded || needsToolTip) {
//            tooltip.accept(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
//        }

        super.appendHoverText(stack, context, display, tooltip, type);
    }
}