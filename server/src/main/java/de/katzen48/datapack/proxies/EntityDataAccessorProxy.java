package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.ConstructorInvoker;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.server.commands.data.EntityDataAccessor")
public interface EntityDataAccessorProxy {
    @ConstructorInvoker
    Object create(@Type(className = "net.minecraft.world.entity.Entity") Object entity);

    @MethodName("getData")
    Object getData(@Type(className = "net.minecraft.server.commands.data.EntityDataAccessor") Object entityDataAccessor);
}
