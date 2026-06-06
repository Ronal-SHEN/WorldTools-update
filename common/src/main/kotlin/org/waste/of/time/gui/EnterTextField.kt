package org.waste.of.time.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.waste.of.time.manager.CaptureManager

class EnterTextField(
    textRenderer: Font, x: Int, y: Int, width: Int, height: Int, message: Component, val client: Minecraft?
) : EditBox(textRenderer, x, y, width, height, message) {

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (CaptureManager.capturing) {
                client?.setScreen(null)
                CaptureManager.stop()
            } else {
                client?.setScreen(null)
                CaptureManager.start(value)
            }
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}