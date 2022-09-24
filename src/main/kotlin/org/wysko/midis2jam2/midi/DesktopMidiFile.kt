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
import javax.sound.midi.ShortMessage

private const val TEMPO_MSG = 0x51
private const val TEXT_MSG = 0x01
private const val LYRIC_MSG = 0x05

/**
 * This object is used to separate the `javax.midi` package from the [MidiFile] class so that it can be used in Android.
 */
class DesktopMidiFile(file: File) : MidiFile(
    file.name,
    tracks = StandardMidiFileReader().getSequence(file).tracks.map {
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

                                ShortMessage.NOTE_ON -> {
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

                                ShortMessage.NOTE_OFF -> {
                                    events += MidiNoteOffEvent(time = tick, channel = msg.channel, note = msg.data1)
                                }

                                ShortMessage.PROGRAM_CHANGE -> {
                                    events += MidiProgramEvent(
                                        time = tick,
                                        channel = msg.channel,
                                        programNum = msg.data1
                                    )
                                }

                                ShortMessage.PITCH_BEND -> {
                                    events += MidiPitchBendEvent(
                                        time = tick,
                                        channel = msg.channel,
                                        value = msg.data1 + msg.data2 * 128
                                    )
                                }

                                ShortMessage.CONTROL_CHANGE -> {
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
    },
    division = StandardMidiFileReader().getSequence(file).resolution
)
