/*
 * Copyright (C) 2023 Jacob Wysko
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
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/** An instrument that uses NoteOn and NoteOff events instead of NotePeriods. */
abstract class ToggledInstrument(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) : Instrument(context) {

    /** The event collector. */
    protected open val eventCollector: EventCollector<MidiNoteEvent> = EventCollector(
        events = events.filterIsInstance<MidiNoteEvent>(),
        context = context
    )

    private var currentPolyphony = 0
    private val noteIsPlaying = Array(128) { false }

    override fun tick(time: Double, delta: Float) {
        setVisibility(time)
        moveForMultiChannel(delta)
        eventCollector.advanceCollectAll(time).forEach {
            when (it) {
                is MidiNoteOnEvent -> noteStarted(it)
                is MidiNoteOffEvent -> noteEnded(it)
            }
        }
    }

    /** Signals when a note has begun. */
    open fun noteStarted(note: MidiNoteOnEvent) {
        if (!noteIsPlaying[note.note]) {
            currentPolyphony++
            noteIsPlaying[note.note] = true
        }
    }

    /** Signals when a note has ended. */
    open fun noteEnded(note: MidiNoteOffEvent) {
        if (noteIsPlaying[note.note]) {
            currentPolyphony--
            noteIsPlaying[note.note] = false
        }
    }

    override fun calcVisibility(time: Double, future: Boolean): Boolean {
        if (currentPolyphony > 0) return true

        // Visible if within one second of playing
        eventCollector.peek()?.let {
            if (context.file.eventInSeconds(it) - time <= 1.0) return true
        }

        // Visible if within a 7-second gap of two notes
        eventCollector.prev()?.let { prev ->
            eventCollector.peek()?.let { peek ->
                if (context.file.eventInSeconds(peek) - context.file.eventInSeconds(prev) <= 7.0) return true
            }
        }

        // Visible if 2 seconds after the last note period
        eventCollector.prev()?.let {
            if (time - context.file.eventInSeconds(it) <= 2.0) return true
        }

        // Invisible
        return false
    }

    override fun toString(): String {
        return super.toString() + buildString {
            append(debugProperty("currentPolyphony", currentPolyphony.toString()))
        }
    }
}
