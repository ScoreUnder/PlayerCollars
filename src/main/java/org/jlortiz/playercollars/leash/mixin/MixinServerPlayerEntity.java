package org.jlortiz.playercollars.leash.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.leash.LeashServerSideImpl;
import org.jlortiz.playercollars.leash.LeashProxyEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements LeashServerSideImpl {
    @Unique
    private static final String playerCollars$LEASH_HOLDER_TAG = "playercollars:leash_holder";

    @Unique
    private static final Codec<Either<UUID, GlobalPos>> leashplayers$LEASH_HOLDER_CODEC = Codec.either(Uuids.CODEC, GlobalPos.CODEC);

    @Shadow public abstract ServerWorld getServerWorld();

    @Shadow public abstract boolean teleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch, boolean resetCamera);

    @Unique
    private Either<UUID, GlobalPos> leashplayers$leashInfo;
    @Unique
    private LeashProxyEntity leashplayers$proxy;
    @Unique
    private int leashplayers$lastage;
    @Unique
    private double leashplayer$loyalty;

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique
    private void leashplayers$update() {
        if (leashplayers$leashInfo == null) return;

        Entity holder = playerCollars$getRealLeashHolder();
        if (holder == null) {
            leashplayers$detachAndDrop();
            return;
        }

        if (holder.getWorld() != getWorld()) {
            leashplayers$onTooLongLeash(holder);
        } else if (!holder.isAlive() || !isAlive() || !PlayerCollarsMod.isPet(this)) {
            leashplayers$detachAndDrop();
        } else {
            ActionResult result = PlayerCollarsMod.applyLeashPull(this, holder.getPos(), leashplayers$getLeashPullLength(), leashplayers$getMaxLeashLength());
            if (result == ActionResult.FAIL) {
                leashplayers$onTooLongLeash(holder);
            }
        }
    }

    @Unique
    private void leashplayers$onTooLongLeash(@NotNull Entity holder) {
        if (playerCollars$mustTeleportToHolder(holder)) {
            ServerWorld holderWorld = (ServerWorld) holder.getWorld();
            World myWorld = getWorld();

            Vec3d telePos = leashplayers$getTeleportToHolderPos(holder, holderWorld, myWorld);

            teleport(holderWorld, telePos.getX(), telePos.getY(), telePos.getZ(), Set.of(), holder.getYaw(), getPitch(), true);
            if (holderWorld != myWorld) {
                leashplayers$killLeashProxy();
                leashplayers$refreshLeashProxy(holder);
            }
        } else {
            leashplayers$detachAndDrop();
        }
    }

    @Unique
    private Vec3d leashplayers$getTeleportToHolderPos(@NotNull Entity holder, ServerWorld holderWorld, World myWorld) {
        Vec3d desiredPos = holder.getPos();
        if (holderWorld == myWorld) {
            // Try to spawn near to owner if we are just somehow pulling too far on the leash
            Vec3d directionXZ = getPos().subtract(desiredPos);
            directionXZ = new Vec3d(directionXZ.getX(), desiredPos.getY(), directionXZ.getZ()).normalize();
            Vec3d nearbyPos = desiredPos.add(directionXZ);
            var collisions = holderWorld.getBlockCollisions(this, this.getBoundingBox().offset(nearbyPos));
            if (!collisions.iterator().hasNext()) {
                return nearbyPos;
            }
        }

        // Try to teleport close by like a respawn anchor (seems a little more polite than just appearing inside someone)
        Optional<Vec3d> x = RespawnAnchorBlock.findRespawnPosition(EntityType.PLAYER, holderWorld, holder.getBlockPos().down());
        if (x.isPresent()) {
            Vec3d pos = x.get();
            if (pos.distanceTo(desiredPos) < leashplayers$getLeashPullLength())
                return pos;
        }

        return desiredPos;
    }

    @Unique
    private boolean playerCollars$mustTeleportToHolder(@NotNull Entity holder) {
        ServerWorld myWorld = getServerWorld();
        if (!myWorld.getGameRules().getBoolean(PlayerCollarsMod.PLAYER_LEASHES_BREAK_RULE)) return true;
        if (age == 0) return false;
        return holder.getWorld() != myWorld;
    }

    @Unique
    private Entity leashplayers$createEntityForBlockLeashHolder(GlobalPos pos) {
        MinecraftServer server = getServer();
        if (server == null) return null;
        ServerWorld world = server.getWorld(pos.dimension());
        if (world == null) return null;
        if (!leashplayers$isLeashableBlock(getServerWorld().getBlockState(pos.pos()))) return null;
        return LeashKnotEntity.getOrCreate(world, pos.pos());
    }

    @Override
    public void leashplayers$attach(Entity entity) {
        leashplayer$loyalty = getAttributeValue(PlayerCollarsMod.ATTR_LEASH_DISTANCE);
        leashplayers$refreshLeashProxy(entity);

        if (this.hasVehicle() && !this.getServerWorld().getGameRules().getBoolean(PlayerCollarsMod.LEASHED_PLAYERS_RIDE_ENTITIES)) {
            this.stopRiding();
        }

        leashplayers$lastage = age;
    }

    @Override
    public void leashplayers$onLeashTransfer(Entity leashHolder) {
        if (leashHolder instanceof LeashKnotEntity knot) {
            leashplayers$leashInfo = Either.right(GlobalPos.create(knot.getWorld().getRegistryKey(), knot.getAttachedBlockPos()));
        } else {
            leashplayers$leashInfo = Either.left(leashHolder.getUuid());
        }
    }

    @Unique
    private void leashplayers$refreshLeashProxy(@NotNull Entity holder) {
        if (leashplayers$proxy != null && leashplayers$proxy.isRemoved()) {
            leashplayers$proxy = null;
        }
        if (leashplayers$proxy == null) {
            leashplayers$proxy = new LeashProxyEntity(this);
            getWorld().spawnEntity(leashplayers$proxy);
        }
        leashplayers$proxy.attachLeash(holder, true);
    }

    @Unique
    private void leashplayers$detachAndDrop() {
        leashplayers$drop();
        leashplayers$detach();
    }

    @Unique
    private void leashplayers$detach() {
        leashplayers$leashInfo = null;
        leashplayers$killLeashProxy();
    }

    @Unique
    private void leashplayers$killLeashProxy() {
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
        boolean isLeashed = leashplayers$leashInfo != null;
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
            leashplayers$LEASH_HOLDER_CODEC.parse(NbtOps.INSTANCE, leashHolderNbt)
                    .ifSuccess(newHolder -> leashplayers$leashInfo = newHolder);
        }
    }

    @Inject(method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    public void leashplayers$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (leashplayers$leashInfo != null) {
            nbt.put(playerCollars$LEASH_HOLDER_TAG,
                    leashplayers$LEASH_HOLDER_CODEC.encodeStart(NbtOps.INSTANCE, leashplayers$leashInfo).getOrThrow());
        }
    }

    @Unique
    private void leashplayers$resolveLeashHolder(@NotNull Either<UUID, GlobalPos> holderInfo) {
        Entity holder = leashplayers$forceResolveLeashHolder(holderInfo);
        if (holder != null && holder.getWorld() == getWorld())
            leashplayers$attach(holder);
    }

    @Unique
    private Entity leashplayers$forceResolveLeashHolder(@NotNull Either<UUID, GlobalPos> holderInfo) {
        MinecraftServer server = getServer();
        if (server == null) return null;
        return holderInfo.map(server.getPlayerManager()::getPlayer, this::leashplayers$createEntityForBlockLeashHolder);
    }

    @Override
    public ActionResult leashplayers$interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Entity existingLeashHolder = playerCollars$getRealLeashHolder();
        if (stack.getItem() == Items.LEAD && existingLeashHolder == null) {
            if (!PlayerCollarsMod.getOwnershipLevel(this, player).isOwned()) return ActionResult.PASS;
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            leashplayers$attach(player);
            return ActionResult.SUCCESS;
        }

        if (existingLeashHolder == player && leashplayers$lastage + 20 < age) {
            if (!player.isCreative()) {
                leashplayers$drop();
            }
            leashplayers$detach();
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public double leashplayers$getLeashPullLength() {
        return leashplayer$loyalty;
    }

    @Override
    public double leashplayers$getMaxLeashLength() {
        return leashplayer$loyalty + 6;
    }

    @Override
    public LeashProxyEntity playerCollars$getLeashProxy() {
        Either<UUID, GlobalPos> leashInfo = leashplayers$leashInfo;
        if (leashInfo == null) return null;
        LeashProxyEntity proxy = leashplayers$proxy;
        if (!playerCollars$proxyIsStale(proxy)) return proxy;

        leashplayers$killLeashProxy();
        leashplayers$resolveLeashHolder(leashInfo);
        return leashplayers$proxy;
    }

    @Override
    public void playerCollars$setLeashProxy(LeashProxyEntity value) {
        leashplayers$proxy = value;
    }

    @Unique
    private static boolean playerCollars$proxyIsStale(LeashProxyEntity proxy) {
        if (proxy == null || proxy.isRemoved()) return true;
        Entity leashHolder = proxy.getLeashHolder();
        return leashHolder == null || leashHolder.isRemoved();
    }

    @Override
    public @Nullable Entity playerCollars$getRealLeashHolder() {
        Either<UUID, GlobalPos> leashInfo = leashplayers$leashInfo;
        if (leashInfo == null) return null;
        var holder = LeashServerSideImpl.super.playerCollars$getRealLeashHolder();
        if (holder != null) return holder;
        return leashplayers$forceResolveLeashHolder(leashInfo);
    }
}