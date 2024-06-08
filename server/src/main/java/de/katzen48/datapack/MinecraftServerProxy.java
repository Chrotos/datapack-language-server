package de.katzen48.datapack;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;

@Proxies(className = "net.minecraft.server.MinecraftServer")
public interface MinecraftServerProxy {
    @Static
    @MethodName("getServer")
    Object getServer();

    @MethodName("getCommands")
    Object getCommands(Object minecraftServer);

    @MethodName("createCommandSourceStack")
    Object createCommandSourceStack(Object minecraftServer);
}