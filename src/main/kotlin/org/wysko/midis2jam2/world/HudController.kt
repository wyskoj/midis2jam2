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
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2

private const val VERTICAL_FILLBAR_SCALE = 0.7f
private const val FILLBAR_LOCATION_OFFSET = 3f
private const val FILLBAR_WIDTH = 16
private const val FILLBAR_BOX_WIDTH = 512
private const val MAXIMUM_FILLBAR_SCALE = (FILLBAR_BOX_WIDTH - (FILLBAR_LOCATION_OFFSET * 2)) / FILLBAR_WIDTH

/**
 * Displays the fillbar and file name on-screen.
 *
 * @param context context to the main class
 */
class HudController(val context: Midis2jam2) {

    /**
     * The node that contains the fillbar and text.
     */
    private val node: Node = Node().also {
        context.app.guiNode.attachChild(it)
        it.move(10f, 10f, 0f)
    }

    /**
     * The name text.
     */
    private val text = BitmapText(context.assetManager.loadFont("Interface/Fonts/Default.fnt")).apply {
        node.attachChild(this)
        text = context.file.name
        this.move(0f, 40f, 0f)
        color = ColorRGBA(1f, 1f, 1f, 1f)
    }

    /**
     * The red fillbar.
     */
    private val fillbar = context.assetManager.loadSprite("SongFillbar.bmp").also {
        node.attachChild(it)
        it.move(FILLBAR_LOCATION_OFFSET, FILLBAR_LOCATION_OFFSET, 1f)

    }

    init {
        context.assetManager.loadSprite("SongFillbarBox.bmp").also {
            node.attachChild(it)
        }
    }

    /**
     * Updates animation.
     */
    fun tick(timeSinceStart: Double, fadeValue: Float) {
        fillbar.localScale = Vector3f(
            (MAXIMUM_FILLBAR_SCALE * (timeSinceStart / context.file.length)).toFloat(),
            VERTICAL_FILLBAR_SCALE,
            1f
        )
        node.children.forEach {
            when (it) {
                is Sprite -> {
                    it.opacity = fadeValue
                }
                is BitmapText -> {
                    it.color = ColorRGBA(1f, 1f, 1f, fadeValue)
                }
            }
        }
    }
}