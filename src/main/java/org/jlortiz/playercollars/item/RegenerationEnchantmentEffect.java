package org.jlortiz.playercollars.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jlortiz.playercollars.OwnerComponent;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.List;

public record RegenerationEnchantmentEffect(EnchantmentLevelBasedValue level) implements EnchantmentEntityEffect {
    public static final MapCodec<RegenerationEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                EnchantmentLevelBasedValue.CODEC.fieldOf("level").forGetter(RegenerationEnchantmentEffect::level)
            ).apply(instance, RegenerationEnchantmentEffect::new));

    @Override
    public void apply(ServerWorld world, int enchantLevel, EnchantmentEffectContext context, Entity user, Vec3d pos) {
        LivingEntity player = context.owner();
        if (player == null) return;
        if (player.age % 20 != 0) return; // Perform checks only once every few ticks. No need to be especially precise.
        List<SlotEntryReference> ls = PlayerCollarsMod.getEquippedCollars(player);
        for (SlotEntryReference p : ls) {
            OwnerComponent oc = p.stack().get(PlayerCollarsMod.OWNER_COMPONENT_TYPE);
            if (oc == null) continue;

            PlayerEntity own = world.getPlayerByUuid(oc.uuid());
            if (own == null || own.squaredDistanceTo(user) >= 16 * 16) continue;

            onOwnerNearby(player, enchantLevel);
            return;
        }
    }

    private void onOwnerNearby(LivingEntity player, int enchantLevel) {
        int effectLevel = Math.round(level.getValue(enchantLevel));
        StatusEffectInstance existingRegenEffect = player.getStatusEffect(StatusEffects.REGENERATION);
        int duration = existingRegenEffect == null || existingRegenEffect.getAmplifier() != effectLevel ? 40 : existingRegenEffect.getDuration();
        if (duration < 50) {
            // Keep duration above 50 if possible
            // 'Regeneration I' activates once every 50 ticks, but it uses its remaining duration to figure
            // out when to activate. If duration is <50, it never activates.
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration + 50, effectLevel, false, false, false));
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> getCodec() {
        return CODEC;
    }
}
