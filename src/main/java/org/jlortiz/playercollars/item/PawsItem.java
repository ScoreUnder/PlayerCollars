package org.jlortiz.playercollars.item;

import com.mojang.datafixers.util.Either;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.NotNull;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.network.PawsPermissionData;

import java.util.List;
import java.util.Optional;

public class PawsItem extends FootPawsItem {
    public PawsItem(RegistryKey<Item> key, int color, int pawColor) {
        super(key, color, pawColor);
    }

    public static boolean shouldPreventBlockInteraction(ItemStack stack, @NotNull BlockState block, boolean isBreak) {
        if (block.isIn(isBreak ? PlayerCollarsMod.PAWS_ALLOW_BREAK : PlayerCollarsMod.PAWS_ALLOW_INTERACT)) return false;
        PawsPermissionData<Block> allowed = stack.get(isBreak ? PlayerCollarsMod.CAN_BREAK_COMPONENT_TYPE : PlayerCollarsMod.CAN_INTERACT_COMPONENT_TYPE);
        Optional<RegistryKey<Block>> key = block.getRegistryEntry().getKey();
        if (allowed == null || key.isEmpty()) return false;
        for (Either<TagKey<Block>, RegistryKey<Block>> entry : allowed.permittedList()) {
            if (entry.map(block::isIn, (y) -> y.equals(key.get()))) return allowed.isDenyList();
        }
        return !allowed.isDenyList();
    }

    public static boolean canInteractWithPaws(@NotNull LivingEntity player, @NotNull BlockView world, @NotNull BlockPos pos, boolean isBreak) {
        List<SlotEntryReference> equippedPaws = PlayerCollarsMod.getEquippedAccessories(player, PlayerCollarsMod.PAWS_TAG);
        if (equippedPaws.isEmpty()) return true;
        var state = world.getBlockState(pos);
        for (var paw : equippedPaws) {
            if (shouldPreventBlockInteraction(paw.stack(), state, isBreak)) return false;
        }
        return true;
    }

    public static boolean shouldDrop(ItemStack pawsStack, ItemStack thing) {
        if (thing.isEmpty()) return false;
        PawsPermissionData<Item> slippery = pawsStack.get(PlayerCollarsMod.HELD_ITEMS_COMPONENT_TYPE);
        Optional<RegistryKey<Item>> key = thing.getRegistryEntry().getKey();
        if (slippery == null || key.isEmpty()) return false;
        for (Either<TagKey<Item>, RegistryKey<Item>> entry : slippery.permittedList()) {
            if (entry.map(thing::isIn, (y) -> y.equals(key.get()))) return slippery.isDenyList();
        }
        return !slippery.isDenyList();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        if (stack.get(PlayerCollarsMod.HELD_ITEMS_COMPONENT_TYPE) != null) tooltip.add(Text.translatable("item.playercollars.paws.slippery"));
        if (stack.get(PlayerCollarsMod.CAN_INTERACT_COMPONENT_TYPE) != null || stack.get(PlayerCollarsMod.CAN_BREAK_COMPONENT_TYPE) != null) tooltip.add(Text.translatable("item.playercollars.paws.interaction"));
    }

    public static RegistryKey<Item> getRegistryKey(DyeColor c) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, c.getName() + "_paws"));
    }
}
