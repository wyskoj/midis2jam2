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

package org.wysko.midis2jam2.instrument.family.animusic

import com.jme3.math.ColorRGBA

/**
 * Defines a type of [SpaceLaser].
 *
 * @property filename The texture file of the laser.
 * @property glowColor The glow color.
 * @see SpaceLaser
 */
sealed class SpaceLaserType(val filename: String, val glowColor: ColorRGBA) {
    /**
     * Sawtooth laser.
     */
    data object Saw : SpaceLaserType("Laser.bmp", ColorRGBA.Green)

    /**
     * Square laser.
     */
    data object Square : SpaceLaserType("LaserRed.png", ColorRGBA.Red)
}
