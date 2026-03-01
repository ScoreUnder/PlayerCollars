package org.jlortiz.playercollars.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
@Environment(EnvType.CLIENT)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {
    private ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D"), allow = 1)
    private double getSneakingOrCrawlingSpeed(double original) {
        if (!PlayerCollarsMod.isWalkingOnAllFours(this))
            return original;
        var pawFactor = PlayerCollarsMod.getFootPawsCrawlingSpeed();
        if (isSneaking()) return original * pawFactor;
        return pawFactor;
    }
}
