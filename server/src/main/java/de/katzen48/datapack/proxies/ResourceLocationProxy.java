package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.resources.ResourceLocation")
public interface ResourceLocationProxy {
    @MethodName("getPath")
    String getPath(@Type(className = "net.minecraft.resources.ResourceLocation") Object resourceLocation);
}
