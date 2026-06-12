package org.waste.of.time.storage.serializable

import net.minecraft.world.entity.player.Player
import org.waste.of.time.Utils.saveToCompound
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.Util
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.waste.of.time.Utils.asString
import org.waste.of.time.Utils.sanitizePlayerForSingleplayer
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.config
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.Cacheable
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable
import org.waste.of.time.storage.cache.HotCache
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

data class PlayerStoreable(
    val player: Player
) : Cacheable, Storeable() {
    override fun shouldStore() = config.general.capture.players

    override val verboseInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.player",
            player.name,
            "${player.blockX}, ${player.blockY}, ${player.blockZ}",
            player.level().dimension().identifier().path
        )

    override val anonymizedInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.player.anonymized",
            player.name,
            player.level().dimension().identifier().path
        )

    override fun cache() {
        HotCache.players.add(this)
    }

    override fun flush() {
        HotCache.players.remove(this)
    }

    override fun store(session: LevelStorageAccess, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        savePlayerData(player, session)
        StatisticManager.players++
        StatisticManager.dimensions.add(player.level().dimension().identifier().path)
    }

    private fun savePlayerData(player: Player, session: LevelStorageAccess) {
        try {
            val playerDataDir = session.getLevelPath(LevelResource.PLAYER_DATA_DIR).toFile()
            playerDataDir.mkdirs()

            val newPlayerFile = File.createTempFile(player.stringUUID + "-", ".dat", playerDataDir).toPath()
            NbtIo.writeCompressed(player.saveToCompound().apply {
                // The vanilla entity save omits the Dimension tag (it is normally written by
                // the server's player-save wrapper). In single-player the integrated server
                // loads the player from playerdata/<uuid>.dat in preference to level.dat's
                // Player tag, so without a Dimension here the player is dropped into the
                // default (often empty) overworld instead of their captured dimension.
                putString("Dimension", "minecraft:${player.level().dimension().identifier().path}")
                if (config.entity.censor.lastDeathLocation) {
                    remove("LastDeathLocation")
                }
                if (config.world.playerBehavior.modifyPlayerBehavior) {
                    sanitizePlayerForSingleplayer()
                }
            }, newPlayerFile)
            val currentFile = File(playerDataDir, player.stringUUID + ".dat").toPath()
            val backupFile = File(playerDataDir, player.stringUUID + ".dat_old").toPath()
            Util.safeReplaceFile(currentFile, newPlayerFile, backupFile)
        } catch (e: Exception) {
            WorldTools.LOG.warn("Failed to save player data for {}", player.name.string)
        }
    }
}
