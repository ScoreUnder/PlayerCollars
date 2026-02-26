package org.jlortiz.playercollars.leash;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.jlortiz.playercollars.PlayerCollarsMod;

public class LeashServerSideInit {
    public static final EntityType<LeashProxyEntity> LEASH_PROXY_ENTITY_TYPE;

    static {
        Identifier leashProxyId = Identifier.of(PlayerCollarsMod.MOD_ID, "leash_proxy");
        RegistryKey<EntityType<?>> registryKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE, leashProxyId);
        LEASH_PROXY_ENTITY_TYPE = Registry.register(
                Registries.ENTITY_TYPE,
                registryKey,
                EntityType.Builder.<LeashProxyEntity>create(LeashProxyEntity::new, SpawnGroup.MISC)
                        .disableSummon().dropsNothing().disableSaving()
                        .dimensions(0, 0)
                        .trackingTickInterval(20)
                        .maxTrackingRange(EntityType.PLAYER.getMaxTrackDistance()).build(registryKey));
    }

    private LeashServerSideInit() {
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(LEASH_PROXY_ENTITY_TYPE, MobEntity.createMobAttributes());
    }
}
