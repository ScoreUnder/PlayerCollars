package org.jlortiz.playercollars.mixin.crawlrendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.jlortiz.playercollars.PlayerCollarsMod.PAWS_TAG;
import static org.jlortiz.playercollars.PlayerCollarsMod.getEquippedAccessories;

@Mixin(ClientPlayerEntity.class) @Environment(EnvType.CLIENT) public abstract class ClientPlayerEntityMixin {
    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D"))
    private static double getSneakingOrCrawlingSpeed(ClientPlayerEntity instance, RegistryEntry<EntityAttribute> registryEntry) {
        if (registryEntry != EntityAttributes.SNEAKING_SPEED || getEquippedAccessories(instance, PAWS_TAG).isEmpty())
            return instance.getAttributeValue(registryEntry);
        return PlayerCollarsMod.getFootPawsCrawlingSpeed();
    }
}
