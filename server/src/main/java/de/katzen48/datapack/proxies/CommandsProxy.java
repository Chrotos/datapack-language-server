package de.katzen48.datapack.proxies;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

import java.util.Map;

@Proxies(className = "net.minecraft.commands.Commands")
public interface CommandsProxy {
    @FieldGetter("dispatcher")
    CommandDispatcher<Object> getDispatcher(
        @Type(className = "net.minecraft.commands.Commands") Object commands
    );

    @MethodName("fillUsableCommands")
    void fillUsableCommands(
        @Type(className = "net.minecraft.commands.Commands") Object commands, 
        CommandNode<Object> tree, 
        CommandNode<Object> result, 
        @Type(className = "net.minecraft.commands.CommandSourceStack") Object source, 
        Map<CommandNode<Object>, 
        CommandNode<?>> resultNodes
    );

    @Static
    @MethodName("getParseException")
    CommandSyntaxException getParseException(ParseResults<Object> parseResults);
}
