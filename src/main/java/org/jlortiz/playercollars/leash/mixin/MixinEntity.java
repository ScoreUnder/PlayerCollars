package org.jlortiz.playercollars.leash.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.BlockStateRaycastContext;
import net.minecraft.world.World;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.block.InvisibleFenceBlock;
import org.jlortiz.playercollars.leash.LeashHeldByProxyImpl;
import org.jlortiz.playercollars.leash.LeashServerSideImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public abstract World getWorld();

    @Shadow
    public abstract Vec3d getEyePos();

    @Shadow
    private World world;

    @Shadow
    public abstract Vec3d getPos();

    @ModifyExpressionValue(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;canStartRiding(Lnet/minecraft/entity/Entity;)Z"))
    private boolean canStartRidingEvenIfPet(boolean result, Entity entity, boolean force) {
        if (!result) return false;
        if (!PlayerCollarsMod.entityIsPet((Entity) (Object) this)) return true;
        if (!playerCollars$findInvisibleFence(entity) && !playerCollars$isTooFarFromLeashHolder(entity)) return true;
        if (!world.isClient && (Object) this instanceof PlayerEntity pe) {
            pe.sendMessage(Text.translatable("message.playercollars.no_ride_entity"), true);
        }
        return false;
    }

    @Inject(method = "canUsePortals", at = @At("TAIL"), cancellable = true)
    private void disableLeashedPortalUse(boolean allowVehicles, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;

        if(playerCollars$isLeashedPetOrLeashedPetsVehicle((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean playerCollars$isLeashedPetOrLeashedPetsVehicle(Entity entity) {
        if (playerCollars$isLeashedPet(entity)) return true;
        for (Entity passenger : entity.getPassengersDeep()) {
            if (playerCollars$isLeashedPet(passenger)) return true;
        }
        return false;
    }

    @Unique
    private static boolean playerCollars$isLeashedPet(Entity who) {
        if (!(who instanceof LeashHeldByProxyImpl impl)) return false;
        return impl.playerCollars$getRealLeashHolder() != null;
    }

    @Unique
    private boolean playerCollars$findInvisibleFence(Entity to) {
        Vec3d dest = to.getEyePos();
        Vec3d eyePos = getEyePos();
        if (eyePos.distanceTo(dest) > 32) {
            // Just in case there are some crazy mod shenanigans going on, I don't want to do an insanely long raycast
            return false;
        }
        // Make sure the entity to ride isn't past an invisible fence
        if (playerCollars$findInvisibleFence(eyePos, dest).getType() == HitResult.Type.BLOCK) return true;
        if (playerCollars$findInvisibleFence(getPos(), dest).getType() == HitResult.Type.BLOCK) return true;

        // Make sure the entity to ride isn't intersecting an invisible fence
        var i = new BlockCollisionSpliterator<>(world, (Entity) (Object) this, to.getBoundingBox(), false, (pos, voxelShape) -> pos);
        while (i.hasNext()) {
            var pos = i.next();
            if (world.getBlockState(pos).isOf(PlayerCollarsMod.INVISIBLE_FENCE_BLOCK)) return true;
        }
        return false;
    }

    @Unique
    private BlockHitResult playerCollars$findInvisibleFence(Vec3d from, Vec3d to) {
        return getWorld().raycast(new BlockStateRaycastContext(from, to, MixinEntity::playerCollars$isInvisibleFence));
    }

    @Unique
    private static boolean playerCollars$isInvisibleFence(BlockState bs) {
        return bs.getBlock() instanceof InvisibleFenceBlock;
    }

    @Unique
    private boolean playerCollars$isTooFarFromLeashHolder(Entity to) {
        if (!(this instanceof LeashServerSideImpl leash)) return false;
        Entity leashHolder = leash.playerCollars$getRealLeashHolder();
        if (leashHolder == null) return false;

        float newLeashLen = leashHolder.distanceTo(to);
        return newLeashLen + to.getDimensions(to.getPose()).width() > leash.leashplayers$getMaxLeashLength();
    }
}
