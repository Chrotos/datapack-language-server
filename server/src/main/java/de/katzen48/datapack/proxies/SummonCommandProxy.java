package de.katzen48.datapack.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

@Proxies(className = "net.minecraft.server.commands.SummonCommand")
public interface SummonCommandProxy {
    @Static
    @MethodName("createEntity")
    Object createEntity(
        @Type(className = "net.minecraft.commands.CommandSourceStack") Object source,
        @Type(className = "net.minecraft.core.Holder$Reference") Object entityType,
        @Type(className = "net.minecraft.world.phys.Vec3") Object pos,
        @Type(className = "net.minecraft.nbt.CompoundTag") Object nbt,
        boolean initialize
    );
}
