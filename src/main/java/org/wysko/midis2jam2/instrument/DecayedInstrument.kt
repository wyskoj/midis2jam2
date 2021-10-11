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
package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/** Any instrument that only depends on [MidiNoteOnEvent]s to function. The [MidiNoteOffEvent] is discarded. */
abstract class DecayedInstrument protected constructor(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    Instrument(context) {

    /** List of events this instrument should play. This is mutable by lower classes. */
    protected open val hits: MutableList<MidiNoteOnEvent> =
        eventList.filterIsInstance<MidiNoteOnEvent>().toMutableList()

    /** Initialized to the same vales of [hits], but used for visibility calculations. */
    protected val hitsV: MutableList<MidiNoteOnEvent> =
        eventList.filterIsInstance<MidiNoteOnEvent>().toMutableList()

    /** The last note that this instrument has played, used for visibility calculations. */
    protected var lastHit: MidiNoteOnEvent? = null

    override fun calcVisibility(time: Double): Boolean {
        /* Within one second of a hit? Visible. */
        if (hitsV.isNotEmpty() && context.file.eventInSeconds(hitsV[0]) - time <= 1) return true

        /* If within a 7-second gap between the last hit and the next? Visible. */
        if (lastHit != null
            && hitsV.isNotEmpty()
            && context.file.eventInSeconds(hitsV[0]) - context.file.eventInSeconds(lastHit) <= 7
        ) return true

        /* If after 2 seconds of the last hit? Visible. */
        if (lastHit != null && time - context.file.eventInSeconds(lastHit) <= 2) return true

        /* Invisible. */
        return false
    }

    override fun tick(time: Double, delta: Float) {
        setVisibility(time)
        /* Simulate hit truncation */
        NoteQueue.collectOne(hitsV, context, time)?.let {
            lastHit = it
        }
        moveForMultiChannel(delta)
    }
}