/*
 * Copyright (C) 2022 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.world

import com.jme3.font.BitmapText
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.lwjgl.opengl.GL11
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.Percussion
import org.wysko.midis2jam2.util.cullHint

private val GL_RENDERER: String by lazy { GL11.glGetString(GL11.GL_RENDERER) ?: "UNKNOWN GL_RENDERER" }

/**
 * Draws debug text on the screen.
 *
 * @param context Context to the main class.
 */
class DebugTextController(val context: Midis2jam2) {

    private val text = BitmapText(context.assetManager.loadFont("Interface/Fonts/Console.fnt")).apply {
        context.app.guiNode.attachChild(this)
        cullHint = Spatial.CullHint.Always
    }

    private val percussionText = BitmapText(context.assetManager.loadFont("Interface/Fonts/Console.fnt")).apply {
        context.app.guiNode.attachChild(this)
        cullHint = Spatial.CullHint.Always
    }

    /**
     * Enables and disables the display of the debug text.
     */
    var enabled: Boolean = false
        set(value) {
            text.cullHint = value.cullHint()
            percussionText.cullHint = value.cullHint()
            field = value
        }

    /**
     * Toggles the visibility of the debug text.
     */
    fun toggle(): Unit = run { enabled = !enabled }

    /**
     * Updates the debug text.
     *
     * @param tpf the time per frame
     */
    fun tick(tpf: Float) {
        if (enabled) {
            with(text) {
                setLocalTranslation(0f, context.app.viewPort.camera.height.toFloat(), 0f)
                text = context.debugText(tpf, context.timeSinceStart)
            }
            with(percussionText) {
                setLocalTranslation(1000f, context.app.viewPort.camera.height.toFloat(), 0f)
                text = context.debugTextPercussion()
            }
        }
    }
}

private fun Midis2jam2.debugTextPercussion(): String {
    return buildString {
        append(this@debugTextPercussion.instruments.firstOrNull { it is Percussion } ?: "NO PERCUSSION")
    }
}

/**
 * Generates debug text.
 */
private fun Midis2jam2.debugText(tpf: Float, time: Double): String {
    return buildString {
        /* midis2jam2 version and build */
        append("midis2jam2 v${this@debugText.version} (built at ${this@debugText.build})\n")

        /* computer operating system and renderer */
        appendLine()
        append("OS: $operatingSystem\n")
        append("Graphics: $GL_RENDERER\n")

        /* settings */
        appendLine()
        append(
            "Properties:\n${
                this@debugText.properties.entries.joinToString(separator = "\n")
            }\n"
        )

        /* fps and time */
        appendLine()
        append("${String.format("%.0f", 1 / tpf)} fps\n")
        append("${String.format("%.2f", time)}s / ${String.format("%.2f", this@debugText.file.length)}s\n")

        /* camera position and rotation */
        appendLine()
        append("cam: ${this@debugText.app.camera.location.sigFigs()} / ${this@debugText.app.camera.rotation.sigFigs()}\n")

        /* instruments strings */
        appendLine()
        append("${this@debugText.instruments.filter { it !is Percussion }.joinToString("")}\n")
    }
}

private fun Quaternion.sigFigs(): String {
    return String.format(
        "[ w = %5.2f, x = %5.2f, y = %5.2f, z = %5.2f ]",
        w, x, y, z
    )
}

private fun Vector3f.sigFigs(): String {
    return String.format(
        "[ x = %7.2f, y = %7.2f, z = %7.2f ]",
        x, y, z
    )
}

private val operatingSystem = "${System.getProperty("os.name")} / ${System.getProperty("os.version")}"