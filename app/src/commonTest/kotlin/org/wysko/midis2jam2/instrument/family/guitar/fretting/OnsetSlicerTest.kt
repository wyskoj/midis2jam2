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

import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.chord
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.melody
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.progression
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Grouping notes into chords by when they start.
 *
 * The old engine grouped notes by any overlap at all, so a chord whose notes rang a few milliseconds into the next
 * chord merged with it into one impossible "chord". Slicing by onset is what stops that.
 */
class OnsetSlicerTest {

    @Test
    fun `notes starting within a few milliseconds of each other are one chord`() {
        val notes = listOf(40, 47, 52, 56).mapIndexed { i, p -> FrettingNote(p, i * 0.01, 1.0) }
        val slices = OnsetSlicer.slice(notes, 6)
        assertEquals(1, slices.size, "A chord played with 10 ms of jitter is one chord, got $slices")
    }

    @Test
    fun `a strum spread over a hundred milliseconds is one chord`() {
        val notes = chord(0.0, 1.0, listOf(40, 47, 52, 56, 59, 64), spread = 0.02)
        val slices = OnsetSlicer.slice(notes, 6)
        assertEquals(1, slices.size, "A six-string downstroke taking 100 ms is one chord, got $slices")
        assertEquals(6, slices.single().size)
    }

    @Test
    fun `chords ringing into the next chord stay separate chords`() {
        val chords = listOf(listOf(40, 47, 52, 56), listOf(45, 52, 57, 61), listOf(43, 50, 55, 59))
        val slices = OnsetSlicer.slice(progression(chords, 0.5, lateOff = 0.03), 6)
        assertEquals(listOf(4, 4, 4), slices.map { it.size }, "Each chord's notes end 30 ms into the next: $slices")
    }

    @Test
    fun `a legato melody is one note per slice`() {
        val notes = melody(listOf(60, 62, 64, 65, 67, 69, 71, 72), step = 0.1, overlap = 0.01)
        assertEquals(List(8) { 1 }, OnsetSlicer.slice(notes, 6).map { it.size })
    }

    @Test
    fun `a fast run is not mistaken for a strum`() {
        // Thirty-second notes 30 ms apart, rising like a strum, but each ends before the next begins.
        val notes = melody(listOf(52, 54, 56, 57, 59, 61), step = 0.03, overlap = -0.005)
        assertEquals(List(6) { 1 }, OnsetSlicer.slice(notes, 6).map { it.size })
    }

    @Test
    fun `a re-struck pitch starts a new slice`() {
        val notes = listOf(FrettingNote(60, 0.0, 0.01), FrettingNote(60, 0.02, 0.5))
        assertEquals(2, OnsetSlicer.slice(notes, 6).size)
    }

    @Test
    fun `slices list their notes from the lowest pitch up`() {
        val notes = listOf(FrettingNote(64, 0.0, 1.0), FrettingNote(40, 0.005, 1.0), FrettingNote(52, 0.01, 1.0))
        val slice = OnsetSlicer.slice(notes, 6).single()
        assertEquals(listOf(40, 52, 64), slice.notes.map { notes[it].pitch })
    }
}
