/*
 * Copyright (C) 2025 Jacob Wysko
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

import java.util.Comparator.comparingDouble

private const val RUNNING_AVERAGE_COUNT = 10

/**
 * The fretting engine handles the calculations of determining what frets to press.
 *
 * This fretting algorithm attempts to use a running average
 * of the last used frets to avoid drifting across the fretboard.
 * This does, however, mean that certain shapes on the fretboard may change, even though the notes are the same.
 *
 * @property numberOfStrings The number of strings on the instrument.
 * @property numberOfFrets The number of frets on the instrument.
 * @param openStringMidiNotes An array that contains the MIDI note value of each string as played open.
 */
class StandardFrettingEngine(
    val numberOfStrings: Int,
    val numberOfFrets: Int,
    private val openStringMidiNotes: IntArray
) : FrettingEngine {

    init {
        require(openStringMidiNotes.size == numberOfStrings) {
            "The number of strings does not equal the number of data in the open string MIDI notes."
        }
    }

    override val frets: IntArray = IntArray(numberOfStrings).apply { fill(-1) }
    private val runningAverage: MutableList<FretboardPosition> = ArrayList()

    /**
     * Calculates the best fretboard location for the specified MIDI note with temporal consideration. If no possible
     * position exists (all the strings are occupied), returns `null`.
     *
     * @param midiNote The MIDI note to find the best fretboard position.
     * @return The best fretboard position, or `null` if one doesn't exist.
     */
    override fun bestFretboardPosition(midiNote: Byte): FretboardPosition? {
        val possiblePositions: MutableList<FretboardPosition> = allPossibleFretboardPositions(midiNote)

        // Sort possible positions by distance to running average
        possiblePositions.sortWith(
            comparingDouble { o: FretboardPosition ->
                o.distance(runningAveragePosition())
            }
        )

        return possiblePositions.firstOrNull()
    }

    /**
     * Applies the usage of this fretboard position, occupying the string. Adds the position to the running average.
     *
     * @param position The fretboard position to apply.
     */
    override fun applyFretboardPosition(position: FretboardPosition) {
        // Apply this position and add it to the running average.
        with(position) {
            frets[string] = fret
            runningAverage.add(this)
        }

        // Truncate running average if over limit
        if (runningAverage.size > RUNNING_AVERAGE_COUNT) {
            runningAverage.removeAt(0)
        }
    }

    /** Releases a string, stopping the animation on it and allowing it to be used for another note. */
    override fun releaseString(string: Int) {
        require(string in 0 until numberOfStrings) { "Can't release a string that does not exist." }
        frets[string] = -1
    }

    /**
     * Calculates the lowest fretboard location for the specified MIDI note with temporal consideration.
     * If no possible position exists (all the strings are occupied), returns `null`.
     *
     * @param midiNote The MIDI note to find the best fretboard position.
     * @param occupiedStrings The strings that are already occupied.
     * @param lowestByFret If `true`, the lowest fret is prioritized over the lowest string.
     */
    fun lowestFretboardPosition(
        midiNote: Byte,
        occupiedStrings: List<Int>,
        lowestByFret: Boolean = false
    ): FretboardPosition? = allPossibleFretboardPositions(midiNote)
        .filter { !occupiedStrings.contains(it.string) }
        .minByOrNull { if (lowestByFret) it.fret else it.string }

    private fun allPossibleFretboardPositions(midiNote: Byte): MutableList<FretboardPosition> =
        openStringMidiNotes.mapIndexed { index, openStringNote ->
            val fret = midiNote - openStringNote
            if (fret !in 0..numberOfFrets || frets[index] != -1) {
                return@mapIndexed null
            } else {
                return@mapIndexed FretboardPosition(index, fret)
            }
        }.filterNotNull().toMutableList()

    private fun runningAveragePosition(): FretboardPosition =
        if (runningAverage.isEmpty()) {
            // If there are no notes yet played, assume the average is the open note on the lowest string.
            FretboardPosition(0, 0)
        } else {
            // Compute the average of the strings and frets
            FretboardPosition(
                runningAverage.minOf { it.string },
                runningAverage.minOf { it.fret }
            )
        }
}
