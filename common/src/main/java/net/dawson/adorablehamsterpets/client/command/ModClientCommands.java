package net.dawson.adorablehamsterpets.client.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.util.HamsterTextureUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class ModClientCommands {
    public static void register(CommandDispatcher<ClientCommandRegistrationEvent.ClientCommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(ClientCommandRegistrationEvent.literal("ahp_print_currently_rendered_hamster_textures_to_disc")
                .executes(context -> {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );

                    HamsterTextureUtil.dumpAllCachedTextures(Minecraft.getInstance().player);
                    return 1;
                })
        );

        dispatcher.register(ClientCommandRegistrationEvent.literal("ahp_open_config_screen")
                .executes(context -> {

                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );

                    // Root config screen
                    ConfigApiJava.INSTANCE.openScreen("adorablehamsterpets");
                    return 1;
                })
        );

        dispatcher.register(ClientCommandRegistrationEvent.literal("ahp_disable_throw_warning")
                .executes(context -> {
                    // Update and save config setting
                    Configs.AHP_UI.enableThrowCancellationWarning = false;
                    Configs.AHP_UI.save();

                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 1.5F)
                    );

                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.adorablehamsterpets.throw_warning.disabled").withStyle(ChatFormatting.YELLOW));
                    }
                    return 1;
                })
        );
    }
}