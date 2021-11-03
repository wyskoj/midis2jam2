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

/**
 * The fretting engine handles the calculations of determining which frets to press. It attempts to calculate the best
 * fretboard position for a given note, given context. The context that the engine takes into consideration is
 * determined by implementations of this interface.
 */
interface FrettingEngine {

    /**
     * Calculates and returns the best fretboard location for the specified [midiNote] with temporal consideration. If
     * no possible positions exists (all the strings are occupied), returns null.
     */
    @Contract(pure = true)
    fun bestFretboardPosition(midiNote: Int): FretboardPosition?

    /** Applies the usage of this fretboard [position], occupying the string. */
    fun applyFretboardPosition(position: FretboardPosition)

    /** Releases a [string], stopping the animation on it and allowing it to be used for another note. */
    fun releaseString(string: Int)

    /** Returns the current fret configuration. */
    val frets: IntArray
}