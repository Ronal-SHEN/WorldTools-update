package org.waste.of.time.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.manager.CaptureManager;
import org.waste.of.time.storage.serializable.StatisticStoreable;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleAwardStats", at = @At("RETURN"))
    private void onStatistics(ClientboundAwardStatsPacket packet, CallbackInfo ci) {
        if (!CaptureManager.INSTANCE.getCapturing()) return;
        new StatisticStoreable().emit();
    }
}
