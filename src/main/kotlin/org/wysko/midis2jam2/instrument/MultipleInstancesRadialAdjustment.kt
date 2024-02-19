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

package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.world.Axis

/**
 * An interface for instruments that adjust their position by rotation when there are multiple instances of them.
 */
interface MultipleInstancesRadialAdjustment {
    /**
     * The axis to rotate the instrument around.
     */
    val rotationAxis: Axis

    /**
     * The angle to rotate the instrument by, in degrees.
     */
    val rotationAngle: Float

    /**
     * The base angle, when there is only one instance of the instrument, in degrees.
     */
    val baseAngle: Float
}
