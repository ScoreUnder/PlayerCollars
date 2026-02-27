package org.jlortiz.playercollars.leash.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.jlortiz.playercollars.leash.LeashHeldByProxyImpl;
import org.jlortiz.playercollars.leash.WorldRenderActiveImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
@Environment(EnvType.CLIENT)
public abstract class MixinEntityRenderDispatcher {
    @Shadow
    public abstract <E extends Entity> void render(E entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", at = @At(value = "TAIL"))
    private <E extends Entity, S extends EntityRenderState> void addLeashRender(E entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<? super E, S> renderer, CallbackInfo ci) {
        if (!(MinecraftClient.getInstance().worldRenderer instanceof WorldRenderActiveImpl wrImpl)) return;
        if (wrImpl.playerCollars$isRenderingWorld()) return;

        // When our player is rendered in GUIs and such, we should also render the LeashProxyEntity
        if (!(entity instanceof LeashHeldByProxyImpl leashImpl)) return;
        var proxy = leashImpl.playerCollars$getLeashProxy();
        if (proxy == null) return;

        double xAdj = proxy.getX() - entity.getX();
        double yAdj = proxy.getY() - entity.getY();
        double zAdj = proxy.getZ() - entity.getZ();
        render(proxy, x + xAdj, y + yAdj, z + zAdj, tickDelta, matrices, vertexConsumers, 0xf000f0);
    }
}
