package org.waste.of.time.storage.serializable

import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.MutableComponent
import net.minecraft.FileUtil
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.waste.of.time.Utils
import org.waste.of.time.WorldTools.CREDIT_MESSAGE_MD
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.MOD_NAME
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.BarManager
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.CaptureManager.levelName
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.PathTreeNode
import org.waste.of.time.storage.StorageFlow
import org.waste.of.time.storage.Storeable
import java.net.InetSocketAddress
import java.nio.file.Path
import kotlin.io.path.writeBytes

class MetadataStoreable : Storeable() {
    override fun shouldStore() = config.general.capture.metadata

    override val verboseInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.metadata",
            currentLevelName
        )

    override val anonymizedInfo: MutableComponent
        get() = verboseInfo

    override fun store(session: LevelStorageAccess, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        session.writeIconFile()

        session.getLevelPath(LevelResource.ROOT).resolve(MOD_NAME).apply {
            FileUtil.createDirectoriesSafe(this)

            writePlayerEntryList()
            writeDimensionTree()
            writeMetadata()
        }
    }

    private fun Path.writeMetadata() {
        resolve("Capture Metadata.md")
            .toFile()
            .writeText(createMetadata())

        LOG.info("Saved capture metadata.")
    }

    private fun Path.writePlayerEntryList() {
        if (mc.isLocalServer) return

        mc.connection?.playerList?.let { playerList ->
            if (playerList.isEmpty()) return@let
            resolve("Player Entry List.csv").toFile()
                .writeText(createPlayerEntryList(playerList.toList()))
            LOG.info("Saved ${playerList.size} player entry list entries.")
        }
    }

    private fun Path.writeDimensionTree() {
        mc.connection?.worldKeys?.let { keys ->
            if (keys.isEmpty()) return@let
            resolve("Dimension Tree.txt").toFile()
                .writeText(PathTreeNode.buildTree(keys.map { it.value.path }))
            LOG.info("Saved ${keys.size} dimensions in tree.")
        }
    }

    private fun LevelStorageAccess.writeIconFile() {
        mc.connection?.serverInfo?.favicon?.let { favicon ->
            iconFile.ifPresent {
                it.writeBytes(favicon)
            }
        } ?: mc.server?.iconFile?.ifPresent { spIconPath ->
            iconFile.ifPresent {
                it.writeBytes(spIconPath.toFile().readBytes())
            }
        }
        LOG.info("Saved favicon.")
    }

    private fun createMetadata() = StringBuilder().apply {
        if (currentLevelName != levelName) {
            appendLine("# $currentLevelName ($levelName) World Save - Snapshot Details")
        } else {
            appendLine("# $currentLevelName World Save - Snapshot Details")
        }

        if (mc.isLocalServer) {
            appendLine("![World Icon](../icon.png)")
        } else {
            appendLine("![Server Icon](../icon.png)")
        }

        appendLine()
        appendLine("- **Time**: `${Utils.getTime()}` (Timestamp: `${System.currentTimeMillis()}`)")
        appendLine("- **Captured By**: `${mc.player?.name?.string}`")

        appendLine()

        mc.connection?.serverInfo?.let { info ->
            appendLine("## Server")
            if (info.name != "Minecraft Server") {
                appendLine("- **List Entry Name**: `${info.name}`")
            }
            appendLine("- **IP**: `${info.address}`")
            if (info.playerCountLabel.string.isNotBlank()) {
                appendLine("- **Capacity**: `${info.playerCountLabel.string}`")
            }
            mc.connection?.let {
                appendLine("- **Brand**: `${it.brand}`")
            }
            appendLine("- **MOTD**: `${info.label.string.split("\n").joinToString(" ")}`")
            appendLine("- **Version**: `${info.version.string}`")
            appendLine("- **Protocol Version**: `${info.protocolVersion}`")
            appendLine("- **Server Type**: `${info.serverType}`")

            info.players?.sample?.let l@ { sample ->
                if (sample.isEmpty()) return@l
                appendLine("- **Short Label**: `${sample.joinToString { it.name }}`")
            }
            info.playerListSummary?.let l@ {
                if (it.isEmpty()) return@l
                appendLine("- **Full Label**: `${it.joinToString(" ") { str -> str.string }}`")
            }

            appendLine()
            appendLine("## Connection")
            (mc.connection?.connection?.address as? InetSocketAddress)?.let {
                appendLine("- **Host Name**: `${it.address.canonicalHostName}`")
                appendLine("- **Port**: `${it.port}`")
            }
        } ?: run {
            appendLine("## Singleplayer Capture")
            appendLine("- **Source World Name**: `${mc.server?.name}`")
            appendLine("- **Version**: `${mc.server?.version}`")
        }

        mc.connection?.sessionId?.let { id ->
            appendLine("- **LevelStorageAccess ID**: `$id`")
        }

        appendLine()
        appendLine(CREDIT_MESSAGE_MD)
    }.toString()

    private fun createPlayerEntryList(listEntries: List<PlayerInfo>) = StringBuilder().apply {
        appendLine("Name, ID, Game Mode, Latency, Scoreboard Team, Model Type, LevelStorageAccess ID, Public Key")

        listEntries.forEachIndexed { i, entry ->
            StorageFlow.lastStoredTimestamp = System.currentTimeMillis()
            BarManager.progressBar.progress = i.toFloat() / listEntries.size
            serializePlayerListEntry(entry)
        }
    }.toString()

    private fun StringBuilder.serializePlayerListEntry(entry: PlayerInfo) {
        append("${entry.profile.name}, ")
        append("${entry.profile.id}, ")
        append("${entry.gameMode.name}, ")
        append("${entry.latency}, ")
        append("${entry.scoreboardTeam?.name}, ")
        appendLine(entry.skinTextures.model)
        entry.chatSession?.let {
            append("${it.sessionId}, ")
            append("${it.publicKeyData?.data}, ")
        }
    }
}
