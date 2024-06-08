package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.core.Holder$Reference")
public interface ReferenceProxy {
    @MethodName("key")
    Object key(@Type(className = "net.minecraft.core.Holder$Reference") Object reference);
}
