package org.jlortiz.playercollars.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.jlortiz.playercollars.item.PawsItem;

public class PawsEventHandler {
    public static void registerPawsEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) ->
                PawsItem.canInteractWithPaws(player, world, pos, true));

        UseBlockCallback.EVENT.register((PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) -> {
            if (player.isSpectator()) return ActionResult.PASS;
            if (PawsItem.canInteractWithPaws(player, world, hitResult.getBlockPos(), true)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });
    }
}
