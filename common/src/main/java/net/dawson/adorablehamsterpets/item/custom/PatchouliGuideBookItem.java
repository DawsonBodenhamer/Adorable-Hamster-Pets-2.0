package net.dawson.adorablehamsterpets.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.List;

public class PatchouliGuideBookItem extends Item {
    public PatchouliGuideBookItem(Settings settings) {
        super(settings);
    }

    /**
     * Called when the player right-clicks with this item.
     * This opens the Patchouli book screen for the player.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity serverPlayer) {
            PatchouliAPI.get().openBookGUI(serverPlayer, Identifier.of("adorablehamsterpets", "hamster_tips_guide_book"));
        }
        return TypedActionResult.success(stack);
    }


    /**
     * Called when the player right-clicks a block with this item.
     * Mimics 1.20.1 vanilla book behavior by explicitly placing the item into empty lecterns.
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);

        // If the block is a lectern, add my book to it
        if (blockState.isOf(Blocks.LECTERN)) {
            return LecternBlock.putBookIfAbsent(context.getPlayer(), world, blockPos, blockState, context.getStack())
                    ? ActionResult.success(world.isClient())
                    : ActionResult.PASS;
        }

        return super.useOnBlock(context);
    }

    /**
     * Appends the custom tooltip, including a context-aware check to prevent
     * duplicating the mod name when another mod (like Jade) would also add it.
     * This method is annotated with @Environment to be stripped from dedicated servers.
     */
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        // --- 1. Add the primary hint text unconditionally ---
        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_guide_book.hint").formatted(Formatting.GRAY));

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
//            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
//        }

        super.appendTooltip(stack, world, tooltip, context);
    }
}