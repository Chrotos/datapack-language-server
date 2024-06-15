package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;

@Proxies(className = "com.mojang.serialization.JsonOps")
public interface JsonOpsProxy {
    @Static
    @FieldGetter("INSTANCE")
    Object getInstance();
}
