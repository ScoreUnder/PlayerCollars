package org.jlortiz.playercollars.leash;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

import static org.jlortiz.playercollars.leash.LeashServerSideInit.LEASH_PROXY_ENTITY_TYPE;

@Environment(EnvType.CLIENT)
public class LeashClientSideInit {
    private LeashClientSideInit() {
    }

    public static void initialize() {
        var nullModel = new EntityModel<LivingEntityRenderState>(new ModelPart(List.of(), Map.of())) {
        };
        EntityRendererRegistry.register(LEASH_PROXY_ENTITY_TYPE, c -> new LivingEntityRenderer(c, nullModel, 0) {
            @Override
            public EntityRenderState createRenderState() {
                return new LivingEntityRenderState();
            }

            @Override
            public Identifier getTexture(LivingEntityRenderState state) {
                return null;
            }
        });
    }
}
