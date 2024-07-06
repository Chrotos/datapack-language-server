package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.world.level.storage.loot.LootDataType")
public interface LootDataTypeProxyRegistryKey {
    @FieldGetter("registryKey")
    Object registryKey(@Type(className = "net.minecraft.world.level.storage.loot.LootDataType") Object lootDataType);
}
