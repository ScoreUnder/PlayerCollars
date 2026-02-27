package org.jlortiz.playercollars.leash;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

class LeashProxyEntityRenderer extends EntityRenderer<LeashProxyEntity, EntityRenderState> {
    public LeashProxyEntityRenderer(EntityRendererFactory.Context c) {
        super(c);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public boolean shouldRender(LeashProxyEntity entity, Frustum frustum, double x, double y, double z) {
        Entity leashTarget = entity.getLeashTarget();
        Entity leashHolder = entity.getLeashHolder();
        if (leashTarget == null || leashHolder == null) return false;
        return checkVisibility(leashHolder, frustum, x, y, z) || checkVisibility(leashTarget, frustum, x, y, z);
    }

    private boolean checkVisibility(Entity entity, Frustum frustum, double x, double y, double z) {
        return dispatcher.getRenderer(entity).shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void updateRenderState(LeashProxyEntity entity, EntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        EntityRenderState.LeashData leashData = state.leashData;
        Entity leashTarget = entity.getLeashTarget();
        if (leashData != null & leashTarget != null) {
            if (leashTarget == dispatcher.camera.getFocusedEntity() && !dispatcher.camera.isThirdPerson()) {
                updateLeashOffsetInFirstPerson(entity, leashData, tickDelta);
            } else {
                updateLeashOffset(entity, leashTarget, leashData, tickDelta);
            }
        }
    }

    private void updateLeashOffsetInFirstPerson(@NotNull LeashProxyEntity me, @NotNull EntityRenderState.LeashData leashData, float tickDelta) {
        Vec3d cameraPos = dispatcher.camera.getPos();
        Vec3d leashPos = cameraPos.add(0f, -0.2f, 0f);
        leashData.offset = leashPos.subtract(me.getLerpedPos(tickDelta));
        leashData.startPos = leashPos;
    }

    private void updateLeashOffset(@NotNull LeashProxyEntity me, @NotNull Entity leashTarget, @NotNull EntityRenderState.LeashData leashData, float tickDelta) {
        if (!(leashTarget instanceof AbstractClientPlayerEntity player)) return;
        EntityRenderer<?, ?> targetRenderer1 = dispatcher.getRenderer(leashTarget);
        if (!(targetRenderer1 instanceof PlayerEntityRenderer targetRenderer)) return;

        PlayerEntityModel model = targetRenderer.getModel();
        PlayerEntityRenderState targetState = targetRenderer.getAndUpdateRenderState(player, tickDelta);

        MatrixStack matrices = new MatrixStack();
        simulateRender(targetRenderer, targetState, model, matrices);

        model.getRootPart().rotate(matrices);
        model.body.rotate(matrices);

        Vector3f leashAttachmentPointV3f = new Vector3f(0f, 0.1f, -0.15f);
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        positionMatrix.transformPosition(leashAttachmentPointV3f);

        Vec3d leashAttachmentPoint = new Vec3d(leashAttachmentPointV3f);
        leashAttachmentPoint = leashTarget.getLerpedPos(tickDelta).add(leashAttachmentPoint);
        leashData.offset = leashAttachmentPoint.subtract(me.getLerpedPos(tickDelta));
        leashData.startPos = leashAttachmentPoint;
    }

    private static void simulateRender(PlayerEntityRenderer targetRenderer, PlayerEntityRenderState targetState, PlayerEntityModel model, MatrixStack matrices) {
        // begin snippet from LivingEntityRenderer.render()
        // changes:
        // - everything after final updates to matrices and models are removed
        // - all code not directly pertaining to matrix transformation is removed
        if (targetState.isInPose(EntityPose.SLEEPING)) {
            Direction direction = targetState.sleepingDirection;
            if (direction != null) {
                float f = targetState.standingEyeHeight - 0.1F;
                matrices.translate((float) (-direction.getOffsetX()) * f, 0.0F, (float) (-direction.getOffsetZ()) * f);
            }
        }

        float g = targetState.baseScale;
        matrices.scale(g, g, g);
        targetRenderer.setupTransforms(targetState, matrices, targetState.bodyYaw, g);
        matrices.scale(-1.0F, -1.0F, 1.0F);
        targetRenderer.scale(targetState, matrices);
        matrices.translate(0.0F, -1.501F, 0.0F);
        model.setAngles(targetState);
        // end snippet from LivingEntityRenderer.render()
    }
}
