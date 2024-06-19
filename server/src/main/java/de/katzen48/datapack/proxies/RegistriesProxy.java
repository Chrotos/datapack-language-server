package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.core.registries.Registries")
public interface RegistriesProxy {
    @Static
    @MethodName("elementsDirPath")
    String elementsDirPath(@Type(className = "net.minecraft.resources.ResourceKey") Object registryRef);
}
