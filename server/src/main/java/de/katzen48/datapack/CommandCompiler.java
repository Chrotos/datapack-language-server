package de.katzen48.datapack;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.internal.util.Util;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;

public class CommandCompiler {
    private CommandsProxy commandDispatcher;

    public CommandCompiler(Logger logger) {
        logger.info("Mojang Mapped: " + Util.mojangMapped());

        ReflectionRemapper reflectionMapper = ReflectionRemapper.forReobfMappingsInPaperJar();
        ReflectionProxyFactory factory = ReflectionProxyFactory.create(reflectionMapper, getClass().getClassLoader());

        this.commandDispatcher = factory.reflectionProxy(CommandsProxy.class);
    }

    public ParseResults<?> compile(String command) {
        return getDispatcher().parse(command, MinecraftServer.getServer().createCommandSourceStack());
    }

    public CommandSyntaxException resolveException(ParseResults<?> results) {
        return Commands.getParseException(results);
    }

    public CompletableFuture<Suggestions> getCompletionSuggestions(String text) {
        ParseResults<CommandSourceStack> results = getDispatcher().parse(text, MinecraftServer.getServer().createCommandSourceStack());

        return getDispatcher().getCompletionSuggestions(results);
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return commandDispatcher.getDispatcher(MinecraftServer.getServer().getCommands());
    }
}
