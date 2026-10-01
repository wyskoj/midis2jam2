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
import org.wysko.kmidi.midi.builder.TrackBuilder
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
     * A piano playing one note, [restQuarters] quarter notes into the file (at 120 BPM, half a second each), so the
     * sound starts at a known time.
     */
    fun oneNoteAfterRest(restQuarters: Int = 2): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(0)
                note(60, duration = 1.quarter, absoluteTime = restQuarters * TICKS_PER_QUARTER)
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

    /**
     * A sung line whose syllables use characters outside the bundled Inter font's plain-ASCII
     * character set (Japanese and accented Latin), so tests can exercise the dynamic glyph atlas.
     */
    fun withNonAsciiLyrics(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(0)
                NON_ASCII_LYRIC_SYLLABLES.forEachIndexed { index, syllable ->
                    lyric(syllable, absoluteTime = index * TICKS_PER_QUARTER)
                    if (syllable != "\n") {
                        note(60, duration = 1.quarter, absoluteTime = index * TICKS_PER_QUARTER)
                    }
                }
            }
        }
    }.toTimeBasedSequence()

    /** The syllables [withNonAsciiLyrics] sings, in order. */
    val NON_ASCII_LYRIC_SYLLABLES: List<String> = listOf(
        "こん", "にち", "は", "\n",
        "café ", "naïve ", "résumé",
    )

    /** Steel-string acoustic guitar (zero-based General MIDI program). */
    const val STEEL_GUITAR_PROGRAM = 25

    /** Overdriven guitar (zero-based General MIDI program). */
    const val OVERDRIVEN_GUITAR_PROGRAM = 29

    /** Distortion guitar (zero-based General MIDI program). */
    const val DISTORTION_GUITAR_PROGRAM = 30

    /** Fingered electric bass (zero-based General MIDI program). */
    const val FINGERED_BASS_PROGRAM = 33

    /** How many ticks the chords of [humanizedGuitarChords] ring into the next chord. */
    const val HUMANIZED_LATE_OFF = 4

    /**
     * Open E, A, D and G chords on an acoustic guitar, played the way recorded MIDI plays them: each strummed
     * (a couple of ticks between strings) and ringing a few ticks into the next chord. The old fretting engine
     * merged such chords into one.
     */
    fun humanizedGuitarChords(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(STEEL_GUITAR_PROGRAM, absoluteTime = 0)
                chords(OPEN_CHORDS + OPEN_CHORDS, duration = 2 * TICKS_PER_QUARTER, spread = 2, lateOff = HUMANIZED_LATE_OFF)
            }
        }
    }.toTimeBasedSequence()

    /** The chords [humanizedGuitarChords] plays, twice over. */
    val OPEN_CHORDS: List<List<Int>> = listOf(
        listOf(40, 47, 52, 56, 59, 64),
        listOf(45, 52, 57, 61, 64),
        listOf(50, 57, 62, 66),
        listOf(43, 47, 50, 55, 59, 67),
    )

    /**
     * A power-chord riff on the low D string, which only drop-D tuning can play. With [delayed], it starts
     * [TUNED_PART_START_SECONDS] in, so the guitar's retune before it falls inside the song.
     */
    fun dropDRiff(delayed: Boolean = false): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(DISTORTION_GUITAR_PROGRAM, absoluteTime = 0)
                val riff = listOf(listOf(38, 45, 50), listOf(38, 45, 50), listOf(41, 48, 53), listOf(43, 50, 55))
                chords(
                    List(4) { riff }.flatten(),
                    duration = TICKS_PER_QUARTER / 2,
                    offset = if (delayed) TUNED_PART_START_TICKS else 0,
                )
            }
        }
    }.toTimeBasedSequence()

    /** The note of [guitarSoloWithBends] that is bent up a whole step. */
    const val BENT_NOTE_INDEX = 3

    /** An A-minor pentatonic lick on an overdriven guitar, with one note bent up a whole step. */
    fun guitarSoloWithBends(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(OVERDRIVEN_GUITAR_PROGRAM, absoluteTime = 0)
                val eighth = TICKS_PER_QUARTER / 2
                SOLO_LICK.forEachIndexed { i, pitch ->
                    note(pitch, duration = eighth, absoluteTime = i * eighth)
                }
                // A whole-step bend (the full default two-semitone range) during the bent note, then back.
                pitch(FULL_BEND, absoluteTime = BENT_NOTE_INDEX * eighth + eighth / 4)
                pitch(0.0, absoluteTime = (BENT_NOTE_INDEX + 1) * eighth)
            }
        }
    }.toTimeBasedSequence()

    /**
     * Open-chord shapes (G, C, D, Em) strummed on a steel-string acoustic two frets up, which the engine plays with a
     * capo on the second fret. It starts [TUNED_PART_START_SECONDS] in.
     */
    fun capoChords(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(STEEL_GUITAR_PROGRAM, absoluteTime = 0)
                val shapes = listOf(
                    listOf(43, 47, 50, 55, 59, 67),
                    listOf(48, 52, 55, 60, 64),
                    listOf(50, 57, 62, 66),
                    listOf(40, 47, 52, 55, 59, 64),
                )
                val song = List(6) { shapes }.flatten().map { chord -> chord.map { it + 2 } }
                chords(song, duration = 2 * TICKS_PER_QUARTER, spread = 2, offset = TUNED_PART_START_TICKS)
            }
        }
    }.toTimeBasedSequence()

    /** When a delayed [dropDRiff] and [capoChords] start playing, in seconds. */
    const val TUNED_PART_START_SECONDS = 2.0
    private const val TUNED_PART_START_TICKS = 4 * TICKS_PER_QUARTER

    /** The notes of [guitarSoloWithBends]. */
    val SOLO_LICK: List<Int> = listOf(69, 72, 74, 74, 72, 69, 67, 69, 72, 74, 76, 74, 72, 69, 67, 64)

    /** Just under the largest pitch-wheel value, which a whole-step bend uses at the default range. */
    private const val FULL_BEND = 0.9999

    /** When [rhythmThenSolo] switches from chords to the solo, in seconds. */
    const val SOLO_START_SECONDS = 4.0

    /** Two bars of power chords and then a two-bar single-note solo, on one distortion guitar. */
    fun rhythmThenSolo(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(DISTORTION_GUITAR_PROGRAM, absoluteTime = 0)
                val riff = listOf(listOf(40, 47, 52), listOf(43, 50, 55), listOf(45, 52, 57), listOf(43, 50, 55))
                chords(riff + riff, duration = TICKS_PER_QUARTER)
                val soloStart = 8 * TICKS_PER_QUARTER
                val sixteenth = TICKS_PER_QUARTER / 4
                (SOLO_LICK + SOLO_LICK).forEachIndexed { i, pitch ->
                    note(pitch, duration = sixteenth, absoluteTime = soloStart + i * sixteenth)
                }
            }
        }
    }.toTimeBasedSequence()

    /** A rock bass line in eighth notes, with octave jumps. */
    fun bassLine(): TimeBasedSequence = smf {
        format = StandardMidiFile.Header.Format.Format0
        division = tpq(TICKS_PER_QUARTER)
        track {
            tempo(120)
            channel(0) {
                program(FINGERED_BASS_PROGRAM, absoluteTime = 0)
                val eighth = TICKS_PER_QUARTER / 2
                val line = listOf(28, 28, 40, 28, 31, 33, 35, 36, 33, 33, 45, 33, 31, 31, 43, 31)
                List(2) { line }.flatten().forEachIndexed { i, pitch ->
                    note(pitch, duration = eighth, absoluteTime = i * eighth)
                }
            }
        }
    }.toTimeBasedSequence()

    /**
     * Plays [chords] one after another, each lasting [duration] ticks, with [spread] ticks between the strings
     * of a strum. Each note rings [lateOff] ticks into the next chord, unless the next chord plays the same pitch
     * (MIDI can't sound one pitch twice at once on a channel).
     */
    private fun TrackBuilder.chords(
        chords: List<List<Int>>,
        duration: Int,
        spread: Int = 0,
        lateOff: Int = 0,
        offset: Int = 0,
    ) {
        chords.forEachIndexed { i, chord ->
            val next = chords.getOrNull(i + 1).orEmpty()
            chord.forEachIndexed { k, pitch ->
                val start = offset + i * duration + k * spread
                val end = offset + (i + 1) * duration + if (pitch in next) -1 else lateOff
                note(pitch, duration = end - start, absoluteTime = start)
            }
        }
    }
}
