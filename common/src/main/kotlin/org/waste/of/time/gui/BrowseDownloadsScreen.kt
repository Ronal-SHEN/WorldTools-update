package org.waste.of.time.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.network.chat.Component

// todo: we need a local database of downloads like original wdl mod
object BrowseDownloadsScreen : Screen(Component.translatable("worldtools.gui.browser.title")) {
    override fun init() {

    }

    object DownloadListWidget : ObjectSelectionList<WorldDownloadEntry>(
        Minecraft.getInstance(),
        width,
        height,
        20,
        height - 30
    ) {


    }

    class WorldDownloadEntry : ObjectSelectionList.Entry<WorldDownloadEntry>() {
        override fun renderContent(
            context: GuiGraphics,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            TODO("Not yet implemented")
        }

        override fun getNarration(): Component {
            TODO("Not yet implemented")
        }

    }
}
