package org.waste.of.time.storage

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.FileUtil
import net.minecraft.util.ExceptionCollector
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.storage.RegionFile
import net.minecraft.world.level.chunk.storage.RegionStorageInfo
import org.waste.of.time.WorldTools.MCA_EXTENSION
import org.waste.of.time.WorldTools.MOD_NAME
import org.waste.of.time.WorldTools.mc
import java.io.DataOutput
import java.io.IOException
import java.nio.file.Path


open class CustomRegionBasedStorage internal constructor(
    private val directory: Path,
    private val dsync: Boolean
) : AutoCloseable {
    private val cachedRegionFiles: Long2ObjectLinkedOpenHashMap<RegionFile?> = Long2ObjectLinkedOpenHashMap()

    companion object {
        // Seems to only be used for MC's profiler
        // simpler to just use a default key instead of wiring this all in here
        val defaultStorageKey: RegionStorageInfo = RegionStorageInfo(MOD_NAME, Level.OVERWORLD, "chunk")
    }

    @Throws(IOException::class)
    fun getRegionFile(pos: ChunkPos): RegionFile {
        val longPos = ChunkPos.asLong(pos.regionX, pos.regionZ)
        cachedRegionFiles.getAndMoveToFirst(longPos)?.let { return it }

        if (cachedRegionFiles.size >= 256) {
            cachedRegionFiles.removeLast()?.close()
        }

        FileUtil.createDirectoriesSafe(directory)
        val path = directory.resolve("r." + pos.regionX + "." + pos.regionZ + MCA_EXTENSION)
        val regionFile = RegionFile(defaultStorageKey, path, directory, dsync)
        cachedRegionFiles.putAndMoveToFirst(longPos, regionFile)
        return regionFile
    }

    @Throws(IOException::class)
    fun write(pos: ChunkPos, nbt: CompoundTag?) {
        val regionFile = getRegionFile(pos)
        if (nbt == null) {
            regionFile.clear(pos)
        } else {
            regionFile.getChunkDataOutputStream(pos).use { dataOutputStream ->
                NbtIo.write(nbt, dataOutputStream as DataOutput)
            }
        }
    }

    private fun getNbtAt(chunkPos: ChunkPos) =
        getRegionFile(chunkPos).getChunkDataInputStream(chunkPos)?.use { dataInputStream ->
            NbtIo.read(dataInputStream)
        }

    fun getBlockEntities(chunkPos: ChunkPos): List<BlockEntity> =
        getNbtAt(chunkPos)
            ?.getListOrEmpty("block_entities")
            ?.filterIsInstance<CompoundTag>()
            ?.mapNotNull { compoundTag ->
                val blockPos = BlockPos(compoundTag.getIntOr("x", 0), compoundTag.getIntOr("y", 0), compoundTag.getIntOr("z", 0))
                val blockStateIdentifier = ResourceLocation.parse(compoundTag.getStringOr("id", ""))
                val world = mc.level ?: return@mapNotNull null

                runCatching {
                    val block = BuiltInRegistries.BLOCK.getValue(blockStateIdentifier)
                    BuiltInRegistries.BLOCK_ENTITY_TYPE
                        .getOptional(blockStateIdentifier)
                        .orElse(null)
                        ?.create(blockPos, block.defaultBlockState())?.apply {
                            loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), compoundTag))
                        }
                }.getOrNull()
            } ?: emptyList()

    @Throws(IOException::class)
    override fun close() {
        val throwableDeliverer = ExceptionCollector<IOException>()

        cachedRegionFiles.values.filterNotNull().forEach { regionFile ->
            try {
                regionFile.close()
            } catch (iOException: IOException) {
                throwableDeliverer.add(iOException)
            }
        }

        throwableDeliverer.throwIfPresent()
    }
}
