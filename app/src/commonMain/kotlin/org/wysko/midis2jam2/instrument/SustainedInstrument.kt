/*
 * Copyright (C) 2025 Jacob Wysko
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

import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcCollector
import org.wysko.midis2jam2.instrument.algorithmic.Visibility
import kotlin.time.Duration

/**
 * An instrument that uses both NoteOn and NoteOff events to play notes.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 */
abstract class SustainedInstrument(context: Midis2jam2, events: List<MidiEvent>) : Instrument(context) {

    /**
     * The list of all note periods that this instrument should play.
     */
    protected val timedArcs: MutableList<TimedArc> =
        TimedArc.fromNoteEvents(context.sequence, events.filterIsInstance<NoteEvent>()).toMutableList()

    /**
     * The collector that manages the note periods.
     */
    protected open val collector: TimedArcCollector =
        TimedArcCollector(context = context, arcs = timedArcs)

    override fun tick(time: Duration, delta: Duration) {
        collector.advance(time)
        isVisible = calculateVisibility(time)
        adjustForMultipleInstances(delta)
    }

    override fun calculateVisibility(time: Duration, future: Boolean): Boolean =
        Visibility.standardRules(collector, time).also {
            if (!isVisible && it) onEntry()
            if (isVisible && !it) onExit()
        }
}
