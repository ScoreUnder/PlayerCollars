package org.jlortiz.playercollars.leash.mixin;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.leash.LeashHeldByProxyImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    @Unique
    private boolean playerCollars$isLeashPullInevitable;

    private MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickLeashedPlayer(CallbackInfo ci) {
        playerCollars$isLeashPullInevitable = false;
        Entity holder = ((LeashHeldByProxyImpl) this).playerCollars$getRealLeashHolder();
        if (holder == null) return;

        double leashPullDist = getAttributeValue(PlayerCollarsMod.ATTR_LEASH_DISTANCE);
        ActionResult result = PlayerCollarsMod.applyLeashPull(this, holder.getPos(), leashPullDist, leashPullDist + 6);
        if (result != ActionResult.SUCCESS) return;

        Vec3d newVelocity = getVelocity();
        double deltaX = holder.getX() - getX();
        double deltaZ = holder.getZ() - getZ();
        double xzDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (xzDist == 0) return;
        double xzDotProduct = (newVelocity.getX() * deltaX + newVelocity.getZ() * deltaZ) / xzDist;
        playerCollars$isLeashPullInevitable = xzDotProduct > getMovementSpeed();
    }

    @Inject(method = "shouldAutoJump", at = @At("TAIL"), cancellable = true)
    private void forceAutoJumpWhenFarOnLeash(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        if (hasVehicle() || !isOnGround()) return;
        if (playerCollars$isLeashPullInevitable) {
            cir.setReturnValue(true);
        }
    }
}
