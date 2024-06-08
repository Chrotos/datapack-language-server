package de.katzen48.datapack.proxies;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
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
        @Type(className = "net.minecraft.commands.CoummandSourceStack") Object source, 
        Map<CommandNode<Object>, 
        CommandNode<Object>> resultNodes
    );

    @MethodName("getParseException")
    CommandSyntaxException getParseException(
        @Type(className = "net.minecraft.commands.Commands") Object commands,
        ParseResults<Object> parseResults
    );
}
