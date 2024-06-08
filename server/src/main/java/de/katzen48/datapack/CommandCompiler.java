package de.katzen48.datapack;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;

import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;

public class CommandCompiler {
    private CommandsProxy commandDispatcher;
    private MinecraftServerProxy minecraftServerProxy;

    public CommandCompiler(ReflectionProxyFactory factory, MinecraftServerProxy minecraftServerProxy) {
        this.minecraftServerProxy = minecraftServerProxy;
        this.commandDispatcher = factory.reflectionProxy(CommandsProxy.class);
    }

    public ParseResults<?> compile(String command) {
        return getDispatcher().parse(command, createCommandSourceStack());
    }

    public CommandSyntaxException resolveException(ParseResults<?> results) {
        return commandDispatcher.getParseException(getCommands(), results);
    }

    public CompletableFuture<Suggestions> getCompletionSuggestions(String text) {
        ParseResults<Object> results = getDispatcher().parse(text, createCommandSourceStack());

        return getDispatcher().getCompletionSuggestions(results);
    }

    private Object createCommandSourceStack() {
        Object minecraftServer = minecraftServerProxy.getServer();
        return minecraftServerProxy.createCommandSourceStack(minecraftServer);
    }

    private Object getCommands() {
        Object minecraftServer = minecraftServerProxy.getServer();
        return minecraftServerProxy.getCommands(minecraftServer);
    }

    public CommandDispatcher<Object> getDispatcher() {
        
        return commandDispatcher.getDispatcher(getCommands());
    }
}
