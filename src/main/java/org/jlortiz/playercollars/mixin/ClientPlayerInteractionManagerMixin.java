package org.jlortiz.playercollars.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import static org.jlortiz.playercollars.item.PawsItem.canInteractWithPaws;

@Mixin(ClientPlayerInteractionManager.class)
@Environment(EnvType.CLIENT)
public class ClientPlayerInteractionManagerMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @ModifyExpressionValue(method = "attackBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameMode;isCreative()Z"))
    private boolean canBreakAsCreative(boolean result, BlockPos pos, Direction direction) {
        if (!result) return false;
        //noinspection DataFlowIssue // player and world can't be null if we're doing this
        return canInteractWithPaws(client.player, client.world, pos, true);
    }

    @ModifyExpressionValue(method = "updateBlockBreakingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameMode;isCreative()Z"))
    private boolean canBreakAsCreative2(boolean result, BlockPos pos, Direction direction) {
        if (!result) return false;
        //noinspection DataFlowIssue // player and world can't be null if we're doing this
        return canInteractWithPaws(client.player, client.world, pos, true);
    }
}
