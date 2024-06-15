package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.core.HolderLookup$Provider")
public interface HolderLookupProviderProxy {
    @MethodName("createSerializationContext")
    Object createSerializationContext(
        @Type(className = "net.minecraft.core.HolderLookup$Provider") Object provider,
        @Type(className = "com.mojang.serialization.DynamicOps") Object delegate
    );
}
