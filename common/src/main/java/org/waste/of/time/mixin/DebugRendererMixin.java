package org.waste.of.time.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.Events;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
    @Inject(method = "emitGizmos", at = @At("HEAD"))
    public void emitGizmosInject(
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ,
            float tickDelta,
            CallbackInfo ci
    ) {
        Events.INSTANCE.onDebugRenderStart();
    }
}
