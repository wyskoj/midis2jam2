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

package org.wysko.midis2jam2.instrument.family.guitar.fretting

import kotlin.math.pow

/**
 * Where frets physically are on a neck of a given scale length, in equal temperament.
 *
 * Measuring the hand in millimetres rather than frets is what makes one model fit every instrument: the same
 * 100 mm reach covers four frets at the bottom of a guitar neck, three on a bass, and more higher up either.
 *
 * @property scaleLengthMm The vibrating length of an open string, nut to bridge.
 */
class FretGeometry(val scaleLengthMm: Double) {
    private val fingerCache = DoubleArray(MAX_CACHED_FRET + 1) { fingerPositionUncached(it) }

    /** How far fret wire [fret] is from the nut. */
    fun wirePosition(fret: Double): Double = scaleLengthMm * (1 - 2.0.pow(-fret / 12))

    /** Where a finger presses to sound [fret]: just behind the wire. The open string (0) is at the nut. */
    fun fingerPosition(fret: Int): Double =
        if (fret in 0..MAX_CACHED_FRET) fingerCache[fret] else fingerPositionUncached(fret)

    private fun fingerPositionUncached(fret: Int): Double =
        if (fret <= 0) 0.0 else wirePosition(fret - FINGER_BEHIND_WIRE)

    /** The distance a hand has to span to press [lowFret] and [highFret] at once. */
    fun span(lowFret: Int, highFret: Int): Double =
        if (lowFret <= 0 || highFret <= 0) 0.0 else fingerPosition(maxOf(lowFret, highFret)) - fingerPosition(minOf(lowFret, highFret))

    /** The highest fret a hand whose index finger is on [indexFret] reaches within [spanMm]. */
    fun reach(indexFret: Int, spanMm: Double, fretCount: Int): Int {
        var fret = indexFret
        while (fret < fretCount && span(indexFret, fret + 1) <= spanMm) fret++
        return fret
    }

    private companion object {
        const val FINGER_BEHIND_WIRE = 0.4
        const val MAX_CACHED_FRET = 36
    }
}
