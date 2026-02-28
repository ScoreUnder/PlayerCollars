package org.jlortiz.playercollars.mixin.crawlrendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.jlortiz.playercollars.accessor.BipedRenderExtensions;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class) @Environment(EnvType.CLIENT)
public abstract class BipedEntityModelMixin<T extends BipedEntityRenderState> {
    @Unique private static final float PI = 3.14159265f;
    @Unique private static final float LIMB_SPEED_FACTOR = 0.8f;
    @Unique private static final float LIMB_AMPLITUDE_FACTOR = 0.8f;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftLeg;
    @Shadow @Final public ModelPart rightLeg;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;preferredArm:Lnet/minecraft/util/Arm;", opcode = Opcodes.GETFIELD, ordinal = 0, shift = At.Shift.BY, by = -11), cancellable = true)
    private void setLimbAngles(T state, CallbackInfo ci) {
        if (state instanceof BipedRenderExtensions ext && ext.playerCollars$isCrawlingWithPaws()) {
            playerCollars$setPetCrawlPose(state);
            ci.cancel();
        }
    }

    @Unique
    private void playerCollars$setPetCrawlPose(T state) {
        float amplitude = LIMB_AMPLITUDE_FACTOR * state.limbAmplitudeMultiplier;
        float frequency = state.limbFrequency * LIMB_SPEED_FACTOR;
        float limbPitch = MathHelper.cos(frequency) * amplitude;
        float leaningPitch = state.leaningPitch;

        if (!state.isUsingItem) {
            boolean isSwingingArm = state.handSwingProgress > 0.0F;
            playerCollars$setPetCrawlArmPose(this.leftArm, this.rightArm, state.preferredArm, leaningPitch, isSwingingArm, limbPitch);
        }

        playerCollars$setPetCrawlLegPose(this.leftLeg, this.rightLeg, leaningPitch, limbPitch);
    }

    @Unique
    private static void playerCollars$setPetCrawlLegPose(ModelPart leftLeg, ModelPart rightLeg, float leaningPitch, float limbPitch) {
        leftLeg.pitch = MathHelper.lerp(leaningPitch, leftLeg.pitch, -PI / 2 - limbPitch);
        rightLeg.pitch = MathHelper.lerp(leaningPitch, rightLeg.pitch, -PI / 2 + limbPitch);
    }

    @Unique
    private static void playerCollars$setPetCrawlArmPose(ModelPart leftArm, ModelPart rightArm, Arm preferredArm, float leaningPitch, boolean isSwingingArm, float limbPitch) {
        float rightSwingOffset = preferredArm == Arm.RIGHT && isSwingingArm ? 0.0F : leaningPitch;
        float leftSwingOffset = preferredArm == Arm.LEFT && isSwingingArm ? 0.0F : leaningPitch;
        leftArm.pitch = MathHelper.lerpAngleRadians(leftSwingOffset, leftArm.pitch, PI / 2 + limbPitch);
        rightArm.pitch = MathHelper.lerp(rightSwingOffset, rightArm.pitch, PI / 2 - limbPitch);
        leftArm.yaw = MathHelper.lerpAngleRadians(leftSwingOffset, leftArm.yaw, PI);
        rightArm.yaw = MathHelper.lerp(rightSwingOffset, rightArm.yaw, PI);
        leftArm.roll = MathHelper.lerpAngleRadians(leftSwingOffset, leftArm.roll, PI);
        rightArm.roll = MathHelper.lerp(rightSwingOffset, rightArm.roll, PI);
    }
}
