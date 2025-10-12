package net.dawson.adorablehamsterpets.command;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Handles registration of client-side-only commands.
 */
public class ModClientCommands {

    public static void register() {
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, registryAccess) -> {
            registerResetAnnouncementsCommand(dispatcher);
            registerReEnableAnnouncementsCommand(dispatcher);
        });
    }

    private static void registerResetAnnouncementsCommand(CommandDispatcher<ClientCommandRegistrationEvent.ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandRegistrationEvent.ClientCommandSourceStack> command = LiteralArgumentBuilder.<ClientCommandRegistrationEvent.ClientCommandSourceStack>literal("ahp_client")
                .then(LiteralArgumentBuilder.<ClientCommandRegistrationEvent.ClientCommandSourceStack>literal("reset_announcements")
                        .executes(context -> {
                            AnnouncementManager.INSTANCE.resetClientState();
                            if (MinecraftClient.getInstance().player != null) {
                                MinecraftClient.getInstance().player.sendMessage(
                                        Text.translatable("message.adorablehamsterpets.announcements_reset").formatted(Formatting.GREEN),
                                        false
                                );
                            }
                            return 1; // Success
                        })
                );

        dispatcher.register(command);
    }

    private static void registerReEnableAnnouncementsCommand(CommandDispatcher<ClientCommandRegistrationEvent.ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandRegistrationEvent.ClientCommandSourceStack> command = LiteralArgumentBuilder.<ClientCommandRegistrationEvent.ClientCommandSourceStack>literal("ahp_client")
                .then(LiteralArgumentBuilder.<ClientCommandRegistrationEvent.ClientCommandSourceStack>literal("reenable_announcements")
                        .executes(context -> {
                            AnnouncementManager.INSTANCE.reEnableOptionalAnnouncements();
                            if (MinecraftClient.getInstance().player != null) {
                                MinecraftClient.getInstance().player.sendMessage(
                                        Text.translatable("message.adorablehamsterpets.announcements_reenabled").formatted(Formatting.GREEN),
                                        false
                                );
                            }
                            return 1; // Success
                        })
                );

        dispatcher.register(command);
    }
}