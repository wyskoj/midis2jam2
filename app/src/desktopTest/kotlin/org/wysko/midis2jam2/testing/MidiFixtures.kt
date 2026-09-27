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

    /** One channel playing four notes under [first], then switching to [second] for four more. */
    fun programSwitch(first: Int, second: Int): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(first, absoluteTime = 0)
                repeat(4) { note(60 + it, duration = 1.quarter, absoluteTime = it * TICKS_PER_QUARTER) }
                program(second, absoluteTime = 4 * TICKS_PER_QUARTER)
                repeat(4) { note(60 + it, duration = 1.quarter, absoluteTime = (4 + it) * TICKS_PER_QUARTER) }
            }
        }
    }.toTimeBasedSequence()

    /** The note [noteHeldAcrossProgramChange] holds across the program change. */
    const val HELD_NOTE = 60

    /**
     * One channel that starts [HELD_NOTE] under [first], changes to [second] while the note is still held, and then
     * releases it. The channel also plays a note of its own under [second], so both programs have an instrument.
     */
    fun noteHeldAcrossProgramChange(first: Int, second: Int): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(first, absoluteTime = 0)
                note(HELD_NOTE, duration = 2.quarter, absoluteTime = 0)
                program(second, absoluteTime = TICKS_PER_QUARTER)
                note(HELD_NOTE + 7, duration = 1.quarter, absoluteTime = TICKS_PER_QUARTER)
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

    /** Channel 11 in one-based terms: the channel `take5.mid` turns into a second rhythm channel. */
    const val SECOND_RHYTHM_CHANNEL = 10

    /** What channel 10 plays in [secondRhythmChannel]: a bass drum. */
    const val PRIMARY_RHYTHM_NOTE = 36

    /** What the second rhythm channel plays in [secondRhythmChannel]: a snare. */
    const val SECOND_RHYTHM_NOTE = 40

    /**
     * Two rhythm channels at once, the way GS files like `take5.mid` do it: a GS reset, then the "use for rhythm
     * part" message turning channel 11 into a rhythm part, which then picks its kit by program number.
     *
     * Channel 10 plays [beats] of [PRIMARY_RHYTHM_NOTE] on [primaryKit], or nothing if it's `null`. Channel 11 plays
     * [beats] of [SECOND_RHYTHM_NOTE] on [secondKit]. If [melodicFrom] is given, channel 11 is turned back into a
     * melodic part at that beat, and plays a few notes under the same program.
     */
    fun secondRhythmChannel(
        primaryKit: Int?,
        secondKit: Int,
        beats: Int = 8,
        melodicFrom: Int? = null,
        asRhythmPart: Boolean = true,
    ): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            sysex(GS_RESET, absoluteTime = 0)
            if (asRhythmPart) sysex(gsUseForRhythmPart(SECOND_RHYTHM_CHANNEL, rhythm = true), absoluteTime = 0)
            if (primaryKit != null) {
                channel(PERCUSSION_CHANNEL) {
                    program(primaryKit, absoluteTime = 0)
                    repeat(beats) {
                        note(PRIMARY_RHYTHM_NOTE, duration = 1.eighth, absoluteTime = it * TICKS_PER_QUARTER)
                    }
                }
            }
            channel(SECOND_RHYTHM_CHANNEL) {
                program(secondKit, absoluteTime = 0)
                repeat(beats) {
                    note(SECOND_RHYTHM_NOTE, duration = 1.eighth, absoluteTime = it * TICKS_PER_QUARTER)
                }
            }
            if (melodicFrom != null) {
                val start = melodicFrom * TICKS_PER_QUARTER
                sysex(gsUseForRhythmPart(SECOND_RHYTHM_CHANNEL, rhythm = false), absoluteTime = start)
                channel(SECOND_RHYTHM_CHANNEL) {
                    repeat(4) { note(40 + it, duration = 1.quarter, absoluteTime = start + it * TICKS_PER_QUARTER) }
                }
            }
        }
    }.toTimeBasedSequence()

    /** The GS reset message, as it appears in a file (without the leading F0). */
    val GS_RESET: ByteArray = bytes(0x41, 0x10, 0x42, 0x12, 0x40, 0x00, 0x7F, 0x00, 0x41, 0xF7)

    /** The General MIDI system on message, as it appears in a file. */
    val GM_SYSTEM_ON: ByteArray = bytes(0x7E, 0x7F, 0x09, 0x01, 0xF7)

    /** The General MIDI 2 system on message, as it appears in a file. */
    val GM2_SYSTEM_ON: ByteArray = bytes(0x7E, 0x7F, 0x09, 0x03, 0xF7)

    /** The XG system on message, as it appears in a file. */
    val XG_SYSTEM_ON: ByteArray = bytes(0x43, 0x10, 0x4C, 0x00, 0x00, 0x7E, 0x00, 0xF7)

    /** A voice chosen by bank select and a zero-based program change. */
    data class Patch(val msb: Int, val lsb: Int, val program: Int)

    /**
     * One channel that selects each of [patches] in turn, with bank select then a program change, and plays four
     * notes on each. The file starts with [reset], if there is one.
     *
     * The notes start at [firstNote], so the same fixture works for rhythm channels.
     */
    fun bankSelect(
        reset: ByteArray?,
        vararg patches: Patch,
        channel: Int = 0,
        firstNote: Int = 60,
    ): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            if (reset != null) sysex(reset, absoluteTime = 0)
            channel(channel) {
                patches.forEachIndexed { index, patch ->
                    val start = index * 4 * TICKS_PER_QUARTER
                    controller(0, patch.msb, absoluteTime = start)
                    controller(32, patch.lsb, absoluteTime = start)
                    program(patch.program, absoluteTime = start)
                    repeat(4) {
                        note(firstNote + it, duration = 1.eighth, absoluteTime = start + it * TICKS_PER_QUARTER)
                    }
                }
            }
        }
    }.toTimeBasedSequence()

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

    /** The GS "use for rhythm part" message for the zero-based [channel], as it appears in a file. */
    private fun gsUseForRhythmPart(channel: Int, rhythm: Boolean): ByteArray {
        // GS numbers its parts 10, 1-9, 11-16.
        val part = when (channel) {
            PERCUSSION_CHANNEL -> 0
            in 0..8 -> channel + 1
            else -> channel
        }
        val address = listOf(0x40, 0x10 or part, 0x15)
        val value = if (rhythm) 2 else 0
        val checksum = (128 - (address.sum() + value) % 128) % 128
        return (listOf(0x41, 0x10, 0x42, 0x12) + address + listOf(value, checksum, 0xF7))
            .map { it.toByte() }
            .toByteArray()
    }

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
