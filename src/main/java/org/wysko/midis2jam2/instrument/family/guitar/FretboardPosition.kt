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

import org.jetbrains.annotations.Contract
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents a position on the fretboard. The position consists of two components: the string and fret.
 *
 * The string is notated as its index, where the lowest/thickest string is `0`. The fret is notated as its index
 * from the top of the fretboard, where `0` is no fret, `1` is the first fret, etc.
 *
 * For example, the `FretBoardPosition` for the lowest string with an open fret is `{string=0,fret=0}`.
 */
data class FretboardPosition(
    /** The string of the position. */
    val string: Int,
    /** The fret of the position. */
    val fret: Int,
) {
    /** Calculates and returns the distance from this position to [other], but ignores variable spacing. Good ol' distance formula. */
    @Contract(pure = true)
    fun distance(other: FretboardPosition): Double {
        return sqrt((string.toDouble() - other.string).pow(2.0) + (fret.toDouble() - other.fret).pow(2.0))
    }
}