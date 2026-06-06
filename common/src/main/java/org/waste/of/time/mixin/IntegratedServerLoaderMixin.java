package org.waste.of.time.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.waste.of.time.WorldTools;

@Mixin(WorldOpenFlows.class)
public class IntegratedServerLoaderMixin {

    @WrapOperation(method = "openWorldCheckWorldStemCompatibility",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;askForBackup(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;ZLjava/lang/Runnable;Ljava/lang/Runnable;)V"
            ))
    public void disableExperimentalWorldSettingsScreen(WorldOpenFlows instance, LevelStorageSource.LevelStorageAccess session, boolean customized, Runnable callback, Runnable onCancel, Operation<Void> original) {
        if (WorldTools.INSTANCE.getConfig().getAdvanced().getHideExperimentalWorldGui()) {
            callback.run();
        } else {
            original.call(instance, session, customized, callback, onCancel);
        }
    }
}
