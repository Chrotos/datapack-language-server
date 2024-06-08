package de.katzen48.datapack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

import java.util.Map;

@Proxies(className = "net.minecraft.commands.Commands")
public interface CommandsProxy {
    @FieldGetter("dispatcher")
    CommandDispatcher<Object> getDispatcher(Object commands);

    @MethodName("fillUsableCommands")
    void fillUsableCommands(Object commands, CommandNode<Object> tree, CommandNode<Object> result, Object source, Map<CommandNode<Object>, CommandNode<Object>> resultNodes);

    @MethodName("getParseException")
    CommandSyntaxException getParseException(Object commands, Object parseResults);
}
