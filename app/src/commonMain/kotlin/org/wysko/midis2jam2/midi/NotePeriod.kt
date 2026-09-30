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
package org.wysko.midis2jam2.midi

import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import kotlin.time.Duration.Companion.seconds

/**
 * Calculates the note periods based on the given context and [modulus].
 *
 * @param context The context to the main class.
 * @param modulus The index of the pitch class in the list `A, A#, B, ..., G#` (0-indexed).
 * @return The note-periods based on the given context and modulus.
 */
fun List<MidiEvent>.notePeriodsModulus(context: PerformanceManager, modulus: Int): List<TimedArc> =
    TimedArc.fromNoteEvents(
        context.sequence,
        filterIsInstance<NoteEvent>().filter { (it.note + 3) % 12 == modulus }
    )

/**
 * It is useful for some instruments to identify groups of [NotePeriod]s that overlap. For example, if three notes with
 * different note values played at the same time (their [MidiNoteOnEvent] and [MidiNoteOffEvent]s have the same tick
 * values), this would constitute a group. However, notes with *any* amount of overlap constitute a group.
 *
 * This function assumes the input list is sorted by start time.
 */
fun List<TimedArc>.contiguousGroups(): List<TimedArcGroup> {
    // Easy gimmes
    if (this.isEmpty()) return emptyList()
    if (this.size == 1) return listOf(TimedArcGroup(setOf(first())))

    val groups = mutableListOf<TimedArcGroup>()
    var currentGroup = mutableSetOf<TimedArc>()
    var furthestTime = 0.seconds

    fun TimedArc.register(newGroup: Boolean = false) {
        if (newGroup) {
            groups.add(TimedArcGroup(currentGroup))
            currentGroup = mutableSetOf(this)
            furthestTime = this.endTime
        } else {
            currentGroup.add(this)
            furthestTime = this.endTime.coerceAtLeast(furthestTime)
        }
    }

    this.forEach { notePeriod ->
        notePeriod.register(currentGroup.isNotEmpty() && notePeriod.startTime >= furthestTime)
    }

    if (currentGroup.isNotEmpty()) {
        groups.add(TimedArcGroup(currentGroup))
    }

    return groups
}

