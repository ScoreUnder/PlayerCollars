package org.jlortiz.playercollars.mixin.crawlrendering;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.jlortiz.playercollars.accessor.BipedRenderExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "setupTransforms(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;setupTransforms(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V", shift = At.Shift.AFTER))
    private static void translateModelForCrawling(PlayerEntityRenderState state, MatrixStack matrixStack, float f, float g, CallbackInfo ci) {
        if (!(state instanceof BipedRenderExtensions ext) || !ext.playerCollars$isCrawlingWithPaws()) return;

        float t = (float) (Math.PI / 2) * state.leaningPitch;
        float sinPitch = MathHelper.sin(t);
        float negCosPitch = 1 - MathHelper.cos(t);
        float legLength = 0.725f;
        float pushIntoGround = 0.05f;
        float yNew = (legLength - pushIntoGround) * negCosPitch;
        float zNew = 1.25f * sinPitch;
        matrixStack.translate(0, yNew, zNew);
    }

    @ModifyExpressionValue(method = "setupTransforms(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;isSwimming:Z"))
    private static boolean setSwimmingWhenCrawling(boolean isSwimming, PlayerEntityRenderState state, MatrixStack matrixStack, float f, float g) {
        return isSwimming && !(state instanceof BipedRenderExtensions ext && ext.playerCollars$isCrawlingWithPaws());
    }
}
