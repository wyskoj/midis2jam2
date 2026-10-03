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

/**
 * Builds note lists for fretting tests, in seconds.
 */
internal object FrettingTestNotes {
    /** Common guitar pitches. */
    const val E2 = 40
    const val A2 = 45
    const val D3 = 50
    const val G3 = 55
    const val B3 = 59
    const val E4 = 64

    /**
     * A chord of [pitches] at [time] lasting [duration]. Each successive note starts [spread] later (a strum) and
     * every note ends [lateOff] after the chord's nominal end (as humanized MIDI often does).
     */
    fun chord(time: Double, duration: Double, pitches: List<Int>, spread: Double = 0.0, lateOff: Double = 0.0) =
        pitches.mapIndexed { i, p -> FrettingNote(p, time + i * spread, time + duration + lateOff) }

    /** A progression of [chords], each lasting [duration], starting at [start]. */
    fun progression(
        chords: List<List<Int>>,
        duration: Double,
        start: Double = 0.0,
        spread: Double = 0.0,
        lateOff: Double = 0.0,
    ) = chords.flatMapIndexed { i, pitches -> chord(start + i * duration, duration, pitches, spread, lateOff) }

    /**
     * A single-note line of [pitches], one every [step] seconds from [start], each ringing [overlap] seconds into
     * the next (negative for a gap).
     */
    fun melody(pitches: List<Int>, step: Double, start: Double = 0.0, overlap: Double = -0.01, bends: Map<Int, Double> = emptyMap()) =
        pitches.mapIndexed { i, p ->
            FrettingNote(p, start + i * step, start + (i + 1) * step + overlap, bendUp = bends[i] ?: 0.0)
        }

    /** The strings the notes of [notes] (by index) are played on, in order. */
    fun FrettingSolution.stringsOf(indices: IntRange): List<Int?> = indices.map { fingerings[it]?.string }

    /** The frets the notes of [notes] (by index) are played at, in order. */
    fun FrettingSolution.fretsOf(indices: IntRange): List<Int?> = indices.map { fingerings[it]?.fret }

    /** A compact description of every note's position, for failure messages. */
    fun FrettingSolution.describe(notes: List<FrettingNote>): String =
        notes.indices.joinToString(" ") { i ->
            val f = fingerings[i]
            "${noteName(notes[i].pitch)}=" + (f?.let { "s${it.string}f${it.fret}" } ?: "none")
        }
}
