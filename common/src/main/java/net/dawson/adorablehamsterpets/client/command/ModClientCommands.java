package net.dawson.adorablehamsterpets.client.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.sound.SoundEvents;

public class ModClientCommands {
    public static void register(CommandDispatcher<ClientCommandRegistrationEvent.ClientCommandSourceStack> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandRegistrationEvent.literal("ahp_open_config_screen")
                .executes(context -> {

                    MinecraftClient.getInstance().getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );

                    ConfigApiJava.INSTANCE.openScreen("adorablehamsterpets.main");
                    return 1;
                })
        );
    }
}