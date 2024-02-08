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

import com.jme3.math.Vector3f

/**
 * An interface for instruments that adjust their position linearly when there are multiple instances of them.
 */
interface MultipleInstancesLinearAdjustment {
    /**
     * The direction to move the instrument in, for multiple instances.
     */
    val multipleInstancesDirection: Vector3f
}
