package org.waste.of.time.manager

import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.WorldTools.config
import org.waste.of.time.storage.cache.HotCache

object StatisticManager {
    var chunks = 0
    var entities = 0
    var players = 0
    private val containers get(): Int =
        HotCache.scannedBlockEntities.size + HotCache.loadedBlockEntities.size
    val dimensions = mutableSetOf<String>()

    fun reset() {
        chunks = 0
        entities = 0
        players = 0
        dimensions.clear()
    }

    val infoMessage: Component
        get() {
            val savedElements = mutableListOf<Component>().apply {
                if (chunks == 1) {
                    add(translateHighlight("worldtools.capture.chunk", chunks))
                }
                if (chunks > 1) {
                    add(translateHighlight("worldtools.capture.chunks", "%,d".format(chunks)))
                }

                if (entities == 1) {
                    add(translateHighlight("worldtools.capture.entity", entities))
                }
                if (entities > 1) {
                    add(translateHighlight("worldtools.capture.entities", "%,d".format(entities)))
                }

                if (players == 1) {
                    add(translateHighlight("worldtools.capture.player", players))
                }
                if (players > 1) {
                    add(translateHighlight("worldtools.capture.players", "%,d".format(players)))
                }

                if (containers == 1) {
                    add(translateHighlight("worldtools.capture.container", containers))
                }
                if (containers > 1) {
                    add(translateHighlight("worldtools.capture.containers", "%,d".format(containers)))
                }
            }

            return if (savedElements.isEmpty()) {
                translateHighlight("worldtools.capture.nothing_saved_yet", currentLevelName)
            } else {
                val dimensionsFormatted = dimensions.map {
                    Component.literal(it).withStyle { text ->
                        text.withColor(TextColor.fromRgb(config.render.accentColor))
                    }
                }.joinWithAnd()
                Component.translatable("worldtools.capture.saved").copy()
                    .append(savedElements.joinWithAnd())
                    .append(Component.translatable("worldtools.capture.in_dimension"))
                    .append(dimensionsFormatted)
            }
        }

    fun List<Component>.joinWithAnd(): Component {
        val and = Component.translatable("worldtools.capture.and")
        return when (size) {
            0 -> Component.literal("")
            1 -> this[0]
            2 -> this[0].copy().append(and).append(this[1])
            else -> dropLast(1).join().append(and).append(last())
        }
    }

    private fun List<Component>.join(): MutableComponent {
        val comma = Component.literal(", ")
        return foldIndexed(Component.literal("")) { index, acc, text ->
            if (index == 0) return@foldIndexed text.copy()
            acc.append(comma).append(text)
        }
    }
}