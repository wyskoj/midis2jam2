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
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.builder.smf
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.midi.event.SysexEvent
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Trimming the silence from the start and end of a file.
 *
 * Trimming must not change how the song sounds once it starts. The setup a file does before its first note (resets,
 * programs, controllers, tempo) has to reach the synthesizer in the order the file sends it, or the song plays with the
 * wrong instruments, levels or speed.
 */
class SilenceTrimmingTest {

    @Test
    @Spec("playback.trim-silence.removes-leading-and-trailing-silence")
    fun `the first note starts the song and the notes after it keep their timing`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                tempo(120)
                note(60, duration = 1.quarter, absoluteTime = 4.quarter)
                note(64, duration = 1.quarter, deltaTime = 1.quarter)
            }
        }

        val trimmed = file.trimSilence().toTimeBasedSequence()

        assertEquals(Duration.ZERO, trimmed.firstNoteOnTime, "The first note should start the song")
        assertEquals(
            noteOnTimes(file).zipWithNext { a, b -> b - a },
            noteOnTimes(trimmed.smf).zipWithNext { a, b -> b - a },
            "The gaps between the notes should not change",
        )
    }

    @Test
    @Spec("playback.trim-silence.keeps-setup-messages")
    fun `the setup in the silence is sent before the first note, in the order the file sends it`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format1
            division = tpq(TPQ)
            track {
                sysex(GS_RESET, absoluteTime = 0)
                channel(0) { controller(VOLUME, 50, absoluteTime = 300) }
            }
            track {
                channel(0) {
                    controller(VOLUME, 100, absoluteTime = 100)
                    program(40, absoluteTime = 200)
                    note(60, duration = 1.quarter, absoluteTime = 4.quarter)
                }
            }
        }

        val played = file.trimSilence().eventsInPlayOrder()
        val beforeFirstNote = played.takeWhile { it !is NoteEvent.NoteOn }.filter { it !is MetaEvent }

        assertEquals(
            listOf("sysex", "cc 7=100", "program 40", "cc 7=50"),
            beforeFirstNote.map(::describe),
            "Every setup message should be sent before the first note, in the order the file sends them",
        )
        assertTrue(beforeFirstNote.all { it.tick == 0 }, "The setup should all be sent at once: $beforeFirstNote")
    }

    @Test
    fun `a tempo change in the silence is the tempo the song starts in`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                tempo(120)
                tempo(60, absoluteTime = 2.quarter)
                note(60, duration = 1.quarter, absoluteTime = 4.quarter)
                note(64, duration = 1.quarter)
            }
        }

        val trimmed = file.trimSilence().toTimeBasedSequence()

        assertEquals(listOf(Duration.ZERO, 1.seconds), noteOnTimes(trimmed.smf), "Notes a beat apart at 60 BPM")
        assertEquals(2.seconds, trimmed.duration, "Two beats at 60 BPM")
    }

    @Test
    @Spec("playback.trim-silence.removes-leading-and-trailing-silence")
    fun `anything after the last note is dropped`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                tempo(120)
                channel(0) {
                    note(60, duration = 1.quarter)
                    controller(VOLUME, 0, absoluteTime = 16.quarter)
                }
                lyric("end", absoluteTime = 12.quarter)
            }
        }

        val trimmed = file.trimSilence()

        assertEquals(0.5.seconds, trimmed.toTimeBasedSequence().duration, "The song should end with its last note")
        assertTrue(
            trimmed.tracks.flatMap { it.events }.none { it is ControlChangeEvent || it is MetaEvent.Lyric },
            "Events after the last note should be gone: ${trimmed.tracks}",
        )
    }

    @Test
    fun `a file with nothing to trim is left as it is`() {
        val tight = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                tempo(120)
                note(60, duration = 1.quarter)
            }
        }
        val silent = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                tempo(120)
                channel(0) { controller(VOLUME, 100, absoluteTime = 4.quarter) }
            }
        }

        assertSame(tight, tight.trimSilence())
        assertSame(silent, silent.trimSilence(), "A file without notes has no song to trim to")
    }

    @Test
    fun `track names stay with their own tracks`() {
        val built = smf {
            format = StandardMidiFile.Header.Format.Format1
            division = tpq(TPQ)
            track { tempo(120) }
            track { channel(0) { note(60, duration = 1.quarter, absoluteTime = 4.quarter) } }
        }
        val file = built.copy(
            tracks = listOf(
                built.tracks[0],
                StandardMidiFile.Track(listOf(MetaEvent.SequenceTrackName("Melody")) + built.tracks[1].events),
            )
        )

        val trimmed = file.trimSilence()

        assertNull(trimmed.tracks[0].name)
        assertEquals("Melody", trimmed.tracks[1].name)
    }

    @Test
    @Spec("playback.trim-silence.setting")
    fun `the setting decides whether a performance's song is trimmed`() {
        val file = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                tempo(120)
                note(60, duration = 1.quarter, absoluteTime = 4.quarter)
            }
        }
        val trimming = AppSettings().let { it.copy(playbackSettings = it.playbackSettings.copy(isTrimSilence = true)) }

        assertEquals(2.seconds, file.toPerformanceSequence(AppSettings()).firstNoteOnTime, "Off by default")
        assertEquals(Duration.ZERO, file.toPerformanceSequence(trimming).firstNoteOnTime)
    }

    private fun noteOnTimes(file: StandardMidiFile): List<Duration> {
        val sequence = file.toTimeBasedSequence()
        return file.eventsInPlayOrder().filterIsInstance<NoteEvent.NoteOn>().map { sequence.getTimeAtTick(it.tick) }
    }

    private fun describe(event: Event): String = when (event) {
        is SysexEvent -> "sysex"
        is ControlChangeEvent -> "cc ${event.controller}=${event.value}"
        is ProgramEvent -> "program ${event.program}"
        else -> event.toString()
    }

    private companion object {
        const val TPQ = 480
        const val VOLUME = 7
        val GS_RESET = byteArrayOf(0x41, 0x10, 0x42, 0x12, 0x40, 0x00, 0x7F, 0x00, 0x41)
    }
}
