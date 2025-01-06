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

import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.Visibility
import kotlin.time.Duration

/**
 * An instrument that only uses NoteOn events to play notes. The NoteOff events are ignored.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 */
abstract class DecayedInstrument protected constructor(
    context: Midis2jam2,
    private val events: List<MidiEvent>
) : Instrument(context) {

    /**
     * A filter of [events] that only contains [NoteEvent.NoteOn]s.
     */
    val hits: MutableList<NoteEvent.NoteOn> = events.filterIsInstance<NoteEvent.NoteOn>().toMutableList()

    /**
     * An [EventCollector] used to collect events for visibility calculations.
     */
    open val collectorForVisibility: EventCollector<NoteEvent.NoteOn> =
        EventCollector(context, events.filterIsInstance<NoteEvent.NoteOn>())

    override fun calculateVisibility(time: Duration, future: Boolean): Boolean = with(collectorForVisibility) {
        advanceCollectOne(time)
        Visibility.standardRules(context, this, time).also {
            if (!isVisible && it) onEntry()
            if (isVisible && !it) onExit()
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        isVisible = calculateVisibility(time, false)
        adjustForMultipleInstances(delta)
    }
}
