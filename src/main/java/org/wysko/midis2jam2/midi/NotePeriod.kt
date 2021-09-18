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

/**
 * A note period is a tuple consisting of a [MidiNoteOnEvent] and a [MidiNoteOffEvent]. A note period is the
 * equivalent of the blocks you would see in a MIDI piano roll editor.
 */
open class NotePeriod(
    /**
     * The MIDI pitch of this note period.
     */
    val midiNote: Int,

    /**
     * The start time, expressed in seconds.
     */
    val startTime: Double,

    /**
     * The end time, expressed in seconds.
     */
    var endTime: Double,

    /**
     * The [MidiNoteOnEvent].
     */
    val noteOn: MidiNoteOnEvent,

    /**
     * The [MidiNoteOffEvent].
     */
    val noteOff: MidiNoteOffEvent,

    ) {
    /**
     * [FrettedInstrument] gets help from this.
     */
    var animationStarted = false

    /**
     * Returns the MIDI tick this note period starts.
     */
    fun startTick(): Long {
        return noteOn.time
    }

    /**
     * Returns the MIDI tick this note period ends.
     */
    fun endTick(): Long {
        return noteOff.time
    }

    /**
     * Returns the length of this note period, expressed in seconds.
     */
    fun duration(): Double {
        return endTime - startTime
    }

    /**
     * Determines whether this note period would be playing at a given [time], in seconds. If it is playing, returns
     * true, false otherwise. More specifically, it checks if the [time] is within the range of the [startTime] and
     * the [endTime].
     */
    fun isPlayingAt(time: Double): Boolean {
        return time in startTime..endTime
    }

    companion object {
        /**
         * A MIDI file is a sequence of [MidiNoteOnEvent]s and [MidiNoteOffEvent]s. This method searches the
         * files and connects corresponding events together. This is effectively calculating the "blocks" you would see in a
         * piano roll editor.
         *
         * @param noteEvents the note events to calculate into [NotePeriod]s
         */
        @JvmStatic
        @Contract(pure = true)
        fun calculateNotePeriods(instrument: Instrument, noteEvents: MutableList<MidiNoteEvent>): List<NotePeriod> {
            val notePeriods: ArrayList<NotePeriod> = ArrayList()
            val onEvents = arrayOfNulls<MidiNoteOnEvent>(MidiNoteEvent.MIDI_MAX_NOTE + 1)

            /* To calculate NotePeriods, we iterate over each MidiNoteEvent and keep track of when a NoteOnEvent occurs.
             * When it does, we insert it into the array at the index of the note's value. Then, when a NoteOffEvent occurs,
             * we lookup the NoteOnEvent by the NoteOffEvent's value and create a NotePeriod from that.
             *
             * I wrote this with the assumption that there would not be duplicate notes of the same value that overlap,
             * so I'm not sure how it will handle in that scenario.
             *
             * Runs in O(n) time.
             */
            for (noteEvent in noteEvents) {
                if (noteEvent is MidiNoteOnEvent) {
                    onEvents[noteEvent.note] = noteEvent
                } else {
                    val noteOff = noteEvent as MidiNoteOffEvent
                    if (onEvents[noteOff.note] != null) {
                        val onEvent = onEvents[noteOff.note]
                        notePeriods.add(NotePeriod(noteOff.note,
                            instrument.context.file.eventInSeconds(onEvent!!.time),
                            instrument.context.file.eventInSeconds(noteOff.time),
                            onEvent,
                            noteOff))
                        onEvents[noteOff.note] = null
                    }
                }
            }

            /* Remove exact duplicates */
            return ArrayList(notePeriods.distinct())
        }
    }

    override fun toString(): String {
        return "NotePeriod{" +
                "midiNote=" + midiNote +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}'
    }

    fun copy(
        midiNote: Int = this.midiNote,
        startTime: Double = this.startTime,
        endTime: Double = this.endTime,
        noteOn: MidiNoteOnEvent = this.noteOn,
        noteOff: MidiNoteOffEvent = this.noteOff
    ) = NotePeriod(midiNote, startTime, endTime, noteOn, noteOff)

}