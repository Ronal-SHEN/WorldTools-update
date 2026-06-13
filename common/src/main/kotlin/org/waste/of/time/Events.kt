package org.waste.of.time

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.gizmos.Gizmos
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.world.phys.AABB
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import org.waste.of.time.Utils.manhattanDistance2d
import org.waste.of.time.WorldTools.CAPTURE_KEY
import org.waste.of.time.WorldTools.CONFIG_KEY
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.gui.ManagerScreen
import org.waste.of.time.manager.BarManager.updateCapture
import org.waste.of.time.manager.CaptureManager
import org.waste.of.time.manager.CaptureManager.capturing
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.StorageFlow
import org.waste.of.time.storage.cache.EntityCacheable
import org.waste.of.time.storage.cache.HotCache
import org.waste.of.time.storage.cache.DataInjectionHandler
import org.waste.of.time.storage.serializable.BlockEntityLoadable
import org.waste.of.time.storage.serializable.PlayerStoreable
import org.waste.of.time.storage.serializable.RegionBasedChunk

object Events {
    fun onChunkLoad(chunk: LevelChunk) {
        if (!capturing) return
        RegionBasedChunk(chunk).cache()
        BlockEntityLoadable(chunk).emit()
    }

    fun onChunkUnload(chunk: LevelChunk) {
        if (!capturing) return
        (HotCache.chunks[chunk.pos] ?: RegionBasedChunk(chunk)).apply {
            emit()
            flush()
        }
    }

    fun onEntityLoad(entity: Entity) {
        if (!capturing) return
        if (entity is Player) {
            PlayerStoreable(entity).cache()
        } else {
            EntityCacheable(entity).cache()
        }
    }

    fun onEntityUnload(entity: Entity) {
        if (!capturing) return
        if (entity !is Player) return
        PlayerStoreable(entity).apply {
            emit()
            flush()
        }
    }

    fun onClientTickStart() {
        if (CAPTURE_KEY.consumeClick() && mc.level != null && mc.screen == null) {
            CaptureManager.toggleCapture()
        }

        if (CONFIG_KEY.consumeClick() && mc.level != null && mc.screen == null) {
            mc.setScreen(ManagerScreen)
        }

        if (!capturing) return
        updateCapture()
    }

    fun onClientJoin() {
        HotCache.clear()
        StorageFlow.lastStored = null
        StatisticManager.reset()
        if (config.general.autoDownload) CaptureManager.start()
    }

    fun onClientDisconnect() {
        if (!capturing) return
        CaptureManager.stop()
    }

    fun onInteractBlock(world: Level, hitResult: BlockHitResult) {
        if (!capturing) return
        val blockEntity = world.getBlockEntity(hitResult.blockPos)
        HotCache.lastInteractedBlockEntity = blockEntity
        HotCache.lastInteractedEntity = null
    }

    fun onInteractEntity(entity: Entity) {
        if (!capturing) return
        HotCache.lastInteractedEntity = entity
        HotCache.lastInteractedBlockEntity = null
    }

    fun onDebugRenderStart() {
        if (!capturing || !config.render.renderNotYetCachedContainers) return

        HotCache.unscannedBlockEntities
            .forEach { renderBox(it.blockPos, config.render.unscannedContainerColor) }

        HotCache.loadedBlockEntities
            .forEach { renderBox(it.value.blockPos, config.render.fromCacheLoadedContainerColor) }

        HotCache.unscannedEntities
            .forEach { renderBox(it.entity.boundingBox, config.render.unscannedEntityColor) }
    }

    private fun renderBox(pos: BlockPos, color: Int) {
        Gizmos.cuboid(pos, GizmoStyle.stroke(color.opaque))
    }

    private fun renderBox(box: AABB, color: Int) {
        Gizmos.cuboid(box, GizmoStyle.stroke(color.opaque))
    }

    // config colors are stored as 0xRRGGBB; gizmo styles expect a fully opaque ARGB int
    private val Int.opaque get() = this or 0xFF000000.toInt()

    fun onGameMenuScreenInitWidgets(adder: GridLayout.RowHelper) {
        val widget = if (capturing) {
            val label = translateHighlight("worldtools.gui.escape.button.finish_download", currentLevelName)
            Button.builder(label) {
                CaptureManager.stop()
                mc.setScreen(null)
            }.width(204).build()
        } else {
            Button.builder(MessageManager.brand) {
                Minecraft.getInstance().setScreen(ManagerScreen)
            }.width(204).build()
        }

        adder.addChild(widget, 2)
    }

    fun onScreenRemoved(screen: Screen) {
        if (!capturing) return
        DataInjectionHandler.onScreenRemoved(screen)
        HotCache.lastInteractedBlockEntity = null
    }

    fun onEntityRemoved(entity: Entity, reason: Entity.RemovalReason) {
        if (!capturing) return
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) return

        if (entity is LivingEntity) {
            if (!entity.isDeadOrDying) return

            val cacheable = EntityCacheable(entity)
            HotCache.entities.entries.find { (_, entities) ->
                entities.contains(cacheable)
            }?.value?.remove(cacheable)
        } else {
            // todo: its actually a bit tricky to differentiate the entity being removed from our world or the server world
            //  need to find a reliable way to determine it
            //  if chunk is loaded, remove the entity? -> doesn't seem to work because server will remove entity before chunk is unloaded
            mc.player?.let { player ->
                if (entity.blockPosition().distManhattan(player.blockPosition()) < 32) { // todo: configurable distance, this should be small enough to be safe for most cases
                    val cacheable = EntityCacheable(entity)
                    HotCache.entities[entity.chunkPosition()]?.remove(cacheable)
                }
            }
        }
    }

    fun onMapStateGet(id: MapId) {
        if (!capturing) return
        // todo: looks like the server does not send a map update packet for container
        HotCache.mapIDs.add(id.id)
    }
}
