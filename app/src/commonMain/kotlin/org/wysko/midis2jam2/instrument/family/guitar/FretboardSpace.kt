/*
 * Copyright (C) 2026 Jacob Wysko
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
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ
import org.wysko.midis2jam2.util.Utils
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Where things are on a fretted instrument's neck, in the instrument's geometry space.
 *
 * The note-finger dots place themselves through this, as can anything else that needs to find a string and fret.
 *
 * @property positioning The instrument's string and fret geometry.
 * @property fretCount The highest fret.
 */
class FretboardSpace(val positioning: FrettedInstrumentPositioning, val fretCount: Int) {
    private val stringHeight: Float = positioning.upperY - positioning.lowerY

    /** The number of strings. */
    val stringCount: Int get() = positioning.upperX.size

    /** A unit vector along the neck, from the nut toward the bridge. */
    val along: Vector3f

    /** A unit vector across the neck, from the lowest string toward the highest, perpendicular to [along]. */
    val across: Vector3f

    /** A unit vector out of the fretboard, toward the side the strings are played from. */
    val normal: Vector3f

    init {
        val nut = pointAt((stringCount - 1) / 2.0, 0f)
        val bridge = pointAt((stringCount - 1) / 2.0, 1f)
        along = bridge.subtract(nut).normalizeLocal()
        val lowToHigh = pointAt(stringCount - 1.0, 0f).subtract(pointAt(0.0, 0f))
        across = lowToHigh.subtract(along.mult(lowToHigh.dot(along))).normalizeLocal()
        // The strings sit in front of the fretboard (a positive z offset) on every model.
        normal = along.cross(across).normalizeLocal().let { if (it.z < 0) it.negateLocal() else it }
    }

    /** How far along the string [fret] is: `0` at the nut, following the model's fret table. Fractions interpolate. */
    fun fretDistance(fret: Double): Float {
        val clamped = fret.coerceIn(0.0, fretCount.toDouble())
        val lower = floor(clamped).toInt()
        val upper = (lower + 1).coerceAtMost(fretCount)
        return Utils.lerp(scale(lower), scale(upper), clamped - lower).toFloat()
    }

    /** How far along the string a note at [fret], bent by [bend] semitones, is drawn. */
    fun bentDistance(fret: Int, bend: Double): Float {
        // Find the whole number of semitones bent, and the microtonal remainder.
        val semitones = if (bend > 0) floor(bend).toInt() else ceil(bend).toInt()
        val fraction = bend % 1
        val adjusted = (fret + semitones).coerceIn(0..fretCount)
        return if (fraction > 0) {
            Utils.lerp(scale(adjusted), scale((adjusted + 1).coerceAtMost(fretCount)), fraction).toFloat()
        } else {
            Utils.lerp(scale(adjusted), scale((adjusted - 1).coerceAtLeast(0)), -fraction).toFloat()
        }
    }

    /**
     * Where a note on [string] (fractions fall between strings) is drawn, [distance] along the string (see
     * [fretDistance]).
     */
    fun pointAt(string: Double, distance: Float): Vector3f {
        val clamped = string.coerceIn(0.0, stringCount - 1.0)
        val low = floor(clamped).toInt()
        val high = (low + 1).coerceAtMost(stringCount - 1)
        val t = (clamped - low).toFloat()
        return pointOnString(low, distance).interpolateLocal(pointOnString(high, distance), t)
    }

    /** Where a note on [string] at [fret] (fractions interpolate) is drawn. */
    fun pointOn(string: Double, fret: Double): Vector3f = pointAt(string, fretDistance(fret))

    private fun pointOnString(string: Int, distance: Float): Vector3f = with(positioning) {
        Vector3f(
            (lowerX[string] - upperX[string]) * distance + upperX[string],
            fingerVerticalOffset.y - stringHeight * distance,
            if (this is FrettedInstrumentPositioningWithZ) {
                ((topZ[string] - bottomZ[string]) * distance + topZ[string]) * -1.3f - 2f
            } else {
                0f
            },
        )
    }

    private fun scale(fret: Int): Float = positioning.fretHeights.calculateScale(fret)
}
