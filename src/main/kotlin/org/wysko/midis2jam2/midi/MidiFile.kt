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

import java.io.File
import javax.sound.midi.MetaMessage
import javax.sound.midi.Sequence
import javax.sound.midi.ShortMessage
import javax.sound.midi.ShortMessage.*


private const val TEMPO_MSG = 0x51
private const val TEXT_MSG = 0x01
private const val LYRIC_MSG = 0x05

/**
 * Parse and stores information relating to a MIDI file.
 */
class MidiFile(file: File) {

    /**
     * The [Sequence] that makes up this MIDI file.
     */
    private val sequence: Sequence = StandardMidiFileReader().getSequence(file)

    /**
     * Specifies the meaning of delta-times. Specifically, it is the number of MIDI ticks that make up a quarter-note.
     */
    val division: Int = sequence.resolution

    /**
     * The tracks of the MIDI file. Each [MidiTrack] in this array corresponds to a track in the MIDI file.
     */
    val tracks: List<MidiTrack> = sequence.tracks.map {
        MidiTrack().apply {
            for (i in 0 until it.size()) { // For each event,
                with(it.get(i)) {
                    when (message) {

                        is MetaMessage -> {
                            val msg = message as MetaMessage
                            when (msg.type) {

                                TEMPO_MSG -> {
                                    events += MidiTempoEvent(
                                        time = tick,
                                        number = (msg.data ?: return@with).parseTempo()
                                    )
                                }
                                TEXT_MSG, LYRIC_MSG -> {
                                    events += MidiTextEvent(time = tick, text = String(msg.data))
                                }

                                else -> {}
                            }
                        }

                        is ShortMessage -> {
                            val msg = message as ShortMessage
                            when (msg.command) {

                                NOTE_ON -> {
                                    if (msg.data2 != 0) { // Note on with 0 velocity = note off
                                        events += MidiNoteOnEvent(
                                            time = tick,
                                            channel = msg.channel,
                                            note = msg.data1,
                                            velocity = msg.data2
                                        )
                                    } else {
                                        events += MidiNoteOffEvent(time = tick, channel = msg.channel, note = msg.data1)
                                    }
                                }

                                NOTE_OFF -> {
                                    events += MidiNoteOffEvent(time = tick, channel = msg.channel, note = msg.data1)
                                }

                                PROGRAM_CHANGE -> {
                                    events += MidiProgramEvent(
                                        time = tick,
                                        channel = msg.channel,
                                        programNum = msg.data1
                                    )
                                }

                                PITCH_BEND -> {
                                    events += MidiPitchBendEvent(
                                        time = tick,
                                        channel = msg.channel,
                                        value = msg.data1 + msg.data2 * 128
                                    )
                                }

                                CONTROL_CHANGE -> {
                                    events += MidiControlEvent(
                                        time = tick,
                                        channel = msg.channel,
                                        controlNum = msg.data1,
                                        value = msg.data2
                                    )
                                }

                            }
                        } // ShortMessage

                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * Contains a list of each tempo event in the MIDI file. The events from each track are combined, sorted by their
     * tick, and duplicates have been removed (if multiple events have the same tick, the last one is kept).
     */
    val tempos: List<MidiTempoEvent> =
        tracks.flatMap { it.events }.filterIsInstance<MidiTempoEvent>().ifEmpty { listOf(MidiTempoEvent(0, 500_000)) }
            .sortedBy { it.time }.asReversed().distinctBy { it.time }.asReversed()

    /** Maps MIDI events to their occurrence time in seconds. */
    private val eventTime: MutableMap<MidiEvent, Double> =
        tracks.flatMap { it.events }.associateWith { tickToSeconds(it.time) } as MutableMap<MidiEvent, Double>

    /** The name of the MIDI file. */
    val name: String = file.name

    /** The length of the MIDI file, expressed in seconds. */
    val length: Double = eventTime.maxOf { it.value }

    /**
     * If you need to create "pseudo" events that are helpful for animation, register them so that they have a time
     * associated with them.
     */
    fun registerEvents(events: Collection<MidiEvent>) {
        for (event in events) {
            eventTime[event] = tickToSeconds(event.time)
        }
    }

    /**
     * Given an [event], returns the time it occurs, in seconds.
     */
    fun eventInSeconds(event: MidiEvent): Double = eventTime[event]!!

    /**
     * Given a MIDI [tick], returns the time it occurs, in seconds.
     */
    fun tickToSeconds(tick: Long): Double {
        if (tempos.size == 1 || tick < 0) return tempos.first().spb() * tick.toBeats()

        /* Get all tempos that have started and finished before the current time. */
        return tempos.filter { it.time < tick }.foldIndexed(0.0) { index, acc, tempo ->
            acc + (if (index + 1 in tempos.indices) {
                tempos[index + 1].time.coerceAtMost(tick)
            } else {
                tick
            } - tempo.time).toBeats() * tempo.spb()
        }
    }

    /**
     * Returns the active tempo immediately before a given MIDI [tick].
     */
    private fun tempoBefore(tick: Long): MidiTempoEvent = if (tempos.size == 1 || tick == 0L) {
        tempos[0]
    } else {
        tempos.last { it.time < tick }
    }

    /**
     * Returns the active tempo immediately before a given MIDI [event].
     */
    fun tempoBefore(event: MidiEvent): MidiTempoEvent = tempoBefore(event.time)


    /**
     * Returns the active tempo at the given [time] in seconds, expressed in MIDI format.
     */
    fun tempoAt(time: Double): MidiTempoEvent = when {
        tempos.size == 1 || time < 0 -> tempos.first()
        else -> tempos.last { eventTime[it]!! <= time }
    }

    /** Converts a value expressed in MIDI ticks to its beat number. */
    private fun Long.toBeats(): Double = this / division.toDouble()
}

/**
 * Converts the byte data of a tempo event in a MIDI file to its corresponding integer.
 */
private fun ByteArray.parseTempo(): Int = with(this.map { it.toInt() }) {
    this[0] and 0xff shl 16 or (this[1] and 0xff shl 8) or (this[2] and 0xff)
}
