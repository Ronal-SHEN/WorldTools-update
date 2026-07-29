package org.waste.of.time.gui
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.layouts.*

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.AutoConfigClient
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.waste.of.time.WorldTools.MAX_LEVEL_NAME_LENGTH
import org.waste.of.time.config.WorldToolsConfig
import org.waste.of.time.manager.CaptureManager
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.CaptureManager.levelName

object ManagerScreen : Screen(Component.translatable("worldtools.gui.manager.title")) {
    private lateinit var worldNameTextEntryWidget: EditBox
    private lateinit var titleWidget: StringWidget
    private lateinit var downloadButton: Button
    private lateinit var configButton: Button
    private lateinit var cancelButton: Button
    private const val BUTTON_WIDTH = 90

    override fun init() {
        setupTitle()
        setupEntryGrid()
        setupBottomGrid()
    }

    override fun tick() {
        if (CaptureManager.capturing) {
            downloadButton.message = Component.translatable("worldtools.gui.manager.button.stop_download")
            worldNameTextEntryWidget.setHint(Component.literal(currentLevelName))
            worldNameTextEntryWidget.setEditable(false)
        } else {
            downloadButton.message = Component.translatable("worldtools.gui.manager.button.start_download")
            worldNameTextEntryWidget.setEditable(true)
        }
        super.tick()
    }

    private fun setupTitle() {
        titleWidget = StringWidget(Component.translatable("worldtools.gui.manager.title"), font)
        FrameLayout.alignInRectangle(titleWidget, 0, 0, width, height, 0.5f, 0.01f)
        addRenderableWidget(titleWidget)
    }

    private fun setupEntryGrid() {
        val entryGridWidget = createGridWidget()
        val adder = entryGridWidget.createRowHelper(3)

        worldNameTextEntryWidget = EnterTextField(
            font, 0, 0, 250, 20, Component.literal(levelName), minecraft
        ).apply {
            setHint(Component.translatable("worldtools.gui.manager.world_name_placeholder", levelName))
            setMaxLength(MAX_LEVEL_NAME_LENGTH)
        }
        downloadButton = createButton("worldtools.gui.manager.button.start_download") {
            if (CaptureManager.capturing) {
                minecraft?.gui?.setScreen(null)
                CaptureManager.stop()
            } else {
                minecraft?.gui?.setScreen(null)
                CaptureManager.start(worldNameTextEntryWidget.value)
            }
        }

        adder.addChild(worldNameTextEntryWidget, 2)
        adder.addChild(downloadButton, 1)

        entryGridWidget.arrangeElements()
        FrameLayout.alignInRectangle(entryGridWidget, 0, titleWidget.y, width, height, 0.5f, 0.05f)
        entryGridWidget.visitWidgets(this::addRenderableWidget)
    }

    private fun setupBottomGrid() {
        val bottomGridWidget = createGridWidget()
        val bottomAdder = bottomGridWidget.createRowHelper(2)
        configButton = createButton("worldtools.gui.manager.button.config") {
            minecraft?.setScreenAndShow(AutoConfigClient.getConfigScreen(WorldToolsConfig::class.java, this).get())
        }
        cancelButton = createButton("worldtools.gui.manager.button.cancel") {
            minecraft?.gui?.setScreen(null)
        }

        bottomAdder.addChild(configButton, 1)
        bottomAdder.addChild(cancelButton, 1)

        bottomGridWidget.arrangeElements()
        FrameLayout.alignInRectangle(bottomGridWidget, 0, 0, width, height, 0.5f, .95f)
        bottomGridWidget.visitWidgets(this::addRenderableWidget)
    }

    private fun createGridWidget() = GridLayout().apply {
        defaultCellSetting().padding(4, 4, 4, 4)
    }

    private fun createButton(textKey: String, onClick: (Button) -> Unit) =
        Button.Builder(Component.translatable(textKey), onClick).width(BUTTON_WIDTH).build()
}
