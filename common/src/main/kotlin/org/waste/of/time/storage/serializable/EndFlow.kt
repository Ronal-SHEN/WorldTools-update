package org.waste.of.time.storage.serializable

import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.level.storage.LevelStorageSource
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager.infoToast
import org.waste.of.time.manager.MessageManager.sendInfo
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable

class EndFlow : Storeable() {
    override fun shouldStore() = true

    override val verboseInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.end_flow",
            currentLevelName
        )

    override val anonymizedInfo: MutableComponent
        get() = verboseInfo

    override fun store(
        session: LevelStorageSource.LevelStorageAccess,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        StatisticManager.infoMessage.apply {
            infoToast()

            val directory = Component.translatable("worldtools.capture.to_directory")
            val clickToOpen = translateHighlight(
                "worldtools.capture.click_to_open",
                currentLevelName
            ).copy().withStyle {
                it.withClickEvent(
                    ClickEvent.OpenFile(session.getLevelPath(LevelResource.ROOT).toFile())
                )
            }

            copy().append(directory).append(clickToOpen).sendInfo()
        }
    }
}