package org.waste.of.time.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.waste.of.time.Events;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayInteractionManagerMixin {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "useItemOn", at = @At("HEAD"))
    public void interactBlockHead(final LocalPlayer player, final InteractionHand hand, final BlockHitResult hitResult, final CallbackInfoReturnable<InteractionResult> cir) {
        if (minecraft.level == null) return;
        Events.INSTANCE.onInteractBlock(minecraft.level, hitResult);
    }

    @Inject(method = "interact", at = @At("HEAD"))
    public void interactEntityHead(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (minecraft.level == null) return;
        Events.INSTANCE.onInteractEntity(entity);
    }
}
