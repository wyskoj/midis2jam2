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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.NotePeriod


object NoteQueue {

    /**
     * Given a list of [MidiEvents][MidiEvent], removes the events that are needing animation. This is any event that
     * has a time equal to or less than the current time. This method assumes [events] is sorted by time. It then
     * returns all the removed events.
     */
    @JvmStatic
    fun <T : MidiEvent> collect(events: MutableList<T>, context: Midis2jam2, time: Double): List<T> {
        val queue = events.takeWhile { context.file.eventInSeconds(it) <= time }
        events.removeAll(queue)
        return queue
    }

    /**
     * Given a list of [MidiEvents][MidiEvent], removes the events that are needing animation. This is any event
     * that has a time equal to or less than the current time.
     *
     * This method assumes [events] is sorted by event time.
     *
     * @param events the events to pull from
     * @param time   the current time, in seconds
     * @return the last hit to play
     */
    @JvmStatic
    fun <T : MidiEvent> collectOne(events: MutableList<T>, context: Midis2jam2, time: Double): T? {
        val first: T? = events.firstOrNull { context.file.eventInSeconds(it) <= time }
        return if (first != null) {
            events.remove(first)
            first
        } else {
            null
        }
    }

    fun collectOne(events: MutableList<NotePeriod>, time: Double): NotePeriod? {
        val first = events.takeWhile { it.startTime <= time }.lastOrNull()
        return if (first == null) {
            null
        } else {
            events.remove(first)
            first
        }
    }

    /**
     * Given a list of [MidiEvents][MidiEvent], removes the events that are needing animation. This is any event
     * that has a time equal to or less than the current time.
     *
     * For events that are [MidiNoteOffEvents][MidiNoteOffEvent], they will be removed 1/30th of a second early so
     * that repeated notes on some instruments can be differentiated.
     *
     * This method assumes that [events] is sorted by event time.
     *
     * @param events the events to pull from
     * @param time   the current time, in seconds
     * @return a list of events that occur now or before
     */
    @JvmStatic
    fun <T : MidiEvent> collectWithOffGap(events: MutableList<T>, context: Midis2jam2, time: Double): List<T> {
        val midi = context.file
        val queue = events.takeWhile {
            midi.eventInSeconds(it) <= time || (MidiNoteOffEvent::class.isInstance(it) &&
                    midi.eventInSeconds(it) <= time - 0.033F)
        }
        events.removeAll(queue)
        return queue
    }
}