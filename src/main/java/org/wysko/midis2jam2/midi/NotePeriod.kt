/*
 * Copyright (C) 2021 Jacob Wysko
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

import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument

/**
 * A note period is a tuple consisting of a [MidiNoteOnEvent] and a [MidiNoteOffEvent]. A note period is the
 * equivalent of the blocks you would see in a MIDI piano roll editor.
 */
open class NotePeriod(
    /** The MIDI pitch of this note period. */
    val midiNote: Int,

    /** The start time, expressed in seconds. */
    val startTime: Double,

    /** The end time, expressed in seconds. */
    var endTime: Double,

    /** The [MidiNoteOnEvent]. */
    val noteOn: MidiNoteOnEvent,

    /** The [MidiNoteOffEvent]. */
    val noteOff: MidiNoteOffEvent,
) {
    /** [FrettedInstrument] gets help from this. */
    var animationStarted: Boolean = false

    /** Returns the MIDI tick this note period starts. */
    fun startTick(): Long {
        return noteOn.time
    }

    /** Returns the MIDI tick this note period ends. */
    fun endTick(): Long {
        return noteOff.time
    }

    /** Returns the length of this note period, expressed in seconds. */
    fun duration(): Double {
        return endTime - startTime
    }

    override fun toString(): String {
        return "NotePeriod(midiNote=$midiNote, startTime=$startTime, endTime=$endTime, noteOn=$noteOn, " +
                "noteOff=$noteOff, animationStarted=$animationStarted)"
    }

    companion object {
        /**
         * A MIDI file is a sequence of [MidiNoteOnEvents][MidiNoteOnEvent] and [MidiNoteOffEvents][MidiNoteOffEvent].
         * Each pair of a NoteOn and NoteOff event can correspond to a [NotePeriod]. This method calculates those
         * pairs and returns them as a list of NotePeriods.
         *
         * @param instrument the [Instrument] that the [noteEvents] pertain to
         * @param noteEvents the note events to calculate into NotePeriods
         * @return the note events as a list of NotePeriods
         */
        @Contract(pure = true)
        fun calculateNotePeriods(
            instrument: Instrument,
            noteEvents: MutableList<MidiNoteEvent>
        ): MutableList<NotePeriod> {
            val notePeriods: ArrayList<NotePeriod> = ArrayList()
            val onEvents = arrayOfNulls<MidiNoteOnEvent>(MidiNoteEvent.MIDI_MAX_NOTE + 1)

            /* To calculate NotePeriods, we iterate over each MidiNoteEvent and keep track of when a NoteOnEvent occurs.
             * When it does, we insert it into the array at the index of the note's value. Then, when a NoteOffEvent
             * occurs, we look up the NoteOnEvent by the NoteOffEvent's value and create a NotePeriod from that.
             *
             * I wrote this with the assumption that there would not be duplicate notes of the same value that overlap,
             * so I'm not sure how it will handle in that scenario.
             *
             * Runs in O(n) time.
             */
            noteEvents.forEach { noteEvent ->
                if (noteEvent is MidiNoteOnEvent) {
                    onEvents[noteEvent.note] = noteEvent
                } else {
                    val noteOff = noteEvent as MidiNoteOffEvent
                    onEvents[noteOff.note]?.let {
                        notePeriods.add(
                            NotePeriod(
                                noteOff.note,
                                instrument.context.file.eventInSeconds(it.time),
                                instrument.context.file.eventInSeconds(noteOff.time),
                                it,
                                noteOff
                            )
                        )
                        onEvents[noteOff.note] = null
                    }
                }
            }

            /* Remove exact duplicates */
            return ArrayList(notePeriods.distinct())
        }
    }
}