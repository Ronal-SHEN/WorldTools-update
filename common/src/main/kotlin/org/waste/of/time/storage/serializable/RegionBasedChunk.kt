package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.material.Fluid
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongArrayTag
import net.minecraft.nbt.NbtOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.MutableComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.levelgen.BelowZeroRetrogen
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.storage.SerializableChunkData
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.ticks.SavedTick
import net.minecraft.world.level.levelgen.blending.BlendingData
import net.minecraft.world.level.storage.LevelStorageSource
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.TIMESTAMP_KEY
import org.waste.of.time.WorldTools.config
import org.waste.of.time.extension.IPalettedContainerExtension
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.Cacheable
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.RegionBased
import org.waste.of.time.storage.cache.HotCache

open class RegionBasedChunk(
    val chunk: LevelChunk,
) : RegionBased(chunk.pos, chunk.level, "region"), Cacheable {
    // storing a reference to the block entities in the chunk to prevent them from being unloaded
    val cachedBlockEntities = mutableMapOf<BlockPos, BlockEntity>()

    init {
        cachedBlockEntities.putAll(chunk.blockEntities)

        cachedBlockEntities.values.associateWith { fresh ->
            HotCache.scannedBlockEntities[fresh.blockPos]
        }.forEach { (fresh, cached) ->
            if (cached == null) return@forEach
            cachedBlockEntities[fresh.blockPos] = cached
        }
    }

    override fun shouldStore() = config.general.capture.chunks

    override val verboseInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.chunks",
            chunkPos,
            dimension
        )

    override val anonymizedInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.chunks.anonymized",
            dimension
        )

    private val stateIdContainer = PalettedContainer.codecRW(
        Block.BLOCK_STATE_REGISTRY,
        BlockState.CODEC,
        PalettedContainer.Strategy.SECTION_STATES,
        Blocks.AIR.defaultBlockState()
    )

    override fun cache() {
        HotCache.chunks[chunkPos] = this
        HotCache.savedChunks.add(chunkPos.toLong())
    }

    override fun flush() {
        HotCache.chunks.remove(chunkPos)
    }

    override fun incrementStats() {
        StatisticManager.chunks++
        StatisticManager.dimensions.add(dimension)
    }

    override fun writeToStorage(
        session: LevelStorageSource.LevelStorageAccess,
        storage: CustomRegionBasedStorage,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        // avoiding `emit` here due to flow order issues when capture is stopped
        // i.e., if EndFlow is emitted before this,
        // these are not written because they're behind it in the flow
        HotCache.getEntitySerializableForChunk(chunkPos, world)
            ?.store(session, cachedStorages)
            ?: run {
                // remove any previously stored entities in this chunk in case there are no entities to store
                RegionBasedEntities(chunkPos, emptySet(), world).store(session, cachedStorages)
        }
        if (chunk.isEmpty) return
        super.writeToStorage(session, storage, cachedStorages)
    }

    /**
     * See [net.minecraft.world.ChunkSerializer.serialize]
     */
    override fun compound() = CompoundTag().apply {
        if (config.world.metadata.captureTimestamp) {
            putLong(TIMESTAMP_KEY, System.currentTimeMillis())
        }

        putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version())
        putInt(SerializableChunkData.X_POS_TAG, chunk.pos.x)
        putInt("yPos", chunk.minSectionY)
        putInt(SerializableChunkData.Z_POS_TAG, chunk.pos.z)
        putLong("LastUpdate", chunk.level.gameTime)
        putLong("InhabitedTime", chunk.inhabitedTime)
        putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(chunk.persistedStatus).toString())

        genBackwardsCompat(chunk)

        if (!chunk.upgradeData.isEmpty) {
            put("UpgradeData", chunk.upgradeData.write())
        }

        put(SerializableChunkData.SECTIONS_TAG, generateSections(chunk))

        if (chunk.isLightCorrect) {
            putBoolean(SerializableChunkData.IS_LIGHT_ON_TAG, true)
        }

        put("block_entities", ListTag().apply {
            upsertBlockEntities()
        })

        getTickSchedulers(chunk)
        genPostProcessing(chunk)

        // skip structures
        if (config.debug.logSavedChunks)
            LOG.info("Chunk saved: $chunkPos ($dimension)")
    }

    private fun ListTag.upsertBlockEntities() {
        cachedBlockEntities.entries.map { (_, blockEntity) ->
            blockEntity.saveWithFullMetadata(world.registryAccess()).apply {
                putBoolean("keepPacked", false)
            }
        }.apply {
            addAll(this)
        }
    }

    private fun generateSections(chunk: LevelChunk) = ListTag().apply {
        val biomeRegistry = chunk.level.registryAccess().lookup(Registries.BIOME).orElse(null) ?: return@apply
        val defaultValue = biomeRegistry.get(Biomes.PLAINS).orElse(null) ?: return@apply
        val biomeCodec = PalettedContainer.codecRO(
            biomeRegistry.asHolderIdMap(),
            biomeRegistry.holderByNameCodec(),
            PalettedContainer.Strategy.SECTION_BIOMES,
            defaultValue
        )
        val lightingProvider = chunk.level.lightEngine

        (lightingProvider.minLightSection until lightingProvider.maxLightSection).forEach { y ->
            val sectionCoord = chunk.getSectionIndexFromSectionY(y)
            val inSection = sectionCoord in (0 until chunk.sections.size)
            val blockLightSection =
                lightingProvider.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.pos, y))
            val skyLightSection =
                lightingProvider.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.pos, y))

            if (!inSection && blockLightSection == null && skyLightSection == null) return@forEach

            add(CompoundTag().apply {
                if (inSection) {
                    val chunkSection = chunk.sections[sectionCoord]
                    /**
                     * Mods like Bobby may also try serializing chunk data concurrently on separate threads
                     * PalettedContainer contains a lock that is acquired during read/write operations
                     *
                     * Force disabling checking the lock's status here as it should be safe to
                     * read here, no write operations should happen after the chunk is unloaded
                     */
                    (chunkSection.states as IPalettedContainerExtension).setWTIgnoreLock(true)
                    (chunkSection.biomes as IPalettedContainerExtension).setWTIgnoreLock(true)
                    put(
                        "block_states",
                        stateIdContainer.encodeStart(NbtOps.INSTANCE, chunkSection.states).getOrThrow()
                    )
                    put(
                        "biomes",
                        biomeCodec.encodeStart(NbtOps.INSTANCE, chunkSection.biomes).getOrThrow()
                    )
                    (chunkSection.states as IPalettedContainerExtension).setWTIgnoreLock(false)
                    (chunkSection.biomes as IPalettedContainerExtension).setWTIgnoreLock(false)
                }
                if (blockLightSection != null && !blockLightSection.isEmpty) {
                    putByteArray(SerializableChunkData.BLOCK_LIGHT_TAG, blockLightSection.data)
                }
                if (skyLightSection != null && !skyLightSection.isEmpty) {
                    putByteArray(SerializableChunkData.SKY_LIGHT_TAG, skyLightSection.data)
                }
                if (isEmpty) return@forEach
                putByte("Y", y.toByte())
            })
        }
    }

    private fun CompoundTag.genBackwardsCompat(chunk: LevelChunk) {
        chunk.blendingData?.let { bleedingData ->
            BlendingData.Packed.CODEC.encodeStart(NbtOps.INSTANCE, bleedingData.pack()).resultOrPartial {
                LOG.error(it)
            }.ifPresent {
                put("blending_data", it)
            }
        }

        chunk.belowZeroRetrogen?.let { belowZeroRetrogen ->
            BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen).resultOrPartial {
                LOG.error(it)
            }.ifPresent {
                put("below_zero_retrogen", it)
            }
        }
    }

    private fun CompoundTag.getTickSchedulers(chunk: LevelChunk) {
        val time = chunk.level.gameTime
        val packed = chunk.getTicksForSerialization(time)
        put(
            "block_ticks",
            SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec()).listOf()
                .encodeStart(NbtOps.INSTANCE, packed.blocks()).getOrThrow()
        )
        put(
            "fluid_ticks",
            SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec()).listOf()
                .encodeStart(NbtOps.INSTANCE, packed.fluids()).getOrThrow()
        )
    }

    private fun CompoundTag.genPostProcessing(chunk: LevelChunk) {
        put("PostProcessing", SerializableChunkData.packOffsets(chunk.postProcessing))

        put(SerializableChunkData.HEIGHTMAPS_TAG, CompoundTag().apply {
            chunk.heightmaps.filter {
                chunk.persistedStatus.heightmapsAfter().contains(it.key)
            }.forEach { (key, value) ->
                put(key.serializationKey, LongArrayTag(value.rawData))
            }
        })
    }
}
