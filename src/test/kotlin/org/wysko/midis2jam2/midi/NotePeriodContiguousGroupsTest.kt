/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.midi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("RemoveRedundantBackticks")
class NotePeriodContiguousGroupsTest {

    @Test
    fun `Empty`() {
        assertEquals(emptyList<NotePeriodGroup>(), emptyList<NotePeriod>().contiguousGroups())
    }

    @Test
    fun `One note`() {
        val notePeriod = NotePeriod(
            64,
            1.0,
            2.0,
            MidiNoteOnEvent(96, 0, 64, 127),
            MidiNoteOffEvent(192, 0, 64)
        )
        assertEquals(
            listOf(NotePeriodGroup(listOf(notePeriod))),
            listOf(notePeriod).contiguousGroups()
        )
    }

    @Test
    fun `Two simultaneous notes`() {
        val np = arrayOf(
            NotePeriod(
                64,
                1.0,
                2.0,
                MidiNoteOnEvent(96, 0, 64, 127),
                MidiNoteOffEvent(192, 0, 64)
            ), NotePeriod(
                71,
                1.0,
                2.0,
                MidiNoteOnEvent(96, 0, 71, 127),
                MidiNoteOffEvent(192, 0, 71)
            )
        )
        assertEquals(
            listOf(NotePeriodGroup(listOf(*np))),
            listOf(*np).contiguousGroups()
        )
    }

    @Test
    fun `Three notes with some overlap`() {
        val np = arrayOf(
            NotePeriod(
                64,
                0.0,
                2.0,
                MidiNoteOnEvent(0, 0, 64, 127),
                MidiNoteOffEvent(2, 0, 64)
            ),
            NotePeriod(
                71,
                1.0,
                3.0,
                MidiNoteOnEvent(1, 0, 71, 127),
                MidiNoteOffEvent(3, 0, 71)
            ),
            NotePeriod(
                71,
                2.0,
                4.0,
                MidiNoteOnEvent(2, 0, 71, 127),
                MidiNoteOffEvent(4, 0, 71)
            )
        )
        assertEquals(
            listOf(NotePeriodGroup(listOf(*np))),
            listOf(*np).contiguousGroups()
        )
    }

    @Test
    fun `Two notes with no overlap`() {
        val np = arrayOf(
            NotePeriod(
                64,
                0.0,
                2.0,
                MidiNoteOnEvent(0, 0, 64, 127),
                MidiNoteOffEvent(2, 0, 64)
            ),
            NotePeriod(
                71,
                2.0,
                4.0,
                MidiNoteOnEvent(2, 0, 71, 127),
                MidiNoteOffEvent(4, 0, 71)
            )
        )
        assertEquals(
            listOf(NotePeriodGroup(listOf(np[0])), NotePeriodGroup(listOf(np[1]))),
            listOf(*np).contiguousGroups()
        )
    }

    @Test
    fun `Four notes, where only the first and last two have overlap`() {
        val np = arrayOf(
            NotePeriod(
                64,
                0.0,
                2.0,
                MidiNoteOnEvent(0, 0, 64, 127),
                MidiNoteOffEvent(2, 0, 64)
            ),
            NotePeriod(
                71,
                1.0,
                3.0,
                MidiNoteOnEvent(1, 0, 71, 127),
                MidiNoteOffEvent(3, 0, 71)
            ),
            NotePeriod(
                64,
                4.0,
                6.0,
                MidiNoteOnEvent(4, 0, 64, 127),
                MidiNoteOffEvent(6, 0, 64)
            ),
            NotePeriod(
                71,
                5.0,
                7.0,
                MidiNoteOnEvent(5, 0, 71, 127),
                MidiNoteOffEvent(7, 0, 71)
            )
        )
        assertEquals(
            listOf(
                NotePeriodGroup(listOf(*np.copyOfRange(0, 2))),
                NotePeriodGroup(listOf(*np.copyOfRange(2, 4)))
            ),
            listOf(*np).contiguousGroups()
        )
    }

    @Test
    fun `Three notes, where the second two elapse as the first one elapses`() {
        val np = arrayOf(
            NotePeriod(
                64,
                0.0,
                6.0,
                MidiNoteOnEvent(0, 0, 64, 127),
                MidiNoteOffEvent(6, 0, 64)
            ),
            NotePeriod(
                71,
                1.0,
                2.0,
                MidiNoteOnEvent(1, 0, 71, 127),
                MidiNoteOffEvent(2, 0, 71)
            ),
            NotePeriod(
                64,
                4.0,
                5.0,
                MidiNoteOnEvent(4, 0, 64, 127),
                MidiNoteOffEvent(5, 0, 64)
            ),
        )
        assertEquals(listOf(NotePeriodGroup(listOf(*np))), listOf(*np).contiguousGroups())
    }

    @Test
    fun `Two consecutive notes`() {
        val np = arrayOf(
            NotePeriod(
                64,
                0.0,
                1.0,
                MidiNoteOnEvent(0, 0, 64, 127),
                MidiNoteOffEvent(1, 0, 64)
            ),
            NotePeriod(
                71,
                1.0,
                2.0,
                MidiNoteOnEvent(1, 0, 71, 127),
                MidiNoteOffEvent(2, 0, 71)
            ),
        )

        assertEquals(
            listOf(
                NotePeriodGroup(listOf(np[0])),
                NotePeriodGroup(listOf(np[1]))
            ),
            listOf(*np).contiguousGroups()
        )
    }
}