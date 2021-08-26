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

import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument

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
    val endTime: Double,

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

    override fun toString(): String {
        return "NotePeriod{" +
                "midiNote=" + midiNote +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}'
    }
}