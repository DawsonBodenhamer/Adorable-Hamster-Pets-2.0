package net.dawson.adorablehamsterpets.client.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.util.HamsterTextureUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ModClientCommands {
    public static void register(CommandDispatcher<ClientCommandRegistrationEvent.ClientCommandSourceStack> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandRegistrationEvent.literal("ahp_print_currently_rendered_hamster_textures_to_disc")
                .executes(context -> {
                    MinecraftClient.getInstance().getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );

                    HamsterTextureUtil.dumpAllCachedTextures(MinecraftClient.getInstance().player);
                    return 1;
                })
        );

        dispatcher.register(ClientCommandRegistrationEvent.literal("ahp_open_config_screen")
                .executes(context -> {

                    MinecraftClient.getInstance().getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );

                    ConfigApiJava.INSTANCE.openScreen("adorablehamsterpets.main");
                    return 1;
                })
        );

        dispatcher.register(ClientCommandRegistrationEvent.literal("ahp_disable_throw_warning")
                .executes(context -> {
                    // Update and save config setting
                    Configs.AHP.enableThrowCancellationWarning = false;
                    Configs.AHP.save();

                    MinecraftClient.getInstance().getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 1.5F)
                    );

                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(Text.translatable("message.adorablehamsterpets.throw_warning.disabled").formatted(Formatting.YELLOW), false);
                    }
                    return 1;
                })
        );
    }
}