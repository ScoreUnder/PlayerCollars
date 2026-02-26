package org.jlortiz.playercollars.mixin.crawlrendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.jlortiz.playercollars.accessor.BipedRenderExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.jlortiz.playercollars.PlayerCollarsMod.FOOT_PAWS_TAG;
import static org.jlortiz.playercollars.PlayerCollarsMod.getEquippedAccessories;

@Mixin(BipedEntityRenderer.class) @Environment(EnvType.CLIENT) public abstract class BipedEntityRendererMixin {
    @Inject(method = "updateBipedRenderState", at = @At("TAIL"))
    private static void addPawCrawlingStatus(LivingEntity entity, BipedEntityRenderState state, float tickDelta, ItemModelManager itemModelResolver, CallbackInfo ci) {
        if (!(state instanceof BipedRenderExtensions ext)) return;

        boolean isHorizontal = state.leaningPitch > 0;
        ext.playerCollars$setCrawlingWithPaws(isHorizontal && hasFootPaws(entity));
    }

    @Unique private static boolean hasFootPaws(LivingEntity entity) {
        return !getEquippedAccessories(entity, FOOT_PAWS_TAG).isEmpty();
    }
}
