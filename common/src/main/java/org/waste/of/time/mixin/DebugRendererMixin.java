package org.waste.of.time.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.Events;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    public void renderInject(
            PoseStack matrices,
            Frustum frustum,
            MultiBufferSource.BufferSource vertexConsumers,
            double cameraX,
            double cameraY,
            double cameraZ,
            boolean renderBlockOutline,
            CallbackInfo ci
    ) {
        Events.INSTANCE.onDebugRenderStart(matrices, vertexConsumers, cameraX, cameraY, cameraZ);
    }
}
