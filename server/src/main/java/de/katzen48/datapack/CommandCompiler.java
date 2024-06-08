package de.katzen48.datapack;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;

public class CommandCompiler {
    private final ReflectionHelper reflectionHelper;

    public CommandCompiler(ReflectionHelper reflectionHelper) {
        this.reflectionHelper = reflectionHelper;
    }

    public ParseResults<Object> compile(String command) {
        return getDispatcher().parse(command, createCommandSourceStack());
    }

    public CommandSyntaxException resolveException(ParseResults<Object> results) {
        return reflectionHelper.getCommandsProxy().getParseException(results);
    }

    public CompletableFuture<Suggestions> getCompletionSuggestions(String text) {
        ParseResults<Object> results = getDispatcher().parse(text, createCommandSourceStack());

        return getDispatcher().getCompletionSuggestions(results);
    }

    private Object createCommandSourceStack() {
        Object minecraftServer = reflectionHelper.getMinecraftServerProxy().getServer();
        return reflectionHelper.getMinecraftServerProxy().createCommandSourceStack(minecraftServer);
    }

    private Object getCommands() {
        Object minecraftServer = reflectionHelper.getMinecraftServerProxy().getServer();
        return reflectionHelper.getMinecraftServerProxy().getCommands(minecraftServer);
    }

    public CommandDispatcher<Object> getDispatcher() {
        
        return reflectionHelper.getCommandsProxy().getDispatcher(getCommands());
    }
}
