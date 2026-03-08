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

package org.wysko.midis2jam2.manager

import com.jme3.app.Application
import com.jme3.font.BitmapText
import com.jme3.input.controls.ActionListener
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.shape.Quad
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_DEBUG
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.DebugTextEngine

private const val SCREEN_MARGIN = 64

class DebugTextManager : BaseManager(), ActionListener {
    private lateinit var engine: DebugTextEngine
    private lateinit var textView: BitmapText
    private val node = Node()

    override fun initialize(app: Application) {
        super.initialize(app)
        app.inputManager.addListener(this, ACTION_DEBUG)

        engine = DebugTextEngine(context)
        textView = createBitmapText().apply {
            loc = v3(SCREEN_MARGIN, context.app.viewPort.camera.height - SCREEN_MARGIN, 0)
        }.also(node::attachChild)
        node.attachChild(createDarkUnderlay())
    }

    override fun onEnable() {
        app.guiNode.attachChild(node)
    }

    override fun onDisable() {
        app.guiNode.detachChild(node)
    }

    override fun update(tpf: Float) {
        super.update(tpf)
        textView.text = engine.getText()
    }

    override fun onAction(name: String?, isPressed: Boolean, tpf: Float) {
        if (!isPressed) return

        when (name) {
            ACTION_DEBUG -> isEnabled = !isEnabled
        }
    }

    private fun createBitmapText(): BitmapText {
        val font = app.assetManager.loadFont("Interface/Fonts/Console.fnt")
        return BitmapText(font)
    }

    private fun createDarkUnderlay(): Geometry = Geometry("DebugDarken", Quad(10000f, 10000f)).apply {
        material = Material(app.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
            setColor("Color", ColorRGBA(0f, 0f, 0f, 0.5f))
            additionalRenderState.blendMode = RenderState.BlendMode.Alpha
        }
        loc = v3(0f, 0f, -1f)
    }
}
