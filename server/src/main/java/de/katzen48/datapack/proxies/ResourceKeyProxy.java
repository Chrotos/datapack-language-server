package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.resources.ResourceKey")
public interface ResourceKeyProxy {
    @MethodName("registry")
    Object registry(@Type(className = "net.minecraft.resources.ResourceKey") Object resourceKey);
}
