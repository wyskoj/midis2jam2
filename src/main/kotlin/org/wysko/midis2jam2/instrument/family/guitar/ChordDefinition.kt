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

import kotlinx.serialization.Serializable

/**
 * Represents a chord definition that consists of a list of notes, and a corresponding list of frets on a fretboard.
 * This class provides methods to retrieve the defined notes and map a note to its corresponding fretboard position.
 *
 * For example, a chord definition may look like:
 * ```json
 * {"notes":[-1,45,52,57,61,64],"frets":[-1,0,2,2,2,0]}
 * ```
 * Each property will2 be a list of the same length, where the index of each note in the `notes` list corresponds to the
 * index of the fret in the `frets` list.
 *
 * @property notes The list of notes in the chord. Any note represented as -1 is considered undefined.
 * @property frets The list of frets corresponding to each note in the chord.
 */
@Serializable
data class ChordDefinition(val notes: List<Byte>, val frets: List<Int>) {

    /**
     * Returns the set of notes that this chord defines.
     */
    fun getDefinedNotes(): Set<Byte> = notes.filter { it.toInt() != -1 }.toSet()

    /**
     * Returns the fretboard position of a note in this chord.
     *
     * This function will error if the note is not in the chord.
     *
     * @param note The note to find the fretboard position of.
     * @return The fretboard position of the note.
     */
    fun calculateFretboardPosition(note: Byte): FretboardPosition? = when (val string = notes.indexOf(note)) {
        -1 -> null
        else -> FretboardPosition(string, frets[string])
    }
}