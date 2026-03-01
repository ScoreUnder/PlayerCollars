package org.jlortiz.playercollars.leash.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jlortiz.playercollars.leash.LeashImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "isEntityOnAir", at = @At("HEAD"), cancellable = true)
    private void preventFlyKickFromLeash(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        LeashImpl leash = (LeashImpl) player;
        var leashHolder = leash.leashplayers$getProxyLeashHolder();
        if (leashHolder == null) return;
        var holderPos = leashHolder.getPos();
        var myPos = player.getPos();
        if (myPos.distanceTo(holderPos) >= leash.leashplayers$getMaxLeashLength() && myPos.getY() < holderPos.getY()) {
            cir.setReturnValue(false);
        }
    }
}
