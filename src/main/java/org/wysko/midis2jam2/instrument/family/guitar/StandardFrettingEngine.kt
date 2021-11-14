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
import java.util.Comparator.comparingDouble

/**
 * The fretting engine handles the calculations of determining which frets to press.
 *
 *
 * This fretting algorithm attempts to use a running average of the last used frets to avoid drifting across the
 * fretboard. This does however mean that certain shapes on the fretboard may change, even though the notes are the
 * same. This solves an issue from MIDIJam (reduces fretboard sliding) but introduces another (shapes are not
 * consistent).
 */
class StandardFrettingEngine(
    /** The number of strings on the instrument. */
    private val numberOfStrings: Int,

    /** The number of frets on the instrument. */
    val numberOfFrets: Int,

    /* An array that contains the MIDI note value of each string as played open. */
    private val openStringMidiNotes: IntArray
) : FrettingEngine {

    override val frets: IntArray = IntArray(numberOfStrings).apply { fill(-1) }

    /** The lowest note this engine with deal with. */
    private val rangeLow = openStringMidiNotes.first()

    /** The highest note this engine will deal with. */
    private val rangeHigh = openStringMidiNotes.last() + numberOfFrets

    /** The list of fretboard positions in the running average. */
    private val runningAverage: MutableList<FretboardPosition> = ArrayList()

    /**
     * Calculates the best fretboard location for the specified MIDI note with temporal consideration. If no possible
     * positions exists (all the strings are occupied), returns null.
     *
     * @param midiNote the MIDI note to find the best fretboard position
     * @return the best fretboard position, or null if one does not exist
     */
    @Contract(pure = true)
    override fun bestFretboardPosition(midiNote: Int): FretboardPosition? {
        val possiblePositions: MutableList<FretboardPosition> = ArrayList()
        /* If the note is in range */
        if (midiNote in rangeLow..rangeHigh) {
            /* For each string */
            for (i in 0 until numberOfStrings) {
                /* Calculate the fret that the note would land on */
                val fret = midiNote - openStringMidiNotes[i]

                /* If this is impossible to play on this string */
                if (fret < 0 || fret > numberOfFrets || frets[i] != -1) {
                    /* Skip this string */
                    continue
                }

                /* Otherwise, add this to a list of possible positions */
                possiblePositions.add(FretboardPosition(i, fret))
            }
        }

        /* Sort possible positions by distance to running average */
        possiblePositions.sortWith(comparingDouble { o: FretboardPosition ->
            o.distance(runningAveragePosition())
        })

        /* Return the note with the least distance, or null if there are no available spots */
        return possiblePositions.firstOrNull()
    }

    /** Applies the usage of this fretboard position, occupying the string. Adds the position to the running average. */
    override fun applyFretboardPosition(position: FretboardPosition) {
        /* Apply this position and add it to the running average */
        with(position) {
            frets[string] = fret
            runningAverage.add(this)
        }

        /* Truncate running average if over limit */
        if (runningAverage.size > RUNNING_AVERAGE_COUNT) {
            runningAverage.removeAt(0)
        }
    }

    /** Calculates the running average position. If no frets have been previously applied, returns the position at `0,0`. */
    @Contract(pure = true)
    fun runningAveragePosition(): FretboardPosition =
        /* If there are no notes yet played, assume the average is the open note on the lowest string */
        if (runningAverage.isEmpty()) {
            FretboardPosition(0, 0)
        } else {
            /* Compute the average of the strings and frets */
            FretboardPosition(
                runningAverage.minOf { it.string },
                runningAverage.minOf { it.fret }
            )
        }

    /** Releases a string, stopping the animation on it and allowing it to be used for another note. */
    override fun releaseString(string: Int) {
        require(string in 0 until numberOfStrings) { "Can't release a string that does not exist." }
        frets[string] = -1
    }


    companion object {
        private const val RUNNING_AVERAGE_COUNT = 10
    }

    init {
        require(openStringMidiNotes.size == numberOfStrings) {
            "The number of strings does not equal the number of data in the open string MIDI notes."
        }
    }
}