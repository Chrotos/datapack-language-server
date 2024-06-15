package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

import java.util.Optional;

@Proxies(className = "com.mojang.serialization.DataResult")
public interface DataResultProxy {
    @MethodName("error")
    Optional<Object> error(@Type(className = "com.mojang.serialization.DataResult") Object object);
}
