package org.jlortiz.playercollars.leash;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public interface LeashServerSideImpl extends LeashHeldByProxyImpl {
    ActionResult leashplayers$interact(PlayerEntity plr, Hand hand);

    double leashplayers$getLeashPullLength();

    double leashplayers$getMaxLeashLength();

    void leashplayers$attach(Entity entity);

    void leashplayers$onLeashTransfer(Entity leashHolder);
}
