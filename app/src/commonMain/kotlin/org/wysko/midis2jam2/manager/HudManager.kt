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
import com.jme3.app.SimpleApplication
import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.math.ColorRGBA
import com.jme3.font.Rectangle
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.manager.PlaybackManager.Companion.time
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.scale
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Sprite

private const val VERTICAL_FILLBAR_SCALE = 0.7f
private const val FILLBAR_LOCATION_OFFSET = 3f
private const val FILLBAR_WIDTH = 16
private const val FILLBAR_BOX_WIDTH = 512
private const val MAXIMUM_FILLBAR_SCALE = (FILLBAR_BOX_WIDTH - (FILLBAR_LOCATION_OFFSET * 2)) / FILLBAR_WIDTH

class HudManager : BaseManager() {
    private var fadeManager: FadeManager? = null
    private lateinit var root: Node
    private lateinit var fillbar: Sprite

    override fun initialize(app: Application) {
        super.initialize(app)
        fadeManager = application.stateManager.getState(FadeManager::class.java)

        val fillbarBox = loadSprite("Assets/SongFillbarBox.bmp").apply {
            loc = v3(0, 0, -10)
        }

        val text = BitmapText(getDisplayFont()).apply {
            alignment = BitmapFont.Align.Left
            color = ColorRGBA.White
            loc = v3(0, 46, 0)
            setBox(Rectangle(0f, 488f, FILLBAR_BOX_WIDTH.toFloat(), 512f))
            size = 24f
            text = context.fileName
            verticalAlignment = BitmapFont.VAlign.Bottom
        }

        fillbar = loadSprite("Assets/SongFillbar.bmp").apply {
            loc = v3(FILLBAR_LOCATION_OFFSET, FILLBAR_LOCATION_OFFSET, 10)
        }

        root = Node().apply {
            loc = v3(16, 16, 0)
            attachChild(fillbar)
            attachChild(fillbarBox)
            attachChild(text)
        }
    }

    override fun update(tpf: Float) {
        super.update(tpf)
        val scale = (MAXIMUM_FILLBAR_SCALE * (app.time / context.sequence.duration).coerceAtMost(1.0)).toFloat()
        fillbar.scale = v3(scale, VERTICAL_FILLBAR_SCALE, 1f)

        fadeManager?.let { fadeManager ->
            root.children.forEach {
                setSpriteOpacity(it, fadeManager.fadeValue)
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        (application as SimpleApplication).guiNode.attachChild(root)
    }

    override fun onDisable() {
        super.onDisable()
        (application as SimpleApplication).guiNode.detachChild(root)
    }

    private fun setSpriteOpacity(spatial: Spatial?, alpha: Float) {
        when (spatial) {
            is Sprite -> spatial.opacity = alpha
            is BitmapText -> spatial.color = ColorRGBA(1f, 1f, 1f, alpha)
        }
    }

    private fun loadSprite(textureName: String): Sprite = Sprite(application.assetManager, textureName)

    private fun getDisplayFont(): BitmapFont = application.assetManager.loadFont("Assets/Fonts/Inter_24.fnt")
}