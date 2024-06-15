package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "com.mojang.serialization.Decoder")
public interface DecoderProxy {
    @MethodName("parse")
    Object parse(
        @Type(className = "com.mojang.serialization.Decoder") Object codec,
        @Type(className = "com.mojang.serialization.DynamicOps") Object ops,
        Object json
    );
}
