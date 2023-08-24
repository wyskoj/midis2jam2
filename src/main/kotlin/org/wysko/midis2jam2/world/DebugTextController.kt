/*
 * Copyright (C) 2023 Jacob Wysko
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
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.shape.Quad
import org.lwjgl.opengl.GL11
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.Percussion
import org.wysko.midis2jam2.util.cullHint
import org.wysko.midis2jam2.util.wrap

private val OPERATING_SYSTEM by lazy {
    "${System.getProperty("os.arch")} / ${System.getProperty("os.name")} / ${
        System.getProperty(
            "os.version"
        )
    }"
}
private val GL_RENDERER: String by lazy { GL11.glGetString(GL11.GL_RENDERER) ?: "UNKNOWN GL_RENDERER" }
private val JVM_INFORMATION by lazy {
    "${System.getProperty("java.vm.name")}, ${System.getProperty("java.vm.vendor")}, ${System.getProperty("java.vm.version")}"
}

/**
 * Draws debug text on the screen.
 *
 * @param context Context to the main class.
 */
class DebugTextController(val context: Midis2jam2) {

    private val text = BitmapText(context.assetManager.loadFont("Interface/Fonts/Console.fnt")).apply {
        context.app.guiNode.attachChild(this)
        setLocalTranslation(16f, context.app.viewPort.camera.height - 16f, 0f)
        cullHint = Spatial.CullHint.Always
    }

    private val percussionText = BitmapText(context.assetManager.loadFont("Interface/Fonts/Console.fnt")).apply {
        context.app.guiNode.attachChild(this)
        setLocalTranslation(1024f, context.app.viewPort.camera.height - 16f, 0f)
        cullHint = Spatial.CullHint.Always
    }

    private val darkBackground = Geometry("DebugDarken", Quad(10000f, 10000f)).apply {
        material = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
            setColor("Color", ColorRGBA(0f, 0f, 0f, 0.5f))
            additionalRenderState.blendMode = RenderState.BlendMode.Alpha
        }
        setLocalTranslation(0f, 0f, -1f)
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
            darkBackground.cullHint = value.cullHint()
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
                text = context.debugText(tpf, context.timeSinceStart)
            }
            with(percussionText) {
                text = context.debugTextPercussion()
            }
        }
    }
}

private fun Midis2jam2.debugTextPercussion(): String {
    return buildString {
        this@debugTextPercussion.instruments.firstOrNull { it is Percussion }?.let {
            appendLine("Percussion:")
            appendLine(it.toString())
        } ?: run {
            appendLine("NO PERCUSSION")
        }
    }
}

/**
 * Generates debug text.
 */
private fun Midis2jam2.debugText(tpf: Float, time: Double): String {
    return buildString {
        /* midis2jam2 version and build */
        append(
            "midis2jam2 v${this@debugText.version} (built at ${this@debugText.build})\n"
        )

        /* computer operating system and renderer */
        appendLine()
        appendLine("OS:  $OPERATING_SYSTEM")
        appendLine("GPU: $GL_RENDERER")
        appendLine("JVM: $JVM_INFORMATION")
        appendLine(
            "JRE: ${
                with(Runtime.getRuntime()) {
                    "${availableProcessors()} Cores / ${freeMemory() / 1024 / 1024}/${totalMemory() / 1024 / 1024} MB / ${maxMemory() / 1024 / 1024}MB max"
                }
            }"
        )
        appendLine("${String.format("%.0f", 1 / tpf)} FPS")

        /* settings */
        appendLine()
        appendLine("Settings:")
        appendLine(this@debugText.configs.joinToString().wrap(80))

        appendLine()
        appendLine("File:")
        with(this@debugText.file) {
            appendLine("$name - $division TPQN")
            appendLine("${String.format("%.2f", time)}s / ${String.format("%.2f", length)}s")
        }

        /* camera position and rotation */
        appendLine()
        appendLine("Camera:")
        appendLine("${this@debugText.app.camera.location.sigFigs()} / ${this@debugText.app.camera.rotation.sigFigs()}")
        appendLine(this@debugText.cameraState)

        /* instruments strings */
        appendLine()
        appendLine("Instruments:")
        append("${this@debugText.instruments.filter { it !is Percussion }.joinToString("")}\n")
    }
}

private fun Quaternion.sigFigs(): String {
    return String.format(
        "%5.2f / %5.2f / %5.2f / %5.2f",
        x,
        y,
        z,
        w
    )
}

private fun Vector3f.sigFigs(): String {
    return String.format(
        "%7.2f / %7.2f / %7.2f",
        x,
        y,
        z
    )
}