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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.NotePeriod


/**
 * Provides various methods for manipulating the note queue.
 */
object NoteQueue {

    /**
     * Given a list of [MidiEvent]s, removes the events that are needing animation. This is any event that
     * has a time equal to or less than the current time. This method assumes [events] is sorted by time. It then
     * returns all the removed events.
     *
     * @param T the type of the events in the list, some subclass of [MidiEvent]
     * @param events the list of events to search
     * @param time the current time
     * @param context the [Midis2jam2] context
     * @return a list of events that are past the current time, or at the current time
     */
    fun <T : MidiEvent> collect(events: MutableList<T>, time: Double, context: Midis2jam2): List<T> =
        events.takeWhile { context.file.eventInSeconds(it) <= time }.also {
            events.removeAll(it.toSet())
        }

    /**
     * Given a list of [MidiEvent]s, removes the events that are needing animation. This is any event that
     * has a time equal to or less than the current time. This method assumes [events] is sorted by time. It then
     * returns the first removed event.
     *
     * @param T the type of the events in the list, some subclass of [MidiEvent]
     * @param events the list of events to search
     * @param time the current time
     * @param context the [Midis2jam2] context
     * @return the first event that is past the current time, or at the current time
     */
    fun <T : MidiEvent> collectOne(events: MutableList<T>, time: Double, context: Midis2jam2): T? =
        events.firstOrNull { context.file.eventInSeconds(it) <= time }?.also {
            events.remove(it)
        }

    /**
     * Given a list of [NotePeriod]s, removes the notes that are needing animation. This is any note that has a
     * time equal to or less than the current time. This method assumes [notes] is sorted by time. It then returns
     * the last removed note.
     *
     * @param notes the list of notes to search
     * @param time the current time
     * @return the last note that is past the current time, or at the current time
     */
    fun collectOne(notes: MutableList<NotePeriod>, time: Double): NotePeriod? =
        notes.takeWhile { it.startTime <= time }.lastOrNull()?.also {
            notes.remove(it)
        }

    /**
     * Given a list of [MidiEvent]s, removes the events that are needing animation. This is any event that has a time
     * equal to or less than the current time.
     *
     * For events that are [MidiNoteOffEvent]s, they will be removed 1/30th of a second early so that repeated notes
     * on some instruments can be visually differentiated.
     *
     * This method assumes that [events] is sorted by event time.
     *
     * @param T the type of the events in the list, some subclass of [MidiEvent]
     * @param events the list of events to search
     * @param time the current time
     * @param context the [Midis2jam2] context
     * @return a list of events that are past the current time, or at the current time
     */
    @JvmStatic
    fun <T : MidiEvent> collectWithOffGap(events: MutableList<T>, context: Midis2jam2, time: Double): List<T> =
        with(context.file) {
            events.takeWhile {
                eventInSeconds(it) <= time || it is MidiNoteOffEvent && eventInSeconds(it) <= time - 0.033F
            }.also {
                events.removeAll(it.toSet())
            }
        }
}