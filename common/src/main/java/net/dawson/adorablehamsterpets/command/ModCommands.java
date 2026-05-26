package net.dawson.adorablehamsterpets.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.entity.custom.genetics.PaletteDefinition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModCommands {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Suggestion Providers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final SuggestionProvider<ServerCommandSource> PALETTE_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>(HamsterPaletteManager.PALETTE_REGISTRY.keySet());
        suggestions.add("none");
        return CommandSource.suggestMatching(suggestions, builder);
    };

    private static final SuggestionProvider<ServerCommandSource> PATTERN_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(HamsterPaletteManager.OVERLAY_PATTERN_NAMES, builder);

    private static final SuggestionProvider<ServerCommandSource> EYE_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(HamsterPaletteManager.EYE_GENOTYPE_NAMES, builder);

    private static final SuggestionProvider<ServerCommandSource> AUTHOR_SUGGESTIONS = (context, builder) -> {
        Set<String> authors = HamsterPaletteManager.PALETTE_REGISTRY.values().stream().map(PaletteDefinition::author).collect(Collectors.toSet());
        authors.add("all");
        return CommandSource.suggestMatching(authors, builder);
    };

    private static final SuggestionProvider<ServerCommandSource> TIME_UNIT_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(List.of("days", "months", "years"), builder);

    private static final SuggestionProvider<ServerCommandSource> REPORT_OUTPUT_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(List.of("latest.log (prettier)", "chat (not recommended)"), builder);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Registration
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {

        // Create the unified root node
        LiteralArgumentBuilder<ServerCommandSource> ahpRoot = CommandManager.literal("ahp");

        // --- 1. Utilities ---
        // OP Required
        ahpRoot.then(CommandManager.literal("print_genetics_report")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> PlayerCommandUtil.executeGeneticsReport(context.getSource(), "log"))
                .then(CommandManager.argument("output", StringArgumentType.word()).suggests(REPORT_OUTPUT_SUGGESTIONS)
                        .executes(context -> PlayerCommandUtil.executeGeneticsReport(context.getSource(), StringArgumentType.getString(context, "output")))
                )
        );

        ahpRoot.then(CommandManager.literal("set_age")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0))
                        .then(CommandManager.argument("unit", StringArgumentType.word()).suggests(TIME_UNIT_SUGGESTIONS)
                                .executes(context -> PlayerCommandUtil.executeSetAge(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), StringArgumentType.getString(context, "unit"), Collections.emptyList()))
                                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                        .executes(context -> PlayerCommandUtil.executeSetAge(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), StringArgumentType.getString(context, "unit"), EntityArgumentType.getEntities(context, "targets")))
                                )
                        )
                )
        );

        ahpRoot.then(CommandManager.literal("reset_player_breeding_history")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> PlayerCommandUtil.executeResetPlayerBreedingHistory(context.getSource(), Collections.singletonList(context.getSource().getPlayerOrThrow())))
                .then(CommandManager.argument("players", EntityArgumentType.players())
                        .executes(context -> PlayerCommandUtil.executeResetPlayerBreedingHistory(context.getSource(), EntityArgumentType.getPlayers(context, "players")))
                )
        );

        ahpRoot.then(CommandManager.literal("reset_hamster_breeding_history")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> PlayerCommandUtil.executeResetHamsterBreedingHistory(context.getSource(), Collections.emptyList()))
                .then(CommandManager.argument("hamsters", EntityArgumentType.entities())
                        .executes(context -> PlayerCommandUtil.executeResetHamsterBreedingHistory(context.getSource(), EntityArgumentType.getEntities(context, "hamsters")))
                )
        );

        ahpRoot.then(CommandManager.literal("unlock_all_advancements")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> PlayerCommandUtil.executeUnlockAllModAdvancements(context.getSource()))
        );

        ahpRoot.then(CommandManager.literal("reset_tree_economy")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> PlayerCommandUtil.executeResetHeistHistory(context.getSource()))
        );

        // No OP required
        ahpRoot.then(CommandManager.literal("trigger_guidebook_fx")
                .requires(source -> true)
                .executes(context -> PlayerCommandUtil.executeTriggerBookEffects(context.getSource()))
        );

        ahpRoot.then(CommandManager.literal("give_guidebook")
                .requires(source -> true)
                .executes(context -> PlayerCommandUtil.executeGiveGuidebook(context.getSource()))
        );

        // --- 2. Genetics & Spawning Engine ---
        // OP Required
        ahpRoot.then(CommandManager.literal("undo_last_spawn")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> HamsterSpawnCommandUtil.executeUndoLastSpawn(context.getSource()))
        );

        ahpRoot.then(CommandManager.literal("spawn_all_bases_2D")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("with_wild_overlays", BoolArgumentType.bool())
                        .then(CommandManager.argument("with_sample_breeding_overlays", BoolArgumentType.bool())
                                .then(CommandManager.argument("author", StringArgumentType.word()).suggests(AUTHOR_SUGGESTIONS)
                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author")))
                                )
                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), "all"))
                        )
                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), false, "all"))
                )
                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), false, false, "all"))
        );

        ahpRoot.then(CommandManager.literal("spawn_all_bases_3D")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("with_wild_overlays", BoolArgumentType.bool())
                        .then(CommandManager.argument("with_sample_breeding_overlays", BoolArgumentType.bool())
                                .then(CommandManager.argument("author", StringArgumentType.word()).suggests(AUTHOR_SUGGESTIONS)
                                        .then(CommandManager.argument("spacing_multiplier", DoubleArgumentType.doubleArg(0.1))
                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier")))
                                        )
                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), 1.0))
                                )
                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), "all", 1.0))
                        )
                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), false, "all", 1.0))
                )
                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), false, false, "all", 1.0))
        );

        ahpRoot.then(CommandManager.literal("spawn_all_possible_permutations_THIS_CAN_BREAK_YOUR_WORLD")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("ignore_safety_limits", BoolArgumentType.bool())
                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllPermutations(context.getSource(), BoolArgumentType.getBool(context, "ignore_safety_limits")))
                )
                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllPermutations(context.getSource(), false))
        );

        ahpRoot.then(CommandManager.literal("spawn")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("hamster")
                        .then(CommandManager.argument("basePalette", StringArgumentType.word()).suggests(PALETTE_SUGGESTIONS)
                                .then(CommandManager.argument("wildPattern", StringArgumentType.word()).suggests(PATTERN_SUGGESTIONS)
                                        .then(CommandManager.argument("wildPalette", StringArgumentType.word()).suggests(PALETTE_SUGGESTIONS)
                                                .then(CommandManager.argument("breedPattern", StringArgumentType.word()).suggests(PATTERN_SUGGESTIONS)
                                                        .then(CommandManager.argument("breedPalette", StringArgumentType.word()).suggests(PALETTE_SUGGESTIONS)
                                                                .then(CommandManager.argument("eyes", StringArgumentType.word()).suggests(EYE_SUGGESTIONS)
                                                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnSpecific(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "basePalette"),
                                                                                StringArgumentType.getString(context, "wildPattern"),
                                                                                StringArgumentType.getString(context, "wildPalette"),
                                                                                StringArgumentType.getString(context, "breedPattern"),
                                                                                StringArgumentType.getString(context, "breedPalette"),
                                                                                StringArgumentType.getString(context, "eyes")
                                                                        ))
                                                                ))))))));

        // Register root node to the dispatcher
        dispatcher.register(ahpRoot);
    }
}