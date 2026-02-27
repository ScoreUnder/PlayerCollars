package org.jlortiz.playercollars.leash.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.leash.LeashImpl;
import org.jlortiz.playercollars.leash.LeashProxyEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements LeashImpl {
    @Unique
    private static final String playerCollars$LEASH_HOLDER_TAG = "playercollars:leash_holder";

    @Unique
    private static final Codec<Either<UUID, BlockPos>> leashplayers$LEASH_HOLDER_CODEC = Codec.either(Uuids.CODEC, BlockPos.CODEC);

    @Shadow public abstract ServerWorld getServerWorld();

    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Shadow public abstract boolean teleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch, boolean resetCamera);

    @Unique
    private LeashProxyEntity leashplayers$proxy;
    @Unique
    private Entity leashplayers$holder;
    @Unique
    private int leashplayers$lastage;
    @Unique
    private double leashplayer$loyalty;

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique
    private void leashplayers$update() {
        if (
                leashplayers$holder != null && (
                        !leashplayers$holder.isAlive() || !isAlive() || !PlayerCollarsMod.isPet(this)
                )
        ) {
            leashplayers$detach();
            leashplayers$drop();
        }

        if (leashplayers$proxy != null) {
            if (leashplayers$proxy.proxyIsRemoved()) {
                leashplayers$proxy = null;
            }
            else {
                Entity holderActual = leashplayers$holder;
                Entity holderTarget = leashplayers$proxy.getLeashHolder();

                if (holderTarget == null && holderActual != null) {
                    leashplayers$detach();
                    leashplayers$drop();
                }
                else if (holderTarget != holderActual) {
                    leashplayers$attach(holderTarget);
                }
            }
        }

        leashplayers$apply();
    }

    @Unique
    private void leashplayers$apply() {
        Entity holder = leashplayers$holder;
        if (holder == null) return;

        ActionResult result;
        if (holder.getWorld() != getWorld()) {
            result = ActionResult.FAIL;
        } else {
            result = PlayerCollarsMod.applyLeashPull(this, holder.getPos(), leashplayer$loyalty, leashplayer$loyalty + 6);
        }

        if (result == ActionResult.FAIL) {
            if (mustTeleportToHolder(holder)) {
                teleport((ServerWorld) holder.getWorld(), holder.getX(), holder.getY(), holder.getZ(), Set.of(), holder.getYaw(), getPitch(), true);
            } else {
                leashplayers$detach();
                leashplayers$drop();
            }
        }
    }

    @Unique
    private boolean mustTeleportToHolder(@NotNull Entity holder) {
        ServerWorld myWorld = getServerWorld();
        if (!myWorld.getGameRules().getBoolean(PlayerCollarsMod.PLAYER_LEASHES_BREAK_RULE)) return true;
        if (age == 0) return false;
        return holder.getWorld() != myWorld;
    }

    @Unique
    private void leashplayers$attachToBlock(BlockPos pos) {
        var world = getServerWorld();
        var leashKnotEntity = LeashKnotEntity.getOrCreate(world, pos);
        leashKnotEntity.onPlace();
        leashplayers$attach(leashKnotEntity);
    }

    @Unique
    private void leashplayers$attach(Entity entity) {
        leashplayer$loyalty = getAttributeValue(PlayerCollarsMod.ATTR_LEASH_DISTANCE);
        leashplayers$holder = entity;

        if (leashplayers$proxy == null) {
            leashplayers$proxy = new LeashProxyEntity(this);
            getWorld().spawnEntity(leashplayers$proxy);
        }
        leashplayers$proxy.attachLeash(leashplayers$holder, true);

        if (this.hasVehicle() && !this.getServerWorld().getGameRules().getBoolean(PlayerCollarsMod.LEASHED_PLAYERS_RIDE_ENTITIES)) {
            this.stopRiding();
        }

        leashplayers$lastage = age;
    }

    @Unique
    private void leashplayers$detach() {
        leashplayers$holder = null;

        if (leashplayers$proxy != null) {
            if (leashplayers$proxy.isAlive() || !leashplayers$proxy.proxyIsRemoved()) {
                leashplayers$proxy.proxyRemove();
            }
            leashplayers$proxy = null;
        }
    }

    @Unique
    private void leashplayers$drop() {
        dropItem(getServerWorld(), Items.LEAD);
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void leashplayers$tick(CallbackInfo info) {
        leashplayers$update();
    }

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("HEAD"), cancellable = true)
    private void leashplayers$startriding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {

        boolean isLeashed = this.leashplayers$getProxyLeashHolder() != null;
        boolean disallowMount = !this.getServerWorld().getGameRules().getBoolean(PlayerCollarsMod.LEASHED_PLAYERS_RIDE_ENTITIES);

        if (isLeashed && disallowMount) {
            this.sendMessage(Text.translatable("message.playercollars.no_ride_entity"), true);
            cir.cancel();
        }
    }

    @Unique
    private static boolean leashplayers$isLeashableBlock(BlockState blockState) {
        return blockState.isIn(BlockTags.FENCES);
    }

    @Inject(method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    public void leashplayers$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        NbtElement leashHolderNbt = nbt.get(playerCollars$LEASH_HOLDER_TAG);
        if (leashHolderNbt != null) {
            leashplayers$LEASH_HOLDER_CODEC.parse(NbtOps.INSTANCE, leashHolderNbt).ifSuccess(newHolder ->
                    newHolder.ifLeft(uuid -> {
                        var oldHolder = getServerWorld().getPlayerByUuid(uuid);
                        if (isAlive() && oldHolder != null && oldHolder.isAlive()) {
                            leashplayers$attach(oldHolder);
                        } else {
                            leashplayers$drop();
                        }
                    }).ifRight(blockPos -> {
                        if (leashplayers$isLeashableBlock(getServerWorld().getBlockState(blockPos))) {
                            leashplayers$attachToBlock(blockPos);
                        } else {
                            leashplayers$drop();
                        }
                    }));
        }
    }

    @Inject(method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    public void leashplayers$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        Entity leashHolder = leashplayers$holder;
        if (leashHolder instanceof LeashKnotEntity knot) {
            nbt.put(playerCollars$LEASH_HOLDER_TAG,
                    leashplayers$LEASH_HOLDER_CODEC.encodeStart(NbtOps.INSTANCE, Either.right(knot.getAttachedBlockPos())).getOrThrow());
        } else if (leashHolder != null) {
            nbt.put(playerCollars$LEASH_HOLDER_TAG,
                    leashplayers$LEASH_HOLDER_CODEC.encodeStart(NbtOps.INSTANCE, Either.left(leashHolder.getUuid())).getOrThrow());
        }
    }

    @Override
    public Entity leashplayers$getProxyLeashHolder() {
        return leashplayers$proxy == null ? null : leashplayers$proxy.getLeashHolder();
    }

    @Override
    public ActionResult leashplayers$interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() == Items.LEAD && leashplayers$holder == null) {
            if (!PlayerCollarsMod.getOwnershipLevel(this, player).isOwned()) return ActionResult.PASS;
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            leashplayers$attach(player);
            return ActionResult.SUCCESS;
        }

        if (leashplayers$holder == player && leashplayers$lastage + 20 < age) {
            if (!player.isCreative()) {
                leashplayers$drop();
            }
            leashplayers$detach();
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}