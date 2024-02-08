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

import com.jme3.math.Vector3f

/**
 * Each axis in 3D space.
 */
enum class Axis(
    /** The index of this axis in a 3-element array. */
    val componentIndex: Int,
    val identity: Vector3f,
) {
    /**
     * The X-axis.
     */
    X(0, Vector3f(1f, 0f, 0f)),

    /**
     * The Y-axis.
     */
    Y(1, Vector3f(0f, 1f, 0f)),

    /**
     * The Z-axis
     */
    Z(2, Vector3f(0f, 0f, 1f));
}
