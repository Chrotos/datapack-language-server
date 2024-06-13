package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;

@Proxies(className = "net.minecraft.core.registries.BuiltInRegistries")
public interface BuiltinRegistriesProxy {
    @Static
    @FieldGetter("DATA_COMPONENT_TYPE")
    Object getDataComponentTypeRegistry();
}
