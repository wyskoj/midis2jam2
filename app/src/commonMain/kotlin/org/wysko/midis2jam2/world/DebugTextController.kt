/*
 * Copyright (C) 2025 Jacob Wysko
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

import com.jme3.asset.AssetManager
import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.shape.Quad
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.v3

private const val SCREEN_MARGIN = 64

class DebugTextController(context: Midis2jam2) {
    var enabled: Boolean = false
        set(value) {
            node.cullHint = value.ch
            field = value
        }

    private val engine = DebugTextEngine(context)
    private val node = Node().apply { cullHint = enabled.ch }.also(context.app.guiNode::attachChild)

    private val textView = context.consoleText().apply {
        loc = v3(SCREEN_MARGIN, context.app.viewPort.camera.height - SCREEN_MARGIN, 0)
    }.also(node::attachChild)

    init {
        node.attachChild(context.assetManager.darkUnderlay())
    }

    fun tick() {
        if (!enabled) return

        textView.text = engine.getText()
    }

    fun toggle() {
        enabled = !enabled
    }
}

private fun Midis2jam2.consoleText(): BitmapText = BitmapText(assetManager.consoleFont())
private fun AssetManager.consoleFont(): BitmapFont = loadFont("Interface/Fonts/Console.fnt")
private fun AssetManager.darkUnderlay(): Geometry {
    return Geometry("DebugDarken", Quad(10000f, 10000f)).apply {
        material = Material(this@darkUnderlay, "Common/MatDefs/Misc/Unshaded.j3md").apply {
            setColor("Color", ColorRGBA(0f, 0f, 0f, 0.5f))
            additionalRenderState.blendMode = RenderState.BlendMode.Alpha
        }
        loc = v3(0f, 0f, -1f)
    }
}
