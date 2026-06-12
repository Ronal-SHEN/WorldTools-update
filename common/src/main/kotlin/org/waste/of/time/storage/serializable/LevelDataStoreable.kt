package org.waste.of.time.storage.serializable
import net.minecraft.nbt.*

import net.minecraft.SharedConstants
import net.minecraft.network.chat.MutableComponent
import net.minecraft.Util
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.waste.of.time.Utils.toByte
import org.waste.of.time.WorldTools.DAT_EXTENSION
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.config.WorldToolsConfig.World.WorldGenerator.GeneratorType
import org.waste.of.time.manager.CaptureManager
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable
import java.io.File
import java.io.IOException

class LevelDataStoreable : Storeable() {
    override fun shouldStore() = config.general.capture.levelData

    override val verboseInfo: MutableComponent
        get() = translateHighlight(
            "worldtools.capture.saved.levelData",
            currentLevelName,
            "level${DAT_EXTENSION}"
        )

    override val anonymizedInfo: MutableComponent
        get() = verboseInfo

    /**
     * See [net.minecraft.world.level.storage.LevelStorage.Session.backupLevelDataFile]
     */
    override fun store(
        session: LevelStorageAccess,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        val resultingFile = session.getLevelPath(LevelResource.ROOT).toFile()
        val dataNbt = serializeLevelData()
        // if we save an empty level.dat, clients will crash when opening the SP worlds screen
        if (dataNbt.isEmpty) throw RuntimeException("Failed to serialize level data")
        val levelNbt = CompoundTag().apply {
            put("Data", dataNbt)
        }

        try {
            val newFile = File.createTempFile("level", DAT_EXTENSION, resultingFile).toPath()
            NbtIo.writeCompressed(levelNbt, newFile)
            val backup = session.getLevelPath(LevelResource.OLD_LEVEL_DATA_FILE)
            val current = session.getLevelPath(LevelResource.LEVEL_DATA_FILE)
            Util.safeReplaceFile(current, newFile, backup)
            LOG.info("Saved level data.")
        } catch (exception: IOException) {
            MessageManager.sendError(
                "worldtools.log.error.failed_to_save_level",
                resultingFile.path,
                exception.localizedMessage
            )
        }
    }

    /**
     * See [net.minecraft.world.level.LevelProperties.updateProperties]
     */
    private fun serializeLevelData() = CompoundTag().apply {
        val player = CaptureManager.lastPlayer ?: mc.player ?: return@apply

        mc.connection?.serverBrand()?.let {
            put("ServerBrands", ListTag().apply {
                add(StringTag.valueOf(it))
            })
        }

        putBoolean("WasModded", false)

        // skip removed features

        put("Version", CompoundTag().apply {
            putString("Name", SharedConstants.getCurrentVersion().name)
            putInt("Id", SharedConstants.getCurrentVersion().dataVersion.version)
            putBoolean("Snapshot", !SharedConstants.getCurrentVersion().isStable)
            putString("Series", SharedConstants.getCurrentVersion().dataVersion.series)
        })

        NbtUtils.addCurrentDataVersion(this)

        put("WorldGenSettings", generatorMockNbt())
        mc.connection?.listedOnlinePlayers?.find {
            it.profile.id == player.uuid
        }?.let {
            putInt("GameType", it.gameMode.id)
        } ?: putInt("GameType", player.server?.defaultGameType?.id ?: 0)

        putInt("SpawnX", player.level().levelData.spawnPos.x)
        putInt("SpawnY", player.level().levelData.spawnPos.y)
        putInt("SpawnZ", player.level().levelData.spawnPos.z)
        putFloat("SpawnAngle", player.level().levelData.spawnAngle)
        putLong("Time", player.level().gameTime)
        putLong("DayTime", player.level().dayTime)
        putLong("LastPlayed", System.currentTimeMillis())
        putString("LevelName", currentLevelName)
        putInt("version", 19133)
        putInt("clearWeatherTime", 0) // not sure
        putInt("rainTime", 0) // not sure
        putBoolean("raining", player.level().isRaining)
        putBoolean("thundering", player.level().isThundering)
        putBoolean("hardcore", player.server?.isHardcore ?: false)
        putInt("thunderTime", 0) // not sure
        putBoolean("allowCommands", true) // not sure
        putBoolean("initialized", true) // not sure

        player.level().worldBorder.let { border ->
            putDouble("BorderCenterX", border.centerX)
            putDouble("BorderCenterZ", border.centerZ)
            putDouble("BorderSize", border.size)
            putLong("BorderSizeLerpTime", border.lerpRemainingTime)
            putDouble("BorderSizeLerpTarget", border.lerpTarget)
            putDouble("BorderSafeZone", border.damageSafeZone)
            putDouble("BorderDamagePerBlock", border.damagePerBlock)
            putDouble("BorderWarningBlocks", border.warningBlocks.toDouble())
            putDouble("BorderWarningTime", border.warningTime.toDouble())
        }

        putByte("Difficulty", player.level().levelData.difficulty.id.toByte())
        putBoolean("DifficultyLocked", false) // not sure

        // ToDo: Seems that the client side game rules were removed. Now only works for single player :/
        val rules = player.level()?.server?.gameRules?.genGameRules() ?: CompoundTag()
        put("GameRules", rules)
        put("Player", CompoundTag().apply {
            player.saveWithoutId(this)
            remove("LastDeathLocation") // can contain sensitive information
            putString("Dimension", "minecraft:${player.level().dimension().location().path}")
        })

        put("DragonFight", CompoundTag()) // not sure
        put("CustomBossEvents", CompoundTag()) // not sure
        put("ScheduledEvents", ListTag()) // not sure
        putInt("WanderingTraderSpawnDelay", 0) // not sure
        putInt("WanderingTraderSpawnChance", 0) // not sure

        // skip wandering trader id
    }

    private fun GameRules.genGameRules() = createTag().apply {
        val setting = config.world.gameRules
        if (!setting.modifyGameRules) return@apply

        putString(GameRules.RULE_DO_WARDEN_SPAWNING.id, setting.doWardenSpawning.toString())
        putString(GameRules.RULE_DOFIRETICK.id, setting.doFireTick.toString())
        putString(GameRules.RULE_DO_VINES_SPREAD.id, setting.doVinesSpread.toString())
        putString(GameRules.RULE_DOMOBSPAWNING.id, setting.doMobSpawning.toString())
        putString(GameRules.RULE_DAYLIGHT.id, setting.doDaylightCycle.toString())
        putString(GameRules.RULE_KEEPINVENTORY.id, setting.keepInventory.toString())
        putString(GameRules.RULE_MOBGRIEFING.id, setting.doMobGriefing.toString())
        putString(GameRules.RULE_DO_TRADER_SPAWNING.id, setting.doTraderSpawning.toString())
        putString(GameRules.RULE_DO_PATROL_SPAWNING.id, setting.doPatrolSpawning.toString())
        putString(GameRules.RULE_WEATHER_CYCLE.id, setting.doWeatherCycle.toString())
    }

    private fun generatorMockNbt() = CompoundTag().apply {
        putByte("bonus_chest", config.world.worldGenerator.bonusChest.toByte())
        putLong("seed", config.world.worldGenerator.seed)
        putByte("generate_features", config.world.worldGenerator.generateFeatures.toByte())

        put("dimensions", CompoundTag().apply {
            // Vanilla's WorldDimensions codec requires a minecraft:overworld entry,
            // otherwise loading the world crashes with "Overworld settings missing".
            // Servers that only expose custom dimensions (e.g. play.hollowcube.net)
            // don't have an overworld, so relabel the first captured dimension as
            // the overworld in that case.
            val keys = CaptureManager.lastWorldKeys
            val hasOverworld = keys.any { it.location().path == "overworld" }

            keys.forEachIndexed { index, key ->
                val path = if (!hasOverworld && index == 0) "overworld" else key.location().path

                put("minecraft:$path", CompoundTag().apply {
                    put("generator", generateGenerator(path))

                    when (path) {
                        "the_nether" -> {
                            putString("type", "minecraft:the_nether")
                        }
                        "the_end" -> {
                            putString("type", "minecraft:the_end")
                        }
                        else -> {
                            putString("type", "minecraft:overworld")
                        }
                    }
                })
            }
        })
    }

    private fun generateGenerator(path: String) = CompoundTag().apply {
        when (config.world.worldGenerator.type) {
            GeneratorType.VOID -> voidGenerator()
            GeneratorType.DEFAULT -> defaultGenerator(path)
            GeneratorType.FLAT -> flatGenerator()
        }
    }

    private fun CompoundTag.voidGenerator() {
        put("settings", CompoundTag().apply {
            putByte("features", 1)
            putString("biome", "minecraft:the_void")
            put("layers", ListTag().apply {
                add(CompoundTag().apply {
                    putString("block", "minecraft:air")
                    putInt("height", 1)
                })
            })
            put("structure_overrides", ListTag())
            putByte("lakes", 0)
        })
        putString("type", "minecraft:flat")
    }

    private fun CompoundTag.defaultGenerator(path: String) {
        when (path) {
            "the_nether" -> {
                put("biome_source", CompoundTag().apply {
                    putString("preset", "minecraft:nether")
                    putString("type", "minecraft:multi_noise")
                })
                putString("settings", "minecraft:nether")
                putString("type", "minecraft:noise")
            }
            "the_end" -> {
                put("biome_source", CompoundTag().apply {
                    putString("type", "minecraft:the_end")
                })
                putString("settings", "minecraft:end")
                putString("type", "minecraft:noise")
            }
            else -> {
                put("biome_source", CompoundTag().apply {
                    putString("preset", "minecraft:overworld")
                    putString("type", "minecraft:multi_noise")
                })
                putString("settings", "minecraft:overworld")
                putString("type", "minecraft:noise")
            }
        }
    }

    private fun CompoundTag.flatGenerator() {
        put("settings", CompoundTag().apply {
            putString("biome", "minecraft:plains")
            putByte("features", 0)
            putByte("lakes", 0)
            put("layers", ListTag().apply {
                add(CompoundTag().apply {
                    putString("block", "minecraft:bedrock")
                    putInt("height", 1)
                })
                add(CompoundTag().apply {
                    putString("block", "minecraft:dirt")
                    putInt("height", 2)
                })
                add(CompoundTag().apply {
                    putString("block", "minecraft:grass_block")
                    putInt("height", 1)
                })
            })
            put("structure_overrides", ListTag().apply {
                add(StringTag.valueOf("minecraft:strongholds"))
                add(StringTag.valueOf("minecraft:villages"))
            })
        })
        putString("type", "minecraft:flat")
    }
}
