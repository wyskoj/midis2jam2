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
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/**
 * A DecayedInstrument is any instrument that only depends on [MidiNoteOnEvent]s to function. The note off event
 * is discarded.
 */
abstract class DecayedInstrument protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>,
) : Instrument(context) {

    /**
     * List of events this instrument should play.
     */
    @JvmField
    val hits: MutableList<MidiNoteOnEvent> = eventList.filterIsInstance<MidiNoteOnEvent>().toMutableList()

    /**
     * The list of unmodifiable hits.
     */
    private val finalHits: List<MidiNoteOnEvent> = hits

    override fun tick(time: Double, delta: Float) {
        setIdleVisibilityByStrikes(finalHits, time, instrumentNode)
        moveForMultiChannel(delta)
    }


}