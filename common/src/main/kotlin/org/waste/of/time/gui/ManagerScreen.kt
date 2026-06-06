package org.waste.of.time.gui
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.layouts.*

import me.shedaniel.autoconfig.AutoConfig
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
            worldNameTextEntryWidget.setPlaceholder(Component.of(currentLevelName))
            worldNameTextEntryWidget.setEditable(false)
        } else {
            downloadButton.message = Component.translatable("worldtools.gui.manager.button.start_download")
            worldNameTextEntryWidget.setEditable(true)
        }
        super.tick()
    }

    private fun setupTitle() {
        titleWidget = StringWidget(Component.translatable("worldtools.gui.manager.title"), textRenderer)
        FrameLayout.setPos(titleWidget, 0, 0, width, height, 0.5f, 0.01f)
        addRenderableWidget(titleWidget)
    }

    private fun setupEntryGrid() {
        val entryGridWidget = createGridWidget()
        val adder = entryGridWidget.createAdder(3)

        worldNameTextEntryWidget = EnterTextField(
            textRenderer, 0, 0, 250, 20, Component.of(levelName), client
        ).apply {
            setPlaceholder(Component.translatable("worldtools.gui.manager.world_name_placeholder", levelName))
            setMaxLength(MAX_LEVEL_NAME_LENGTH)
        }
        downloadButton = createButton("worldtools.gui.manager.button.start_download") {
            if (CaptureManager.capturing) {
                client?.setScreen(null)
                CaptureManager.stop()
            } else {
                client?.setScreen(null)
                CaptureManager.start(worldNameTextEntryWidget.text)
            }
        }

        adder.add(worldNameTextEntryWidget, 2)
        adder.add(downloadButton, 1)

        entryGridWidget.refreshPositions()
        FrameLayout.setPos(entryGridWidget, 0, titleWidget.y, width, height, 0.5f, 0.05f)
        entryGridWidget.forEachChild(this::addRenderableWidget)
    }

    private fun setupBottomGrid() {
        val bottomGridWidget = createGridWidget()
        val bottomAdder = bottomGridWidget.createAdder(2)
        configButton = createButton("worldtools.gui.manager.button.config") {
            client?.setScreen(AutoConfig.getConfigScreen(WorldToolsConfig::class.java, this).get())
        }
        cancelButton = createButton("worldtools.gui.manager.button.cancel") {
            client?.setScreen(null)
        }

        bottomAdder.add(configButton, 1)
        bottomAdder.add(cancelButton, 1)

        bottomGridWidget.refreshPositions()
        FrameLayout.setPos(bottomGridWidget, 0, 0, width, height, 0.5f, .95f)
        bottomGridWidget.forEachChild(this::addRenderableWidget)
    }

    private fun createGridWidget() = GridLayout().apply {
        mainPositioner.margin(4, 4, 4, 4)
    }

    private fun createButton(textKey: String, onClick: (Button) -> Unit) =
        Button.Builder(Component.translatable(textKey), onClick).width(BUTTON_WIDTH).build()
}
