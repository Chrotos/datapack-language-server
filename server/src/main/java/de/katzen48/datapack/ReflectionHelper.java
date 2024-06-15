package de.katzen48.datapack;

import de.katzen48.datapack.proxies.DecoderProxy;
import de.katzen48.datapack.proxies.CommandsProxy;
import de.katzen48.datapack.proxies.CompoundTagProxy;
import de.katzen48.datapack.proxies.DataResultErrorProxy;
import de.katzen48.datapack.proxies.DataResultProxy;
import de.katzen48.datapack.proxies.EmptyTagLookupWrapperProxy;
import de.katzen48.datapack.proxies.EntityDataAccessorProxy;
import de.katzen48.datapack.proxies.HolderLookupProviderProxy;
import de.katzen48.datapack.proxies.JsonOpsProxy;
import de.katzen48.datapack.proxies.LayeredRegistryAccessProxy;
import de.katzen48.datapack.proxies.LootDataTypeProxy;
import de.katzen48.datapack.proxies.MinecraftServerProxy;
import de.katzen48.datapack.proxies.ReferenceProxy;
import de.katzen48.datapack.proxies.RegistryLayerProxy;
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
    private LootDataTypeProxy lootDataTypeProxy;
    private DecoderProxy decoderProxy;
    private EmptyTagLookupWrapperProxy emptyTagLookupWrapperProxy;
    private JsonOpsProxy jsonOpsProxy;
    private LayeredRegistryAccessProxy layeredRegistryAccessProxy;
    private DataResultProxy dataResultProxy;
    private DataResultErrorProxy dataResultErrorProxy;
    private RegistryLayerProxy registryLayerProxy;
    private HolderLookupProviderProxy holderLookupProviderProxy;

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
        lootDataTypeProxy = factory.reflectionProxy(LootDataTypeProxy.class);
        decoderProxy = factory.reflectionProxy(DecoderProxy.class);
        emptyTagLookupWrapperProxy = factory.reflectionProxy(EmptyTagLookupWrapperProxy.class);
        jsonOpsProxy = factory.reflectionProxy(JsonOpsProxy.class);
        layeredRegistryAccessProxy = factory.reflectionProxy(LayeredRegistryAccessProxy.class);
        dataResultProxy = factory.reflectionProxy(DataResultProxy.class);
        dataResultErrorProxy = factory.reflectionProxy(DataResultErrorProxy.class);
        registryLayerProxy = factory.reflectionProxy(RegistryLayerProxy.class);
        holderLookupProviderProxy = factory.reflectionProxy(HolderLookupProviderProxy.class);
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

    public LootDataTypeProxy getLootDataTypeProxy() {
        return lootDataTypeProxy;
    }

    public DecoderProxy getDecoderProxy() {
        return decoderProxy;
    }

    public EmptyTagLookupWrapperProxy getEmptyTagLookupWrapperProxy() {
        return emptyTagLookupWrapperProxy;
    }

    public JsonOpsProxy getJsonOpsProxy() {
        return jsonOpsProxy;
    }

    public LayeredRegistryAccessProxy getLayeredRegistryAccessProxy() {
        return layeredRegistryAccessProxy;
    }

    public DataResultProxy getDataResultProxy() {
        return dataResultProxy;
    }

    public DataResultErrorProxy getDataResultErrorProxy() {
        return dataResultErrorProxy;
    }

    public RegistryLayerProxy getRegistryLayerProxy() {
        return registryLayerProxy;
    }

    public HolderLookupProviderProxy getHolderLookupProviderProxy() {
        return holderLookupProviderProxy;
    }

    public String getResourceLocationPathFromEntityTypeReference(Object object) {
        Object key = referenceProxy.key(object);
        Object registry = resourceKeyProxy.registry(key);
        return resourceLocationProxy.getPath(registry);
    }
}
