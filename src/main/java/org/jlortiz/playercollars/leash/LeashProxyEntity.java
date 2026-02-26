package org.jlortiz.playercollars.leash;

import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathConstants;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.joml.Math;

import java.util.Objects;
import java.util.OptionalInt;

import static org.jlortiz.playercollars.leash.LeashServerSideInit.LEASH_PROXY_ENTITY_TYPE;

public final class LeashProxyEntity extends TurtleEntity {
    private static final TrackedData<OptionalInt> TRACKED_LEASH_TARGET =
            DataTracker.registerData(LeashProxyEntity.class, TrackedDataHandlerRegistry.OPTIONAL_INT);

    private LivingEntity target;
    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(MathConstants.EPSILON, MathConstants.EPSILON);

    public LeashProxyEntity(EntityType<? extends LeashProxyEntity> type, World world) {
        super(type, world);
        setHealth(1.0F);
        setInvulnerable(true);
        setBaby(true);
        setInvisible(true);
        noClip = true;
        this.target = this;  // better than nothing? lmao
    }

    public LeashProxyEntity(@NotNull LivingEntity target) {
        this(LEASH_PROXY_ENTITY_TYPE, target.getWorld());

        this.target = target;
        setRealLeashTargetId(OptionalInt.of(target.getId()));

        proxyUpdate();
    }

    private boolean proxyUpdate() {
        if (proxyIsRemoved()) return false;

        if (target == null) return true;
        if (target.getWorld() != getWorld() || !target.isAlive()) return true;

        Vec3d posActual = this.getPos();
        Vec3d posTarget = getTargetPos(target);

        if (!Objects.equals(posActual, posTarget)) {
            setRotation(0.0F, 0.0F);
            setPos(posTarget.x, posTarget.y, posTarget.z);
            setBoundingBox(DIMENSIONS.getBoxAt(target.getPos()));
        }

        return false;
    }

    public static Vec3d getTargetPos(LivingEntity target) {
        Vec3d posTarget = PlayerCollarsMod.isWalkingOnAllFours(target)
                ? Vec3d.fromPolar(0, target.getBodyYaw()).multiply(0.35).add(0f, 0.2f + 0.375f, -0.1f)
                : switch (target.getPose()) {
                    // No point in making cases for SPIN_ATTACK since leashed players can't use it
                    case CROUCHING: yield new Vec3d(0.0D, 1.1D, -0.15D);
                    case SWIMMING: yield Vec3d.fromPolar(0, target.getBodyYaw()).multiply(0.35).add(0, 0.2, -0.1);
                    case GLIDING: yield new Vec3d(0, 1.3, -0.15).rotateX(-Math.toRadians(90 + target.getPitch()))
                            .rotateY(-Math.toRadians(target.getBodyYaw()));
                    case SLEEPING: if (target.getSleepingDirection() != null)
                            yield new Vec3d(target.getSleepingDirection().getUnitVector().mul(-0.2f)).add(0, 0.1, -0.15);
                    default: yield new Vec3d(0.0D, 1.3D, -0.15D);
                };
        posTarget = posTarget.multiply(target.getScale()).add(target.getPos());
        return posTarget;
    }

    @NotNull
    public LivingEntity getLeashTarget() {
        return target;
    }

    @Override
    public void tick() {
        if (this.getWorld().isClient) {
            clientSideSync();
        } else {
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
    public float getHealth() {
        return 1.0F;
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
    protected void initGoals() {
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    @Override
    public void pushAwayFrom(Entity entity) {
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
    }

    @Override
    public void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TRACKED_LEASH_TARGET, OptionalInt.empty());
    }

    private void clientSideSync() {
        // On client-side, we should update the entity position even if the server hasn't asked us.
        // this prevents the leash from visibly lagging.
        if (target == null || target.isRemoved()) return;

        var targetPos = LeashProxyEntity.getTargetPos(target);
        setPos(targetPos.x, targetPos.y, targetPos.z);
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