package org.jlortiz.playercollars.mixin.crawlrendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import org.jlortiz.playercollars.accessor.BipedRenderExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BipedEntityRenderState.class) @Environment(EnvType.CLIENT)
public abstract class BipedEntityRenderStateMixin implements BipedRenderExtensions {
    @Unique private boolean isCrawlingWithPaws;

    @Override public void playerCollars$setCrawlingWithPaws(boolean value) {
        isCrawlingWithPaws = value;
    }

    @Override public boolean playerCollars$isCrawlingWithPaws() {
        return isCrawlingWithPaws;
    }
}
