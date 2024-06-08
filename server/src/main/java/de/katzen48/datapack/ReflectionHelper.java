package de.katzen48.datapack;

import de.katzen48.datapack.proxies.CommandsProxy;
import de.katzen48.datapack.proxies.CompoundTagProxy;
import de.katzen48.datapack.proxies.EntityDataAccessorProxy;
import de.katzen48.datapack.proxies.MinecraftServerProxy;
import de.katzen48.datapack.proxies.ReferenceProxy;
import de.katzen48.datapack.proxies.ResourceKeyProxy;
import de.katzen48.datapack.proxies.ResourceLocationProxy;
import de.katzen48.datapack.proxies.SummonCommandProxy;
import de.katzen48.datapack.proxies.Vec3Proxy;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;

public class ReflectionHelper {
    private Class<?> referenceClass;
    private Class<?> compoundTagClass;

    private CommandsProxy commandsProxy;
    private MinecraftServerProxy minecraftServerProxy;
    private ReferenceProxy referenceProxy;
    private ResourceKeyProxy resourceKeyProxy;
    private ResourceLocationProxy resourceLocationProxy;
    private SummonCommandProxy summonCommandProxy;
    private Vec3Proxy vec3Proxy;
    private EntityDataAccessorProxy entityDataAccessorProxy;
    private CompoundTagProxy compoundTagProxy;

    public ReflectionHelper(ReflectionRemapper reflectionRemapper, ReflectionProxyFactory factory) throws ClassNotFoundException {
        this.referenceClass = Class.forName(reflectionRemapper.remapClassName("net.minecraft.core.Holder$Reference"));
        this.compoundTagClass = Class.forName(reflectionRemapper.remapClassName("net.minecraft.nbt.CompoundTag"));

        commandsProxy = factory.reflectionProxy(CommandsProxy.class);
        minecraftServerProxy = factory.reflectionProxy(MinecraftServerProxy.class);
        referenceProxy = factory.reflectionProxy(ReferenceProxy.class);
        resourceKeyProxy = factory.reflectionProxy(ResourceKeyProxy.class);
        resourceLocationProxy = factory.reflectionProxy(ResourceLocationProxy.class);
        summonCommandProxy = factory.reflectionProxy(SummonCommandProxy.class);
        vec3Proxy = factory.reflectionProxy(Vec3Proxy.class);
        entityDataAccessorProxy = factory.reflectionProxy(EntityDataAccessorProxy.class);
        compoundTagProxy = factory.reflectionProxy(CompoundTagProxy.class);
    }

    public CommandsProxy getCommandsProxy() {
        return commandsProxy;
    }

    public MinecraftServerProxy getMinecraftServerProxy() {
        return minecraftServerProxy;
    }

    public Class<?> getReferenceClass() {
        return referenceClass;
    }

    public Class<?> getCompoundTagClass() {
        return compoundTagClass;
    }

    public ReferenceProxy getReferenceProxy() {
        return referenceProxy;
    }

    public ResourceKeyProxy getResourceKeyProxy() {
        return resourceKeyProxy;
    }

    public ResourceLocationProxy getResourceLocationProxy() {
        return resourceLocationProxy;
    }

    public SummonCommandProxy getSummonCommandProxy() {
        return summonCommandProxy;
    }

    public Vec3Proxy getVec3Proxy() {
        return vec3Proxy;
    }

    public EntityDataAccessorProxy getEntityDataAccessorProxy() {
        return entityDataAccessorProxy;
    }

    public CompoundTagProxy getCompoundTagProxy() {
        return compoundTagProxy;
    }

    public String getResourceLocationPathFromEntityTypeReference(Object object) {
        Object key = referenceProxy.key(object);
        Object registry = resourceKeyProxy.registry(key);
        return resourceLocationProxy.getPath(registry);
    }
}
