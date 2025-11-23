package net.dawson.adorablehamsterpets.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.advancement.Advancement;
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
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> executeUnlockAllModAdvancements(context.getSource()))
        );
    }

    private static int executeUnlockAllModAdvancements(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAdvancementTracker tracker = player.getAdvancementTracker();
        // The collection holds Advancement objects
        Collection<Advancement> allAdvancements = source.getServer().getAdvancementLoader().getAdvancements();
        int count = 0;

        for (Advancement advancement : allAdvancements) {
            Identifier id = advancement.getId();
            // Check for advancements in the mod's "husbandry" path
            if (id.getNamespace().equals(AdorableHamsterPets.MOD_ID) &&
                    (id.getPath().startsWith("husbandry/"))) {

                AdvancementProgress progress = tracker.getProgress(advancement);
                if (!progress.isDone()) {
                    // Grant all criteria for the advancement
                    for (String criterion : advancement.getCriteria().keySet()) {
                        tracker.grantCriterion(advancement, criterion);
                    }
                    count++;
                }
            }
        }

        final int finalCount = count;
        if (finalCount > 0) {
            source.sendFeedback(() -> Text.literal("Unlocked " + finalCount + " Adorable Hamster Pets advancements."), true);
        } else {
            source.sendFeedback(() -> Text.literal("No new Adorable Hamster Pets advancements to unlock or all already unlocked."), true);
        }
        return finalCount;
    }
}