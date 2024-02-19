/*
 * Copyright (C) 2024 Jacob Wysko
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

import kotlin.math.max

/** The max value a MIDI note can have. */
const val MIDI_MAX_NOTE: Int = 127

/**
 * Indicates the start or end of a note.
 *
 * @property time The time this event occurs in MIDI ticks.
 * @property channel The channel on which the note should be played.
 * @property note The note number of the note to be played.
 */
sealed class MidiNoteEvent(
    override val time: Long,
    override val channel: Int,
    open val note: Int
) : MidiChannelEvent(time, channel) {
    companion object {
        /**
         * Returns the maximum polyphony of the MIDI note events in the given list.
         *
         * Polyphony refers to the maximum number of simultaneous notes that are played.
         *
         * @receiver The list of MIDI note events.
         * @return The maximum polyphony.
         */
        val List<MidiNoteEvent>.maximumPolyphony: Int
            get() = fold((0 to 0)) { (current, max), event ->
                when (event) {
                    is MidiNoteOnEvent -> current + 1 to max(current + 1, max)
                    is MidiNoteOffEvent -> current - 1 to max
                }
            }.second

    }
}

/**
 * Filters the list of [MidiNoteEvent]s by the specified notes.
 *
 * @param T The type of [MidiNoteEvent] to filter.
 * @param notes The values of the notes to filter by.
 * @return A new mutable list containing the filtered [MidiNoteEvent]s.
 */
fun <T : MidiNoteEvent> List<T>.filterByNotes(vararg notes: Int): List<T> =
    filter { notes.contains(it.note) }.toMutableList()
