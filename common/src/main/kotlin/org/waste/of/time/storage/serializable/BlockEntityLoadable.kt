package org.waste.of.time.storage.serializable

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.storage.LevelStorageSource
import org.waste.of.time.WorldTools.config
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.cache.HotCache
import org.waste.of.time.storage.cache.HotCache.isSupported
import org.waste.of.time.storage.cache.HotCache.markScanned

class BlockEntityLoadable(
    chunk: LevelChunk
) : RegionBasedChunk(chunk) {
    private var migrated = false
    override fun shouldStore() =
        config.general.reloadBlockEntities && chunk.blockEntities.isNotEmpty()

    override val verboseInfo = translateHighlight(
        "worldtools.capture.loaded.block_entities",
        chunk.pos,
        chunk.level.dimension().identifier().path
    )

    override val anonymizedInfo = translateHighlight(
        "worldtools.capture.loaded.block_entities.anonymized",
        chunk.level.dimension().identifier().path
    )

    fun load(
        session: LevelStorageSource.LevelStorageAccess,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ): Boolean {
        generateStorage(session, cachedStorages)
            .getBlockEntities(chunkPos)
            .filter { it.isSupported }
            .forEach { existing ->
                HotCache.chunks[chunkPos]
                    ?.cachedBlockEntities
                    ?.get(existing.blockPos)
                    ?.let { blockEntity ->
                        when (blockEntity) {
                            is BaseContainerBlockEntity -> blockEntity.migrateData(existing)
                            is LecternBlockEntity -> blockEntity.migrateData(existing)
                        }
                    }
            }
        return migrated
    }

    private fun BaseContainerBlockEntity.migrateData(existing: BlockEntity) {
        if (existing !is BaseContainerBlockEntity) return
        if (!isEmpty) return
        items = existing.items
        markScanned(true)
        migrated = true
    }

    private fun LecternBlockEntity.migrateData(existing: BlockEntity) {
        if (existing !is LecternBlockEntity) return
        if (!book.isEmpty) return
        book = existing.book
        markScanned(true)
        migrated = true
    }
}
