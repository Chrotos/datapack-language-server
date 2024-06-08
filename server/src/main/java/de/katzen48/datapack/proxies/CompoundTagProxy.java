package de.katzen48.datapack.proxies;

import java.util.Set;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.nbt.CompoundTag")
public interface CompoundTagProxy {
    @MethodName("getAllKeys")
    Set<String> getAllKeys(@Type(className = "net.minecraft.nbt.CompoundTag") Object compoundTag);

    @MethodName("contains")
    boolean contains(@Type(className = "net.minecraft.nbt.CompoundTag") Object compoundTag, String key);
}
