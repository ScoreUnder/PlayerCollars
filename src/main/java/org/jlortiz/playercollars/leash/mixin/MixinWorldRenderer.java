package org.jlortiz.playercollars.leash.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import org.jlortiz.playercollars.leash.WorldRenderActiveImpl;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class MixinWorldRenderer implements WorldRenderActiveImpl {
    @Unique
    private boolean renderingWorld;

    @Override
    public boolean playerCollars$isRenderingWorld() {
        return renderingWorld;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderStart(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        renderingWorld = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void renderFinish(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        renderingWorld = false;
    }
}
