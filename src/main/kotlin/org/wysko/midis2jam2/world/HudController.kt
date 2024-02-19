/*
 * Copyright (C) 2024 Jacob Wysko
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
import com.jme3.math.ColorRGBA.White
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.starter.configuration.SettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.getType
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.scale
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3

private const val VERTICAL_FILLBAR_SCALE = 0.7f
private const val FILLBAR_LOCATION_OFFSET = 3f
private const val FILLBAR_WIDTH = 16
private const val FILLBAR_BOX_WIDTH = 512
private const val MAXIMUM_FILLBAR_SCALE = (FILLBAR_BOX_WIDTH - (FILLBAR_LOCATION_OFFSET * 2)) / FILLBAR_WIDTH

/**
 * Displays the fillbar and file name on-screen.
 */
context(Midis2jam2)
class HudController {
    private val root: Node = node {
        loc = v3(10, 10, 0)
    }.also {
        if (configs.getType(SettingsConfiguration::class).showHud) app.guiNode += it
    }

    private val fillbar = with(root) {
        +assetLoader.loadSprite("SongFillbar.bmp").also {
            loc = v3(FILLBAR_LOCATION_OFFSET, FILLBAR_LOCATION_OFFSET, 1)
        }
    }

    init {
        root += assetLoader.loadSprite("SongFillbarBox.bmp")
        BitmapText(assetManager.loadFont("Assets/Fonts/Inter_24.fnt")).apply {
            root += this
            loc = v3(0f, 44f, 0f)
            text = file.name
            size = 24f
            color = White
        }
    }

    /**
     * Updates animation.
     *
     * @param timeSinceStart The time since the song started.
     * @param fadeValue The value to fade the HUD by.
     */
    fun tick(timeSinceStart: Double, fadeValue: Float) {
        fillbar.scale =
            v3(
                x = (MAXIMUM_FILLBAR_SCALE * (timeSinceStart / file.length).coerceAtMost(1.0)).toFloat(),
                y = VERTICAL_FILLBAR_SCALE,
                z = 1f,
            )

        root.children.forEach {
            when (it) {
                is PictureWithFade -> it.opacity = fadeValue
                is BitmapText -> it.color = ColorRGBA(1f, 1f, 1f, fadeValue)
            }
        }
    }
}
