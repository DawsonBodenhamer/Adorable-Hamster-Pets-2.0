package net.dawson.adorablehamsterpets.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;

public class ModCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("ahamsterpets_unlock_advancements")
                .requires(source -> source.hasPermissionLevel(2)) // Require op level 2 (typical for debug commands)
                .executes(context -> executeUnlockAllModAdvancements(context.getSource()))
        );
    }

    private static int executeUnlockAllModAdvancements(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAdvancementTracker tracker = player.getAdvancementTracker();
        Collection<AdvancementEntry> allAdvancements = source.getServer().getAdvancementLoader().getAdvancements();
        int count = 0; // This variable will be modified

        for (AdvancementEntry advancementEntry : allAdvancements) {
            Identifier id = advancementEntry.id();
            if (id.getNamespace().equals(AdorableHamsterPets.MOD_ID) &&
                    (id.getPath().startsWith("husbandry/"))) {

                AdvancementProgress progress = tracker.getProgress(advancementEntry);
                if (!progress.isDone()) {
                    for (String criterion : advancementEntry.value().criteria().keySet()) {
                        tracker.grantCriterion(advancementEntry, criterion);
                    }
                    count++; // count is modified here
                }
            }
        }

        // --- Create a final variable for use in the lambda ---
        final int finalCount = count;
        // --- End Create a final variable ---

        if (finalCount > 0) { // Use finalCount here
            source.sendFeedback(() -> Text.literal("Unlocked " + finalCount + " Adorable Hamster Pets advancements."), true);
        } else {
            source.sendFeedback(() -> Text.literal("No new Adorable Hamster Pets advancements to unlock or all already unlocked."), true);
        }
        return finalCount; // Return finalCount
    }
}