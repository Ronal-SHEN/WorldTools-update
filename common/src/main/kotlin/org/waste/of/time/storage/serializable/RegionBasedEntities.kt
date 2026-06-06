package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.LevelStorageSource
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.manager.StatisticManager.joinWithAnd
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.RegionBased
import org.waste.of.time.storage.cache.EntityCacheable

class RegionBasedEntities(
    chunkPos: ChunkPos,
    val entities: Set<EntityCacheable>, // can be empty, signifies we should clear any previously saved entities
    world: Level
) : RegionBased(chunkPos, world, "entities") {
    override fun shouldStore() = config.general.capture.entities

    override val verboseInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.entities",
            stackEntities(),
            chunkPos,
            dimension
        )

    override val anonymizedInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.entities.anonymized",
            stackEntities(),
            dimension
        )

    override fun compound() = CompoundTag().apply {
        put("Entities", ListTag().apply {
            entities.forEach { entity ->
                add(entity.compound())
            }
        })

        putInt("DataVersion", SharedConstants.getCurrentVersion().saveVersion.id)
        put("Position", IntArrayTag(intArrayOf(chunkPos.x, chunkPos.z)))
        if (config.debug.logSavedEntities) {
            entities.forEach { entity -> LOG.info("Entity saved: $entity (Chunk: $chunkPos)") }
        }
    }

    override fun writeToStorage(
        session: LevelStorageSource.LevelStorageAccess,
        storage: CustomRegionBasedStorage,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        if (entities.isEmpty()) {
            // remove any previously stored entities in this chunk
            if (config.debug.logSavedEntities) {
                LOG.info("Removing any previously saved entities from chunk: {}", chunkPos)
            }
            storage.write(chunkPos, null)
            return
        }
        super.writeToStorage(session, storage, cachedStorages)
    }

    override fun incrementStats() {
        // todo: entities count becomes completely wrong when entities are removed or are loaded twice during the capture
        //  i.e. the player moves away, unloads them, and then comes back to load them again
        StatisticManager.entities += entities.size
        StatisticManager.dimensions.add(dimension)
    }

    private fun stackEntities() = entities.groupBy {
        it.entity.name
    }.map {
        val count = if (it.value.size > 1) {
            " (${it.value.size})"
        } else ""
        it.key.copy().append(count)
    }.joinWithAnd()
}
