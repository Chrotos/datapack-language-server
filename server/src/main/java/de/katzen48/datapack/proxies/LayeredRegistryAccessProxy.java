package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.core.LayeredRegistryAccess")
public interface LayeredRegistryAccessProxy {
    @MethodName("getAccessForLoading")
    Object getAccessForLoading(
        @Type(className = "net.minecraft.core.LayeredRegistryAccess") Object registry,
        Object type
    );
}
