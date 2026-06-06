package org.waste.of.time.manager

import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc

object MessageManager {
    private const val ERROR_COLOR = 0xff3333

    val brand: Component = Component.empty()
        .append(
            Component.literal("W").withStyle {
                it.withColor(TextColor.fromRgb(config.render.accentColor))
            }
        ).append(
            Component.literal("orld")
        ).append(
            Component.literal("T").withStyle {
                it.withColor(TextColor.fromRgb(config.render.accentColor))
            }
        ).append(
            Component.literal("ools")
        )
    private val converted by lazy {
        Component.literal("[").append(brand).append(Component.of("] "))
    }
    private val fullBrand: MutableComponent
        get() = converted.copy()

    fun String.info() =
        Component.of(this).sendInfo()

    fun sendInfo(translateKey: String, vararg args: Any) = translateHighlight(translateKey, *args).sendInfo()

    fun sendError(translateKey: String, vararg args: Any) = Component.translatable(translateKey, *args).sendError()

    fun Component.infoToast() {
        SystemToast.create(
            mc,
            SystemToast.Type.WORLD_BACKUP,
            brand,
            this
        ).addToast()
    }

    private fun Component.errorToast() {
        SystemToast.create(
            mc,
            SystemToast.Type.WORLD_ACCESS_FAILURE,
            brand,
            this
        ).addToast()
    }

    fun Component.sendInfo() =
        fullBrand.append(this).addMessage()

    private fun Component.sendError() {
        LOG.error(string)
        val errorText = copy().withStyle {
            it.withColor(ERROR_COLOR)
        }

        fullBrand.append(errorText).addMessage()
        errorText.errorToast()
    }

    private fun Component.addMessage() {
        if (!config.advanced.showChatMessages) return

        mc.execute {
            mc.inGameHud.chatHud.addMessage(this)
        }
    }

    private fun Toast.addToast() {
        if (!config.advanced.showToasts) return

        mc.execute {
            mc.toastManager.add(this)
        }
    }

    fun translateHighlight(key: String, vararg args: Any): MutableComponent =
        args.map { element ->
            val secondaryColor = TextColor.fromRgb(config.render.accentColor)
            if (element is Component) {
                if (element.style.color != null) {
                    element
                } else {
                    element.copy().withStyle { style ->
                        style.setColor(secondaryColor)
                    }
                }
            } else {
                Component.literal(element.toString()).withStyle { style ->
                    style.setColor(secondaryColor)
                }
            }
        }.toTypedArray().let {
            Component.translatable(key, *it)
        }
}
