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

package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/** An instrument that uses keys to play notes. */
abstract class KeyedInstrument(
    context: Midis2jam2,
    eventList: MutableList<MidiChannelSpecificEvent>,
    /** The lowest note that this instrument can play. */
    val rangeLow: Int,
    /** The highest note that this instrument can play. */
    val rangeHigh: Int
) : SustainedInstrument(context, eventList) {

    /**
     * The events associated with this instrument.
     */
    protected val events: MutableList<MidiNoteEvent> =
        eventList.filterIsInstance<MidiNoteEvent>() as MutableList<MidiNoteEvent>

    /** The keys of this instrument. */
    protected lateinit var keys: Array<Key>

    /** Returns the number of keys on this instrument. */
    fun keyCount(): Int = rangeHigh - rangeLow + 1

    /** Returns the key associated with the [midiNote], or `null` if this instrument can't animate that note. */
    protected abstract fun keyByMidiNote(midiNote: Int): Key?

    /**
     * Sets idle visibility by note on and off events.
     *
     * @param time the current time
     */
    protected open fun setIdleVisibilityByNoteOnAndOff(time: Double) {
        val b = calcVisibility(time)
        isVisible = b
        instrumentNode.cullHint = if (b) Spatial.CullHint.Dynamic else Spatial.CullHint.Always
    }

    override fun tick(time: Double, delta: Float) {
        calculateCurrentNotePeriods(time)
        setIdleVisibilityByNoteOnAndOff(time)
        moveForMultiChannel(delta)
        val eventsToPerform: List<MidiNoteEvent> = getElapsedEvents(time)
        for (event in eventsToPerform) {
            val key = keyByMidiNote(event.note)
            if (event is MidiNoteOnEvent) {
                key?.isBeingPressed = true
            } else if (event is MidiNoteOffEvent) {
                /* If there is a note off event and a note on event in this frame for the same note, you won't see it
                 * because the key will be turned off before the frame renders. So, move the note off event back to the
                 * list of event to be rendered on the next frame. */
                if (eventsToPerform.stream()
                        .anyMatch { e: MidiNoteEvent -> e.note == event.note && e is MidiNoteOnEvent }
                ) {
                    /* bonk. you get to go to the next frame */
                    events.add(0, event)
                } else {
                    key?.isBeingPressed = false
                }
            }
        }
        keys.forEach { it.tick(delta) }
    }

    /**
     * Searches [events] for those that should be animated now, taking special keyboard considerations into
     * place.
     *
     * @param time the current time, in seconds
     * @return the list of events that need animation
     */
    private fun getElapsedEvents(time: Double): List<MidiNoteEvent> {
        val eventsToPerform: MutableList<MidiNoteEvent> = ArrayList()
        if (events.isNotEmpty()) {
            if (events[0] !is MidiNoteOnEvent && events[0] !is MidiNoteOffEvent) {
                events.removeAt(0)
            }
            while (events.isNotEmpty() && (events[0] is MidiNoteOnEvent && context.file.eventInSeconds(events[0]) <= time ||
                        events[0] is MidiNoteOffEvent && context.file.eventInSeconds(events[0]) - time <= 0.05)
            ) {
                eventsToPerform.add(events.removeAt(0))
            }
        }
        return eventsToPerform
    }


    companion object {
        /**
         * Calculates if a MIDI note value is a black or white key on a standard piano.
         *
         * @param x the MIDI note value
         * @return {@link KeyColor#WHITE} or {@link KeyColor#BLACK}
         */
        fun midiValueToColor(x: Int): KeyColor = when (x % 12) {
            1, 3, 6, 8, 10 -> KeyColor.BLACK
            else -> KeyColor.WHITE
        }
    }


    /** Keyboards have two different colored keys: white and black. */
    enum class KeyColor {
        /** White key color. */
        WHITE,

        /** Black key color. */
        BLACK
    }
}