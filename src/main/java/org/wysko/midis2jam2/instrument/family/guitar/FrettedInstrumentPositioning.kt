/*
 * Copyright (C) 2021 Jacob Wysko
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
 * Fretted instruments in M2J2 are composed of several parts:
 *
 *  * Upper strings - These are the strings you see when a string is not being played.
 *  * Lower strings - These are the wobbly, animated strings you see when a string is being played.
 *  * Note finger - The small, yellow dot that hides the seam between upper and lower strings
 *
 * When a note on a string is to be played, the upper string scales by a factor x, and the bottom string scales
 * by a factor 1 - x. This way, they meet at the correct spot on the fretboard. At that position, the note finger
 * is placed to hide the seam between the upper and lower strings.
 *
 * @see FretHeightCalculator
 */
open class FrettedInstrumentPositioning(
    /** The y-coordinate of the "upper strings". */
    val upperY: Float,

    /** The y-coordinate of the "lower strings". */
    val lowerY: Float,

    /** Scales of resting strings. */
    val restingStrings: Array<Vector3f>,

    /** The x-coordinates of upper strings. */
    val upperX: FloatArray,

    /** The x-coordinates of lower strings. */
    val lowerX: FloatArray,

    /** Calculator for fret heights. */
    val fretHeights: FretHeightCalculator
) {
    /** This provides the vertical position of the note fingers. */
    val fingerVerticalOffset: Vector3f = Vector3f(0f, upperY, 0f)

    /** Contains positioning for strings with a Z value. */
    class FrettedInstrumentPositioningWithZ(
        topY: Float,
        bottomY: Float,
        restingStrings: Array<Vector3f>,
        topX: FloatArray,
        bottomX: FloatArray,
        fretHeights: FretHeightCalculator,
        /** The z-coordinate of the "upper strings". */
        val topZ: FloatArray,
        /** The z-coordinate of the "lower strings". */
        val bottomZ: FloatArray
    ) : FrettedInstrumentPositioning(topY, bottomY, restingStrings, topX, bottomX, fretHeights)
}