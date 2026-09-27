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

package org.wysko.midis2jam2.testing

import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.builder.smf

/**
 * MIDI files built in code, so fixtures are readable and reviewable instead of opaque binaries.
 */
object MidiFixtures {

    const val TICKS_PER_QUARTER = 96

    /** The General MIDI melodic programs. */
    val GENERAL_MIDI_PROGRAMS = 0..127

    /** The General MIDI percussion key range, as listed in the specification. */
    val GENERAL_MIDI_PERCUSSION_NOTES = 27..87

    /** Channel 10 in one-based terms, which the specification reserves for percussion. */
    const val PERCUSSION_CHANNEL = 9

    private val MELODIC_CHANNELS = (0..15).filter { it != PERCUSSION_CHANNEL }

    /** An empty sequence: no notes, no program changes. */
    fun empty(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track { tempo(120) }
    }.toTimeBasedSequence()

    /** A single melodic channel playing [noteCount] notes under one program. */
    fun singleProgram(program: Int, noteCount: Int = 4, note: Int = 60): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(program)
                repeat(noteCount) { note(note + it, duration = 1.quarter) }
            }
        }
    }.toTimeBasedSequence()

    /**
     * A file with no program change at all.
     *
     * The documentation promises a piano in this case.
     */
    fun noProgramChange(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                repeat(4) { note(60 + it, duration = 1.quarter) }
            }
        }
    }.toTimeBasedSequence()

    /**
     * Every General MIDI program, spread across the melodic channels.
     *
     * Each channel issues a run of program changes with notes after each, which is how a real
     * file switches instruments mid-performance. One sequence therefore exercises the whole
     * assignment table, and with it every instrument constructor and the assets it loads.
     */
    fun everyGeneralMidiProgram(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            GENERAL_MIDI_PROGRAMS.forEachIndexed { index, program ->
                val targetChannel = MELODIC_CHANNELS[index % MELODIC_CHANNELS.size]
                val slot = index / MELODIC_CHANNELS.size
                channel(targetChannel) {
                    program(program, absoluteTime = slot * 4 * TICKS_PER_QUARTER)
                    // Two notes, so instruments that animate chords see more than one at a time.
                    note(60, duration = 1.quarter, absoluteTime = slot * 4 * TICKS_PER_QUARTER)
                    note(64, duration = 1.quarter, absoluteTime = slot * 4 * TICKS_PER_QUARTER)
                    note(67, duration = 1.quarter, absoluteTime = slot * 4 * TICKS_PER_QUARTER + TICKS_PER_QUARTER)
                }
            }
        }
    }.toTimeBasedSequence()

    /** Every General MIDI percussion note, on the percussion channel. */
    fun everyPercussionNote(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(PERCUSSION_CHANNEL) {
                GENERAL_MIDI_PERCUSSION_NOTES.forEachIndexed { index, drum ->
                    note(drum, duration = 1.eighth, absoluteTime = index * TICKS_PER_QUARTER)
                }
            }
        }
    }.toTimeBasedSequence()

    /** Everything at once: all melodic programs and all percussion notes. */
    fun theWholeBand(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            GENERAL_MIDI_PROGRAMS.forEachIndexed { index, program ->
                val targetChannel = MELODIC_CHANNELS[index % MELODIC_CHANNELS.size]
                val slot = index / MELODIC_CHANNELS.size
                channel(targetChannel) {
                    program(program, absoluteTime = slot * 4 * TICKS_PER_QUARTER)
                    note(60, duration = 1.quarter, absoluteTime = slot * 4 * TICKS_PER_QUARTER)
                    note(64, duration = 1.quarter, absoluteTime = slot * 4 * TICKS_PER_QUARTER)
                    note(67, duration = 1.quarter, absoluteTime = slot * 4 * TICKS_PER_QUARTER + TICKS_PER_QUARTER)
                }
            }
            channel(PERCUSSION_CHANNEL) {
                GENERAL_MIDI_PERCUSSION_NOTES.forEachIndexed { index, drum ->
                    note(drum, duration = 1.eighth, absoluteTime = index * TICKS_PER_QUARTER)
                }
            }
        }
    }.toTimeBasedSequence()

    /**
     * A sung line, one syllable at a time, with an explicit line break.
     *
     * Syllable timing is what the lyric display animates against.
     */
    fun withLyrics(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(0)
                LYRIC_SYLLABLES.forEachIndexed { index, syllable ->
                    lyric(syllable, absoluteTime = index * TICKS_PER_QUARTER)
                    if (syllable != "\n") {
                        note(60, duration = 1.quarter, absoluteTime = index * TICKS_PER_QUARTER)
                    }
                }
            }
        }
    }.toTimeBasedSequence()

    /** The syllables [withLyrics] sings, in order. The newline separates the two lines. */
    val LYRIC_SYLLABLES: List<String> = listOf(
        "Twin", "kle ", "twin", "kle ", "lit", "tle ", "star", "\n",
        "How ", "I ", "won", "der ", "what ", "you ", "are",
    )
}
