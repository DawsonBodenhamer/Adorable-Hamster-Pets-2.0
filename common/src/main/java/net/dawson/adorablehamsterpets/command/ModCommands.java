package net.dawson.adorablehamsterpets.command;

import net.minecraft.server.permissions.Permissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.entity.custom.genetics.PaletteDefinition;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModCommands {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Suggestion Providers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final SuggestionProvider<CommandSourceStack> COUNT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("1", "10", "100", "1000", "all_THIS_CAN_BREAK_YOUR_WORLD"), builder);

    private static final SuggestionProvider<CommandSourceStack> POSE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("sitting", "sleeping", "idle", "none"), builder);

    private static final SuggestionProvider<CommandSourceStack> PALETTE_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>(HamsterPaletteManager.PALETTE_REGISTRY.keySet());
        suggestions.add("none");
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PATTERN_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(HamsterPaletteManager.OVERLAY_PATTERN_NAMES, builder);

    private static final SuggestionProvider<CommandSourceStack> EYE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(HamsterPaletteManager.EYE_GENOTYPE_NAMES, builder);

    private static final SuggestionProvider<CommandSourceStack> AUTHOR_SUGGESTIONS = (context, builder) -> {
        Set<String> authors = HamsterPaletteManager.PALETTE_REGISTRY.values().stream().map(PaletteDefinition::author).collect(Collectors.toSet());
        authors.add("all");
        return SharedSuggestionProvider.suggest(authors, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> TIME_UNIT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("days", "months", "years"), builder);

    private static final SuggestionProvider<CommandSourceStack> REPORT_OUTPUT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("latest.log (prettier)", "chat (not recommended)"), builder);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Registration
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {

        // Create the unified root node
        LiteralArgumentBuilder<CommandSourceStack> ahpRoot = Commands.literal("ahp");

        // --- 1. Utilities ---
        // OP Required
        ahpRoot.then(Commands.literal("print_genetics_report")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> PlayerCommandUtil.executeGeneticsReport(context.getSource(), "log"))
                .then(Commands.argument("output", StringArgumentType.word()).suggests(REPORT_OUTPUT_SUGGESTIONS)
                        .executes(context -> PlayerCommandUtil.executeGeneticsReport(context.getSource(), StringArgumentType.getString(context, "output")))
                )
        );

        ahpRoot.then(Commands.literal("set_age")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .then(Commands.argument("unit", StringArgumentType.word()).suggests(TIME_UNIT_SUGGESTIONS)
                                .executes(context -> PlayerCommandUtil.executeSetAge(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), StringArgumentType.getString(context, "unit"), Collections.emptyList()))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(context -> PlayerCommandUtil.executeSetAge(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), StringArgumentType.getString(context, "unit"), EntityArgument.getEntities(context, "targets")))
                                )
                        )
                )
        );

        ahpRoot.then(Commands.literal("reset_player_breeding_history")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> PlayerCommandUtil.executeResetPlayerBreedingHistory(context.getSource(), Collections.singletonList(context.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> PlayerCommandUtil.executeResetPlayerBreedingHistory(context.getSource(), EntityArgument.getPlayers(context, "players")))
                )
        );

        ahpRoot.then(Commands.literal("reset_hamster_breeding_history")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> PlayerCommandUtil.executeResetHamsterBreedingHistory(context.getSource(), Collections.emptyList()))
                .then(Commands.argument("hamsters", EntityArgument.entities())
                        .executes(context -> PlayerCommandUtil.executeResetHamsterBreedingHistory(context.getSource(), EntityArgument.getEntities(context, "hamsters")))
                )
        );

        ahpRoot.then(Commands.literal("reset_hamster")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> HamsterResetCommandUtil.reset(
                        context.getSource(), Collections.emptyList()))
                .then(Commands.argument("hamsters", EntityArgument.entities())
                        .executes(context -> HamsterResetCommandUtil.reset(
                                context.getSource(), EntityArgument.getEntities(context, "hamsters"))))
        );

        ahpRoot.then(Commands.literal("unlock_all_advancements")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> PlayerCommandUtil.executeUnlockAllModAdvancements(context.getSource()))
        );

        ahpRoot.then(Commands.literal("reset_tree_economy")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> PlayerCommandUtil.executeResetHeistHistory(context.getSource()))
        );

        ahpRoot.then(Commands.literal("redstone_fever")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("apply")
                        .executes(context -> RedstoneFeverCommandUtil.apply(
                                context.getSource(), Collections.emptyList()))
                        .then(Commands.argument("hamsters", EntityArgument.entities())
                                .executes(context -> RedstoneFeverCommandUtil.apply(
                                        context.getSource(), EntityArgument.getEntities(context, "hamsters")))))
                .then(Commands.literal("cure")
                        .executes(context -> RedstoneFeverCommandUtil.cure(
                                context.getSource(), Collections.emptyList()))
                        .then(Commands.argument("hamsters", EntityArgument.entities())
                                .executes(context -> RedstoneFeverCommandUtil.cure(
                                        context.getSource(), EntityArgument.getEntities(context, "hamsters")))))
        );

        // No OP required
        ahpRoot.then(Commands.literal("trigger_guidebook_fx")
                .requires(source -> true)
                .executes(context -> PlayerCommandUtil.executeTriggerBookEffects(context.getSource()))
        );

        ahpRoot.then(Commands.literal("give_guidebook")
                .requires(source -> true)
                .executes(context -> PlayerCommandUtil.executeGiveGuidebook(context.getSource()))
        );

        // --- 2. Genetics & Spawning Engine ---
        // OP Required
        ahpRoot.then(Commands.literal("undo_last_spawn")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> HamsterSpawnCommandUtil.executeUndoLastSpawn(context.getSource()))
        );

        ahpRoot.then(Commands.literal("spawn_all_bases_2D")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("with_wild_overlays", BoolArgumentType.bool())
                        .then(Commands.argument("with_sample_breeding_overlays", BoolArgumentType.bool())
                                .then(Commands.argument("author", StringArgumentType.word()).suggests(AUTHOR_SUGGESTIONS)
                                        .then(Commands.argument("spacing_multiplier", DoubleArgumentType.doubleArg(0.1))
                                                .then(Commands.argument("randomize_sitting", BoolArgumentType.bool())
                                                        .then(Commands.argument("randomize_sleeping", BoolArgumentType.bool())
                                                                .then(Commands.argument("match_player_yaw", BoolArgumentType.bool())
                                                                        .then(Commands.argument("randomize_yaw", BoolArgumentType.bool())
                                                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), BoolArgumentType.getBool(context, "match_player_yaw"), BoolArgumentType.getBool(context, "randomize_yaw")))
                                                                        )
                                                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), BoolArgumentType.getBool(context, "match_player_yaw"), false))
                                                                )
                                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), false, false))
                                                        )
                                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), true, false, false))
                                                )
                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), true, true, false, false))
                                        )
                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), 1.0, true, true, false, false))
                                )
                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), "all", 1.0, true, true, false, false))
                        )
                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), false, "all", 1.0, true, true, false, false))
                )
                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases2D(context.getSource(), false, false, "all", 1.0, true, true, false, false))
        );

        ahpRoot.then(Commands.literal("spawn_all_bases_3D")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("with_wild_overlays", BoolArgumentType.bool())
                        .then(Commands.argument("with_sample_breeding_overlays", BoolArgumentType.bool())
                                .then(Commands.argument("author", StringArgumentType.word()).suggests(AUTHOR_SUGGESTIONS)
                                        .then(Commands.argument("spacing_multiplier", DoubleArgumentType.doubleArg(0.1))
                                                .then(Commands.argument("randomize_sitting", BoolArgumentType.bool())
                                                        .then(Commands.argument("randomize_sleeping", BoolArgumentType.bool())
                                                                .then(Commands.argument("match_player_yaw", BoolArgumentType.bool())
                                                                        .then(Commands.argument("randomize_yaw", BoolArgumentType.bool())
                                                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), BoolArgumentType.getBool(context, "match_player_yaw"), BoolArgumentType.getBool(context, "randomize_yaw")))
                                                                        )
                                                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), BoolArgumentType.getBool(context, "match_player_yaw"), false))
                                                                )
                                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), false, false))
                                                        )
                                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), true, false, false))
                                                )
                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), true, true, false, false))
                                        )
                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), StringArgumentType.getString(context, "author"), 1.0, true, true, false, false))
                                )
                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), BoolArgumentType.getBool(context, "with_sample_breeding_overlays"), "all", 1.0, true, true, false, false))
                        )
                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), BoolArgumentType.getBool(context, "with_wild_overlays"), false, "all", 1.0, true, true, false, false))
                )
                .executes(context -> HamsterSpawnCommandUtil.executeSpawnAllBases3D(context.getSource(), false, false, "all", 1.0, true, true, false, false))
        );

        ahpRoot.then(Commands.literal("spawn_random_group")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("count", StringArgumentType.word()).suggests(COUNT_SUGGESTIONS)
                        .then(Commands.argument("ignore_safety_limits", BoolArgumentType.bool())
                                .then(Commands.argument("spacing_multiplier", DoubleArgumentType.doubleArg(0.1))
                                        .then(Commands.argument("randomize_sitting", BoolArgumentType.bool())
                                                .then(Commands.argument("randomize_sleeping", BoolArgumentType.bool())
                                                        .then(Commands.argument("match_player_yaw", BoolArgumentType.bool())
                                                                .then(Commands.argument("randomize_yaw", BoolArgumentType.bool())
                                                                        .then(Commands.argument("use_wild_overlay_rules_for_breeding_overlays", BoolArgumentType.bool())
                                                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnRandomGroup(context.getSource(), StringArgumentType.getString(context, "count"), BoolArgumentType.getBool(context, "ignore_safety_limits"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), BoolArgumentType.getBool(context, "match_player_yaw"), BoolArgumentType.getBool(context, "randomize_yaw"), BoolArgumentType.getBool(context, "use_wild_overlay_rules_for_breeding_overlays")))
                                                                        )
                                                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnRandomGroup(context.getSource(), StringArgumentType.getString(context, "count"), BoolArgumentType.getBool(context, "ignore_safety_limits"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), BoolArgumentType.getBool(context, "match_player_yaw"), BoolArgumentType.getBool(context, "randomize_yaw"), false))
                                                                )
                                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnRandomGroup(context.getSource(), StringArgumentType.getString(context, "count"), BoolArgumentType.getBool(context, "ignore_safety_limits"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), BoolArgumentType.getBool(context, "match_player_yaw"), false, false))
                                                        )
                                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnRandomGroup(context.getSource(), StringArgumentType.getString(context, "count"), BoolArgumentType.getBool(context, "ignore_safety_limits"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), BoolArgumentType.getBool(context, "randomize_sleeping"), false, false, false))
                                                )
                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnRandomGroup(context.getSource(), StringArgumentType.getString(context, "count"), BoolArgumentType.getBool(context, "ignore_safety_limits"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), BoolArgumentType.getBool(context, "randomize_sitting"), true, false, false, false))
                                        )
                                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnRandomGroup(context.getSource(), StringArgumentType.getString(context, "count"), BoolArgumentType.getBool(context, "ignore_safety_limits"), DoubleArgumentType.getDouble(context, "spacing_multiplier"), true, true, false, false, false))
                                )
                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnRandomGroup(context.getSource(), StringArgumentType.getString(context, "count"), BoolArgumentType.getBool(context, "ignore_safety_limits"), 1.0, true, true, false, false, false))
                        )
                        .executes(context -> HamsterSpawnCommandUtil.executeSpawnRandomGroup(context.getSource(), StringArgumentType.getString(context, "count"), false, 1.0, true, true, false, false, false))
                )
        );

        ahpRoot.then(Commands.literal("spawn")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("hamster")
                        .then(Commands.argument("basePalette", StringArgumentType.word()).suggests(PALETTE_SUGGESTIONS)
                                .then(Commands.argument("wildPattern", StringArgumentType.word()).suggests(PATTERN_SUGGESTIONS)
                                        .then(Commands.argument("wildPalette", StringArgumentType.word()).suggests(PALETTE_SUGGESTIONS)
                                                .then(Commands.argument("breedPattern", StringArgumentType.word()).suggests(PATTERN_SUGGESTIONS)
                                                        .then(Commands.argument("breedPalette", StringArgumentType.word()).suggests(PALETTE_SUGGESTIONS)
                                                                .then(Commands.argument("eyes", StringArgumentType.word()).suggests(EYE_SUGGESTIONS)
                                                                        .then(Commands.argument("pose", StringArgumentType.word()).suggests(POSE_SUGGESTIONS)
                                                                                .executes(context -> HamsterSpawnCommandUtil.executeSpawnSpecific(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(context, "basePalette"),
                                                                                        StringArgumentType.getString(context, "wildPattern"),
                                                                                        StringArgumentType.getString(context, "wildPalette"),
                                                                                        StringArgumentType.getString(context, "breedPattern"),
                                                                                        StringArgumentType.getString(context, "breedPalette"),
                                                                                        StringArgumentType.getString(context, "eyes"),
                                                                                        StringArgumentType.getString(context, "pose")
                                                                                ))
                                                                        )
                                                                ))))))));
        // Register root node to the dispatcher
        dispatcher.register(ahpRoot);
    }
}
