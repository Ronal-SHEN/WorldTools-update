package org.waste.of.time.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.waste.of.time.Events;

@Mixin(PauseScreen.class)
public class GameMenuScreenMixin {

    @Inject(method = "createPauseMenu", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"
    ))
    public void onInitWidgets(final CallbackInfo ci,
                              @Local GridLayout.RowHelper adder) {
        Events.INSTANCE.onGameMenuScreenInitWidgets(adder);
    }
}
