package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.core.Registry")
public interface RegistryProxy {
    @MethodName("get")
    Object get(@Type(className = "net.minecraft.resources.ResourceLocation") Object id);
}