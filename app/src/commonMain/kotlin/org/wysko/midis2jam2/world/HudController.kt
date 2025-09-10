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

import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.font.Rectangle
import com.jme3.math.ColorRGBA
import com.jme3.math.ColorRGBA.White
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.starter.configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.scale
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import kotlin.also
import kotlin.time.Duration

private const val VERTICAL_FILLBAR_SCALE = 0.7f
private const val FILLBAR_LOCATION_OFFSET = 3f
private const val FILLBAR_WIDTH = 16
private const val FILLBAR_BOX_WIDTH = 512
private const val MAXIMUM_FILLBAR_SCALE = (FILLBAR_BOX_WIDTH - (FILLBAR_LOCATION_OFFSET * 2)) / FILLBAR_WIDTH

/**
 * Controls the heads-up display.
 *
 * The HUD consists of a fillbar and a text label.
 * The fillbar is a sprite that fills up as the song progresses.
 * The text label displays the name of the song.
 */
class HudController(private val context: Midis2jam2) {
    private val root = with(context) {
        node().also {
            if (configs.find<AppSettingsConfiguration>().appSettings.onScreenElementsSettings.isShowHeadsUpDisplay) {
                app.guiNode.attachChild(it)
            }
            it.move(v3(16, 16, 0))
        }
    }

    init {
        with(root) {
            +context.assetLoader.loadSprite("SongFillbarBox.bmp").also {
                it.move(v3(0, 0, -10))
            }
            +BitmapText(context.assetManager.loadFont("Assets/Fonts/Inter_24.fnt")).apply {
                loc = v3(0f, 46f, 0f)
                text = context.fileName
                size = 24f
                color = White
                setBox(Rectangle(0f, 488f, FILLBAR_BOX_WIDTH.toFloat(), 512f))
                alignment = BitmapFont.Align.Left
                verticalAlignment = BitmapFont.VAlign.Bottom
            }
        }
    }

    private val fillbar =
        with(root) {
            +context.assetLoader.loadSprite("SongFillbar.bmp").also {
                it.move(v3(FILLBAR_LOCATION_OFFSET, FILLBAR_LOCATION_OFFSET, 10))
            }
        }

    init {
        root.children.forEach {
            when (it) {
                // Start with the HUD invisible for fade-in.
                is PictureWithFade -> it.opacity = 0f
                is BitmapText -> it.color = ColorRGBA(1f, 1f, 1f, 0f)
            }
        }
    }

    /**
     * Updates animation.
     *
     * @param time The time since the song started.
     * @param fadeValue The value to fade the HUD by.
     */
    fun tick(time: Duration, fadeValue: Float) {
        val scale = (MAXIMUM_FILLBAR_SCALE * (time / context.sequence.duration).coerceAtMost(1.0)).toFloat()
        fillbar.scale = v3(scale, VERTICAL_FILLBAR_SCALE, 1f)

        root.children.forEach {
            when (it) {
                is PictureWithFade -> it.opacity = fadeValue
                is BitmapText -> it.color = ColorRGBA(1f, 1f, 1f, fadeValue)
            }
        }
    }
}
