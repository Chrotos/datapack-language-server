package de.katzen48.datapack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

import java.util.Map;

@Proxies(className = "net.minecraft.commands.Commands")
public interface CommandsProxy {
    @FieldGetter("dispatcher")
    CommandDispatcher<CommandSourceStack> getDispatcher(Commands commands);

    @MethodName("fillUsableCommands")
    void fillUsableCommands(Commands commands, CommandNode<CommandSourceStack> tree, CommandNode<SharedSuggestionProvider> result, CommandSourceStack source, Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> resultNodes);
}
