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

package org.wysko.midis2jam2.midi

import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.builder.smf
import org.wysko.kmidi.midi.event.NoteEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reading which MIDI port each event of a file is sent to.
 *
 * MuseScore writes files with more than 16 instruments by putting tracks on different MIDI ports, each with 16
 * channels of its own. If the port is lost, two instruments on the same channel number of different ports merge into
 * one, and play with one program.
 */
class MidiPortTest {

    @Test
    fun `a file without MIDI Port events is all on port 0`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format1
            division = tpq(TPQ)
            track { channel(0) { note(60, duration = 1.quarter) } }
            track { channel(1) { note(64, duration = 1.quarter) } }
        }

        assertEquals(1, file.portCount)
        assertEquals(setOf(0), file.portedEventsInPlayOrder().map { it.port }.toSet())
    }

    @Test
    fun `each track is on the port its MIDI Port event names, and the ports are numbered in order`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format1
            division = tpq(TPQ)
            track { tempo(120) }
            track {
                midiPort(3)
                channel(0) { note(60, duration = 1.quarter, absoluteTime = 0) }
            }
            track {
                midiPort(0)
                channel(0) { note(64, duration = 1.quarter, absoluteTime = 0) }
            }
        }

        assertEquals(2, file.portCount, "The file uses ports 0 and 3, which need two")
        val notes = file.portedEventsInPlayOrder().filter { it.event is NoteEvent.NoteOn }
            .associate { (it.event as NoteEvent.NoteOn).note.toInt() to it.port }
        assertEquals(mapOf(60 to 1, 64 to 0), notes, "Port 3 is the second port the file uses, and port 0 the first")
    }

    @Test
    fun `a MIDI Port event part way through a track moves the events after it`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                channel(0) { note(60, duration = 1.quarter, absoluteTime = 0) }
                midiPort(1, absoluteTime = 2 * TPQ)
                channel(0) { note(64, duration = 1.quarter, absoluteTime = 2 * TPQ) }
            }
        }

        val ports = file.portedEventsInPlayOrder().filter { it.event is NoteEvent.NoteOn }.map { it.port }
        assertEquals(listOf(0, 1), ports)
    }

    @Test
    fun `events keep the play order of the file`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format1
            division = tpq(TPQ)
            track {
                midiPort(1)
                channel(0) { note(60, duration = 1.quarter, absoluteTime = TPQ) }
            }
            track { channel(0) { note(64, duration = 1.quarter, absoluteTime = 0) } }
        }

        assertEquals(file.eventsInPlayOrder(), file.portedEventsInPlayOrder().map { it.event })
    }

    @Test
    fun `a file with more ports than can be played shares them out`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format1
            division = tpq(TPQ)
            repeat(MAX_PORTS + 1) { port ->
                track {
                    midiPort(port)
                    channel(0) { note(60, duration = 1.quarter) }
                }
            }
        }

        assertEquals(MAX_PORTS, file.portCount)
        assertEquals(0, file.portedEventsInPlayOrder().last { it.event is NoteEvent.NoteOn }.port)
    }

    @Test
    fun `a channel key numbers each port's channels after the last port's`() {
        assertEquals(9, channelKey(0, 9))
        assertEquals(25, channelKey(1, 9))
        assertEquals(1, portOf(25))
        assertEquals(9, channelOf(25))
    }

    private companion object {
        const val TPQ = 96
    }
}
