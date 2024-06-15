package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.ConstructorInvoker;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.server.ReloadableServerRegistries$EmptyTagLookupWrapper")
public interface EmptyTagLookupWrapperProxy {
    @ConstructorInvoker
    Object construct(@Type(className = "net.minecraft.core.RegistryAccess") Object registryManager);
}
