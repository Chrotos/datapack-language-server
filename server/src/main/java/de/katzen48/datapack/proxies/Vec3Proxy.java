package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.ConstructorInvoker;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

@Proxies(className = "net.minecraft.world.phys.Vec3")
public interface Vec3Proxy {
    @ConstructorInvoker
    Object create(double x, double y, double z);
}
