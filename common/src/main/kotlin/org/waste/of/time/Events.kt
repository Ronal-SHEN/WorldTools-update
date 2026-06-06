package org.waste.of.time

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
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
import java.awt.Color

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
        if (CAPTURE_KEY.consumeClick() && mc.world != null && mc.screen == null) {
            CaptureManager.toggleCapture()
        }

        if (CONFIG_KEY.consumeClick() && mc.world != null && mc.screen == null) {
            mc.setScreen(ManagerScreen)
        }

        if (!capturing) return
        updateCapture()
    }

    fun onClientJoin() {
        HotCache.clear()
        StorageFlow.lastStored = null
        StatisticManager.updateScreenAndTick()
        if (config.general.autoDownload) CaptureManager.start()
    }

    fun onClientDisconnect() {
        if (!capturing) return
        CaptureManager.stop()
    }

    fun onInteractBlock(world: Level, hitResult: BlockHitResult) {
        if (!capturing) return
        val blockEntity = world.getBlockEntity(hitResult.blockPosition)
        HotCache.lastInteractedBlockEntity = blockEntity
        HotCache.lastInteractedEntity = null
    }

    fun onInteractEntity(entity: Entity) {
        if (!capturing) return
        HotCache.lastInteractedEntity = entity
        HotCache.lastInteractedBlockEntity = null
    }

    fun onDebugRenderStart(
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource.Immediate,
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double
    ) {
        if (!capturing || !config.runTick.renderNotYetCachedContainers) return

        val vertexConsumer = vertexConsumers.getBuffer(RenderType.lines()) ?: return

        HotCache.unscannedBlockEntities
            .forEach { render(it.pos.vec, cameraX, cameraY, cameraZ, matrices, vertexConsumer, Color(config.runTick.unscannedContainerColor)) }

        HotCache.loadedBlockEntities
            .forEach { render(it.value.pos.vec, cameraX, cameraY, cameraZ, matrices, vertexConsumer, Color(config.runTick.fromCacheLoadedContainerColor)) }

        HotCache.unscannedEntities
            .forEach { render(it.entity.pos.add(-.5, .0, -.5), cameraX, cameraY, cameraZ, matrices, vertexConsumer, Color(config.runTick.unscannedEntityColor)) }
    }

    private val BlockPos.vec get() = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

    private fun render(
        vec: Vec3,
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double,
        matrices: PoseStack,
        vertexConsumer: VertexConsumer,
        color: Color
    ) {
        val x1 = (vec.x - cameraX).toFloat()
        val y1 = (vec.y - cameraY).toFloat()
        val z1 = (vec.z - cameraZ).toFloat()
        val x2 = x1 + 1
        val z2 = z1 + 1
        val r = color.red / 255.0f
        val g = color.green / 255.0f
        val b = color.blue / 255.0f
        val a = 1.0f
        val positionMat = matrices.last().positionMatrix
        val normMat = matrices.last()
        vertexConsumer.addVertex(positionMat, x1, y1, z1).color(r, g, b, a).setNormal(normMat, 1.0f, 0.0f, 0.0f)
        vertexConsumer.addVertex(positionMat, x2, y1, z1).color(r, g, b, a).setNormal(normMat, 1.0f, 0.0f, 0.0f)
        vertexConsumer.addVertex(positionMat, x1, y1, z1).color(r, g, b, a).setNormal(normMat, 0.0f, 0.0f, 1.0f)
        vertexConsumer.addVertex(positionMat, x1, y1, z2).color(r, g, b, a).setNormal(normMat, 0.0f, 0.0f, 1.0f)
        vertexConsumer.addVertex(positionMat, x1, y1, z2).color(r, g, b, a).setNormal(normMat, 1.0f, 0.0f, 0.0f)
        vertexConsumer.addVertex(positionMat, x2, y1, z2).color(r, g, b, a).setNormal(normMat, 1.0f, 0.0f, 0.0f)
        vertexConsumer.addVertex(positionMat, x2, y1, z2).color(r, g, b, a).setNormal(normMat, 0.0f, 0.0f, -1.0f)
        vertexConsumer.addVertex(positionMat, x2, y1, z1).color(r, g, b, a).setNormal(normMat, 0.0f, 0.0f, -1.0f)
    }

    fun onGameMenuScreenInitWidgets(adder: GridLayout.Adder) {
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

        adder.add(widget, 2)
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
                if (entity.pos.manhattanDistance2d(player.pos) < 32) { // todo: configurable distance, this should be small enough to be safe for most cases
                    val cacheable = EntityCacheable(entity)
                    HotCache.entities[entity.chunkPosition]?.remove(cacheable)
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
