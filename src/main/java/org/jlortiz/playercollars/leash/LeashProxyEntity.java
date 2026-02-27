package org.jlortiz.playercollars.leash;

import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathConstants;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.OptionalInt;

import static org.jlortiz.playercollars.leash.LeashServerSideInit.LEASH_PROXY_ENTITY_TYPE;

public final class LeashProxyEntity extends Entity implements Leashable {
    private static final TrackedData<OptionalInt> TRACKED_LEASH_TARGET =
            DataTracker.registerData(LeashProxyEntity.class, TrackedDataHandlerRegistry.OPTIONAL_INT);
    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(MathConstants.EPSILON, MathConstants.EPSILON);

    private @Nullable LivingEntity target;
    private @Nullable Leashable.LeashData leashData;

    public LeashProxyEntity(EntityType<? extends LeashProxyEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        setInvisible(true);
        noClip = true;
    }

    public LeashProxyEntity(@NotNull LivingEntity target) {
        this(LEASH_PROXY_ENTITY_TYPE, target.getWorld());

        this.target = target;
        if (target instanceof LeashHeldByProxyImpl impl) {
            impl.playerCollars$setLeashProxy(this);
        }
        setRealLeashTargetId(OptionalInt.of(target.getId()));

        proxyUpdate();
    }

    private boolean proxyUpdate() {
        if (proxyIsRemoved()) return false;

        if (target == null) return true;
        if (target.getWorld() != getWorld() || !target.isAlive()) return true;

        Vec3d posActual = this.getPos();
        Vec3d posTarget = target.getPos();

        if (!Objects.equals(posActual, posTarget)) {
            setRotation(0.0F, 0.0F);
            setPos(posTarget.x, posTarget.y, posTarget.z);
            setBoundingBox(DIMENSIONS.getBoxAt(target.getPos()));
        }

        return false;
    }

    @Nullable  // Nullable on client, non-nullable on server
    public LivingEntity getLeashTarget() {
        return target;
    }

    @Override
    public void tick() {
        if (!this.getWorld().isClient) {
            if (proxyUpdate() && !proxyIsRemoved()) {
                proxyRemove();
            }
        }
    }

    public boolean proxyIsRemoved() {
        return this.isRemoved();
    }

    public void proxyRemove() {
        super.remove(RemovalReason.DISCARDED);
    }

    @Override
    public void remove(RemovalReason reason) {
    }

    @Override
    public boolean collidesWithStateAtPos(BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void detachLeash() {
    }

    @Override
    public void detachLeashWithoutDrop() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void pushAwayFrom(Entity entity) {
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canUsePortals(boolean allowVehicles) {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return shouldRender(0.0);
    }

    @Override
    public boolean shouldRender(double distance) {
        Entity holder = getLeashHolder();
        if (holder == null || target == null) return false;
        return holder.shouldRender(distance) || target.shouldRender(distance);
    }

    @Override
    public void initDataTracker(DataTracker.Builder builder) {
        builder.add(TRACKED_LEASH_TARGET, OptionalInt.empty());
    }

    @Override
    public @Nullable LeashData getLeashData() {
        return this.leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData leashData) {
        this.leashData = leashData;
    }

    private void refreshTarget() {
        var leashTarget = dataTracker.get(TRACKED_LEASH_TARGET);
        if (leashTarget.isEmpty()) {
            this.target = null;
            return;
        }

        var leashTargetEntity = getWorld().getEntityById(leashTarget.getAsInt());
        if (!(leashTargetEntity instanceof LivingEntity newTarget)) {
            this.target = null;
            return;
        }

        this.target = newTarget;
        if (target instanceof LeashHeldByProxyImpl impl) {
            impl.playerCollars$setLeashProxy(this);
        }
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (TRACKED_LEASH_TARGET.equals(data) && getWorld().isClient) {
            refreshTarget();
        } else {
            super.onTrackedDataSet(data);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void setRealLeashTargetId(OptionalInt val) {
        dataTracker.set(TRACKED_LEASH_TARGET, val);
    }
}