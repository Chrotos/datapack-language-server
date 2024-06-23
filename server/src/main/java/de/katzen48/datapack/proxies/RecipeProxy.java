package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;

@Proxies(className = "net.minecraft.world.item.crafting.Recipe")
public interface RecipeProxy {
    @Static
    @FieldGetter("CODEC")
    Object codec();
}
