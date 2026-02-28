package org.jlortiz.playercollars.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "getBlockParticle", at = @At("TAIL"), cancellable = true)
    private void getBlockParticleForFences(CallbackInfoReturnable<Block> cir) {
        if (cir.getReturnValue() == null) {
            var player = this.client.player;
            if (player == null) return;
            Item item = player.getMainHandStack().getItem();
            if (item == PlayerCollarsMod.INVISIBLE_FENCE_BLOCK_ITEM && playerCollars$shouldShowInvisibleFence()) {
                cir.setReturnValue(((BlockItem)item).getBlock());
            }
        }
    }

    @Unique
    @Environment(EnvType.CLIENT)
    private static boolean playerCollars$shouldShowInvisibleFence() {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        return !PlayerCollarsMod.isPet(player);
    }
}
