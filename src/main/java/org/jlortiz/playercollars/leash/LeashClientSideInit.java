package org.jlortiz.playercollars.leash;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import static org.jlortiz.playercollars.leash.LeashServerSideInit.LEASH_PROXY_ENTITY_TYPE;

@Environment(EnvType.CLIENT)
public class LeashClientSideInit {
    private LeashClientSideInit() {
    }

    public static void initialize() {
        EntityRendererRegistry.register(LEASH_PROXY_ENTITY_TYPE, LeashProxyEntityRenderer::new);
    }
}
