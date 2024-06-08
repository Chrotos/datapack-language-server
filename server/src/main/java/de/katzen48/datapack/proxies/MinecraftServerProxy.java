package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.server.MinecraftServer")
public interface MinecraftServerProxy {
    @Static
    @MethodName("getServer")
    Object getServer();

    @MethodName("getCommands")
    Object getCommands(
        @Type(className = "net.minecraft.server.MinecraftServer") Object minecraftServer
    );

    @MethodName("createCommandSourceStack")
    Object createCommandSourceStack(
        @Type(className = "net.minecraft.server.MinecraftServer") Object minecraftServer
    );
}