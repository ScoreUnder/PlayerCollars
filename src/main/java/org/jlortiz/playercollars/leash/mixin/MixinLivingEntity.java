package org.jlortiz.playercollars.leash.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.jlortiz.playercollars.leash.LeashHeldByProxyImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    private MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "canUsePortals", at = @At("TAIL"), cancellable = true)
    private void disableLeashedPortalUse(boolean allowVehicles, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (this instanceof LeashHeldByProxyImpl impl) {
            Entity leashHolder = impl.playerCollars$getRealLeashHolder();
            if (leashHolder == null) return;

            cir.setReturnValue(false);
        }
    }
}
