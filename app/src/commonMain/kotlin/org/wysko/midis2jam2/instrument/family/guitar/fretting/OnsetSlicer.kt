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

import kotlin.math.sign

/**
 * Notes that start together and are fingered together: a chord, a strum, or a single note.
 *
 * @property index The position of this slice in the song.
 * @property notes Indices into the note list, ordered by pitch, lowest first.
 * @property time When the first note of the slice starts, in seconds.
 */
class Slice(val index: Int, val notes: IntArray, val time: Double) {
    /** The number of notes in the slice. */
    val size: Int get() = notes.size

    override fun toString(): String = "Slice#$index@${"%.3f".format(time)}${notes.contentToString()}"
}

/**
 * Groups notes into [Slice]s by when they start.
 *
 * Chord membership comes from onsets, not overlaps: a note still ringing when the next chord starts is not merged
 * into it (the decoder decides whether it keeps its string). Notes starting within [chordWindow] of the first, while
 * the first are still sounding, are one slice. A strum, whose notes arrive one after another, extends the slice up to [strumMax] as long as each
 * note follows the last within [strumGap], the pitches keep moving in one direction, and the earlier notes are
 * still sounding — which a fast single-note run's notes are not.
 */
object OnsetSlicer {
    /** Notes starting within this many seconds of the first note of a slice are one chord. */
    const val chordWindow: Double = 0.035

    /** The longest gap between successive notes of a strum. */
    const val strumGap: Double = 0.030

    /** The longest a strum may take from its first note to its last. */
    const val strumMax: Double = 0.120

    private const val STILL_SOUNDING_MARGIN = 0.030

    /** Notes starting this close together are simultaneous, however short they are. */
    private const val SIMULTANEOUS = 0.012

    /** Slices [notes] for an instrument with [stringCount] strings. */
    fun slice(notes: List<FrettingNote>, stringCount: Int): List<Slice> {
        val order = notes.indices.sortedWith(compareBy({ notes[it].start }, { notes[it].pitch }))
        val slices = mutableListOf<Slice>()
        val current = mutableListOf<Int>()
        var sliceStart = 0.0
        var lastOnset = 0.0
        var firstPitch = 0
        var lastPitch = 0

        fun close() {
            if (current.isEmpty()) return
            val sorted = current.sortedWith(compareBy({ notes[it].pitch }, { it })).toIntArray()
            slices += Slice(slices.size, sorted, sliceStart)
            current.clear()
        }

        for (i in order) {
            val note = notes[i]
            val direction = if (current.size >= 2) (lastPitch - firstPitch).sign else 0
            val joins = current.isNotEmpty() && current.none { notes[it].pitch == note.pitch } && (
                isChordNote(notes, current, note, sliceStart) || isStrumContinuation(
                    notes, current, note, sliceStart, lastOnset, lastPitch, direction, stringCount,
                )
            )
            if (!joins) {
                close()
                sliceStart = note.start
                firstPitch = note.pitch
            }
            current += i
            lastOnset = note.start
            lastPitch = note.pitch
        }
        close()
        return slices
    }

    /**
     * Whether [note] starts close enough to the slice to be part of the same chord. Notes of a chord ring together,
     * so beyond the first few milliseconds a note only joins if the slice's notes are still sounding when it starts;
     * otherwise it is the next note of a very fast run.
     */
    private fun isChordNote(notes: List<FrettingNote>, current: List<Int>, note: FrettingNote, sliceStart: Double): Boolean {
        val offset = note.start - sliceStart
        if (offset > chordWindow) return false
        return offset <= SIMULTANEOUS || current.all { notes[it].end > note.start }
    }

    private fun isStrumContinuation(
        notes: List<FrettingNote>,
        current: List<Int>,
        note: FrettingNote,
        sliceStart: Double,
        lastOnset: Double,
        lastPitch: Int,
        direction: Int,
        stringCount: Int,
    ): Boolean {
        if (current.size >= stringCount) return false
        if (note.start - lastOnset > strumGap) return false
        if (note.start - sliceStart > strumMax) return false
        val step = (note.pitch - lastPitch).sign
        if (direction != 0 && step != direction) return false
        return current.all { notes[it].end > note.start + STILL_SOUNDING_MARGIN }
    }
}
