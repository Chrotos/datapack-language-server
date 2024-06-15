package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;

@Proxies(className = "net.minecraft.server.RegistryLayer")
public interface RegistryLayerProxy {
    @Static
    @MethodName("valueOf")
    Object valueOf(String name);
}
