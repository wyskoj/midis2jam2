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

val STANDARD_GLOW = ColorRGBA(0.75f, 0.75f, 0.85f, 1f)

class GlowController(
    val glowColor: ColorRGBA = STANDARD_GLOW
) {
    fun calculate(animationTime: Double): ColorRGBA {
        return ColorRGBA(
            (-0.25 * animationTime + glowColor.r).toFloat(),
            (-0.25 * animationTime + glowColor.g).toFloat(),
            (-0.25 * animationTime + glowColor.b).toFloat(),
            1.0f
        )
    }
}