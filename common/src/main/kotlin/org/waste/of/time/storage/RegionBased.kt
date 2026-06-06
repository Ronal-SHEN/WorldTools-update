package org.waste.of.time.storage

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.LevelStorageSource
import org.waste.of.time.WorldTools.LOG

abstract class RegionBased(
    val chunkPos: ChunkPos,
    val world: Level,
    private val suffix: String
) : Storeable() {
    val dimension: String = world.dimension.value.path

    private val dimensionPath
        get() = when (dimension) {
            "overworld" -> ""
            "the_nether" -> "DIM-1/"
            "the_end" -> "DIM1/"
            else -> "dimensions/minecraft/$dimension/"
        }

    abstract fun compound(): CompoundTag

    abstract fun incrementStats()

    // can be overridden but super should be called after
    open fun writeToStorage(
        session: LevelStorageSource.LevelStorageAccess,
        storage: CustomRegionBasedStorage,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        try {
            storage.write(
                chunkPos,
                compound()
            )
            incrementStats()
        } catch (e: Exception) {
            LOG.error("Failed to store $this", e)
        }
    }

    override fun store(
        session: LevelStorageSource.LevelStorageAccess,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        if (!shouldStore()) return
        val storage = generateStorage(session, cachedStorages)
        writeToStorage(session, storage, cachedStorages)
    }

    fun generateStorage(
        session: LevelStorageSource.LevelStorageAccess,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ): CustomRegionBasedStorage {
        val path = session.getLevelPath(LevelResource.ROOT)
            .resolve(dimensionPath)
            .resolve(suffix)
        return cachedStorages.getOrPut(path.toString()) {
            CustomRegionBasedStorage(path, false)
        }
    }
}
