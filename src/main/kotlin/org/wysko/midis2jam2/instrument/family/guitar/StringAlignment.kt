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

package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.Vector3f

/**
 * After scaling the strings, they will move backwards slightly because their origin point is not positioned over the
 * string. This value roughly brings them forward back to their initial position.
 */
const val FORWARD_OFFSET: Float = 0.125f

/**
 * Defines a JSON structure for storing information related to the alignment of strings on a fretted instrument.
 *
 * @property lowerVerticalOffset the y-offset of the "lower" strings
 * @property lowerHorizontalOffsets the x-offsets of each "lower" string
 * @property upperVerticalOffset the y-offset of the "upper" strings
 * @property upperHorizontalOffsets the x-offsets of each "upper" string
 * @property rotations the rotations of each string
 * @property scales the scales of each string
 */
@kotlinx.serialization.Serializable
class StringAlignment(
    val lowerVerticalOffset: Float,
    val lowerHorizontalOffsets: FloatArray,
    val upperVerticalOffset: Float,
    val upperHorizontalOffsets: FloatArray,
    val rotations: FloatArray,
    val scales: FloatArray
) {
    /** Returns the scales of this alignment as [Vector3f]s. */
    val scalesVectors: Array<Vector3f> get() = scales.map { Vector3f(it, 1f, it) }.toTypedArray()
}
