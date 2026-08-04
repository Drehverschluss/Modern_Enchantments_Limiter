package com.modernenchantmentslimiter.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlParser;
import com.mojang.brigadier.context.CommandContext;

import com.modernenchantmentslimiter.ModernEnchantmentsLimiter;
import com.modernenchantmentslimiter.config.ModConfig;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.IConfigSpec.ILoadedConfig;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Adds {@code /modernenchantmentslimiter reload}, which re-reads the common config
 * from disk immediately. Normally not needed (NeoForge watches the config file and
 * hot-reloads it automatically), but useful when that file watcher doesn't pick up
 * a change (e.g. some editors/network drives) or you just don't want to wait for it.
 */
@EventBusSubscriber(modid = ModernEnchantmentsLimiter.MODID)
public final class ReloadConfigCommand {

    private ReloadConfigCommand() {}

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("modernenchantmentslimiter")
                        .then(Commands.literal("reload").executes(ReloadConfigCommand::reload))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        net.neoforged.fml.config.ModConfig fmlConfig = ModConfig.SERVER_FML_CONFIG;
        ILoadedConfig loadedConfig = fmlConfig == null ? null : fmlConfig.getLoadedConfig();
        if (loadedConfig == null) {
            context.getSource().sendFailure(Component.literal("Modern Enchantments Limiter config is not loaded yet."));
            return 0;
        }

        Path path = fmlConfig.getFullPath();
        CommentedConfig live = loadedConfig.config();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            live.clear();
            new TomlParser().parse(reader, live, ParsingMode.REPLACE);
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal("Failed to read config file: " + e.getMessage()));
            return 0;
        }

        fmlConfig.getSpec().acceptConfig(loadedConfig);

        context.getSource().sendSuccess(
                () -> Component.literal("Modern Enchantments Limiter config reloaded from disk."), true
        );
        return 1;
    }
}
