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
package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.NotePeriodCollector
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.NotePeriod.Companion.calculateNotePeriods

/**
 * Any instrument that also depends on knowing the [MidiNoteOffEvent] for proper animation. Examples include:
 * saxophone, piano, guitar, telephone ring.
 */
abstract class SustainedInstrument protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>
) : Instrument(context) {

    /** The list of note periods. */
    protected val notePeriods: MutableList<NotePeriod> = calculateNotePeriods(
        context = context,
        noteEvents = eventList.filterIsInstance<MidiNoteEvent>()
    )

    /** The list of current note periods. Will always be updating as the MIDI file progresses. */
    protected var currentNotePeriods: Set<NotePeriod> = setOf()

    /** The note period collector. */
    protected open val notePeriodCollector: NotePeriodCollector = NotePeriodCollector(
        notePeriods = notePeriods,
        context = context
    )

    /**
     * Determines the current note periods.
     */
    protected open fun calculateCurrentNotePeriods(time: Double) {
        currentNotePeriods = notePeriodCollector.advance(time)
    }

    override fun tick(time: Double, delta: Float) {
        calculateCurrentNotePeriods(time)
        setVisibility(time)
        moveForMultiChannel(delta)
    }

    /** Calculates the current visibility by the current [time]. True if the instrument is visible, false otherwise.
     * This method can be used for time values in the future. */
    override fun calcVisibility(time: Double, future: Boolean): Boolean {
        // Visible if currently playing
        if (currentNotePeriods.isNotEmpty()) return true

        // Visible if within one second of playing
        notePeriodCollector.peek()?.let {
            if (it.startTime - time <= 1.0) return true
        }

        // Visible if within a 7-second gap of two notes
        notePeriodCollector.prev()?.let { prev ->
            notePeriodCollector.peek()?.let { peek ->
                if (peek.startTime - prev.endTime <= 7.0) return true
            }
        }

        // Visible if 2 seconds after the last note period
        notePeriodCollector.prev()?.let {
            if (time - it.endTime <= 2.0) return true
        }

        // Invisible
        return false
    }
}
