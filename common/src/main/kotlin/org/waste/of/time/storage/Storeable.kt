package org.waste.of.time.storage

import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.WorldTools.config

abstract class Storeable {
    abstract fun store(
        session: LevelStorageAccess,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    )

    abstract fun shouldStore(): Boolean

    abstract val verboseInfo: MutableComponent

    abstract val anonymizedInfo: MutableComponent

    fun emit() = StorageFlow.emit(this)

    val formattedInfo: Component by lazy {
        if (config.advanced.anonymousMode) {
            anonymizedInfo
        } else {
            verboseInfo
        }.append(translateHighlight("worldtools.capture.took", StorageFlow.lastStoredTimeNeeded))
    }
}