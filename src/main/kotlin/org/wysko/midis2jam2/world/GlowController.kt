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

import com.jme3.math.ColorRGBA

/** Standard glow color. */
val STANDARD_GLOW: ColorRGBA = ColorRGBA(0.75f, 0.75f, 0.85f, 1f)

/** Yellow-tinted glow for vibrating strings. */
val STRING_GLOW: ColorRGBA = ColorRGBA(0.69f, 0.72f, 0.18f, 1f)

/** Similar to [STANDARD_GLOW] but slightly dimmer. */
val DIM_GLOW: ColorRGBA = ColorRGBA(0.67f, 0.67f, 0.67f, 1f)

/**
 * Animates a decaying glow effect.
 *
 * @param glowColor the color of the glow
 */
class GlowController(
    val glowColor: ColorRGBA = STANDARD_GLOW
) {
    /**
     * Given the current [animationTime], determines the correct color for a decaying effect.
     */
    fun calculate(animationTime: Double): ColorRGBA {
        return ColorRGBA(
            (-0.25 * animationTime + glowColor.r).toFloat(),
            (-0.25 * animationTime + glowColor.g).toFloat(),
            (-0.25 * animationTime + glowColor.b).toFloat(),
            1.0f
        )
    }
}
