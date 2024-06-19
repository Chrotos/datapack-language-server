package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

import java.util.stream.Stream;

@Proxies(className = "net.minecraft.world.level.storage.loot.LootDataType")
public interface LootDataTypeProxyDirectory {
    @FieldGetter("directory")
    String directory(@Type(className = "net.minecraft.world.level.storage.loot.LootDataType") Object lootDataType);
}
