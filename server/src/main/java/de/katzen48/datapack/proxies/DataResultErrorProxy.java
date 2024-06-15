package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "com.mojang.serialization.DataResult$Error")
public interface DataResultErrorProxy {
    @MethodName("message")
    String message(@Type(className = "com.mojang.serialization.DataResult$Error") Object object);
}
