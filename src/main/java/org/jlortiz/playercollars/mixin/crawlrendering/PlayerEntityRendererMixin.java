package org.jlortiz.playercollars.mixin.crawlrendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.jlortiz.playercollars.accessor.BipedRenderExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "setupTransforms(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V", at = @At("HEAD"))
    private static void translateModelForCrawling(PlayerEntityRenderState state, MatrixStack matrixStack, float f, float g, CallbackInfo ci) {
        if (state instanceof BipedRenderExtensions ext && ext.playerCollars$isCrawlingWithPaws()) {
            matrixStack.translate(0f, 0.375f, 0f);
        }
    }
}
