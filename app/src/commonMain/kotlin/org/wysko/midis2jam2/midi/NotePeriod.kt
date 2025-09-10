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
import org.wysko.kmidi.midi.analysis.Polyphony
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

fun improvedContiguousGroupDetection(notePeriods: List<TimedArc>): List<TimedArcGroup> {
    val overlapThreshold = 0.01 // 10 milliseconds

    fun shouldIgnoreOverlap(note1: TimedArc, note2: TimedArc): Boolean {
        return note1.end > note2.start && note1.end - note2.start < overlapThreshold
    }

    fun areSustainedTogether(note1: TimedArc, note2: TimedArc): Boolean {
        // Check if note1 and note2 sustain together for a significant portion of their duration
        return note1.start < note2.end && note2.start < note1.end
    }

    val groups = mutableListOf<TimedArcGroup>()
    var currentGroup = mutableSetOf<TimedArc>()

    for (i in notePeriods.indices) {
        val currentNote = notePeriods[i]

        if (currentGroup.isEmpty()) {
            currentGroup.add(currentNote)
        } else {
            val lastNote = currentGroup.last()
            val isOverlapping = lastNote.end > currentNote.start
            val shouldGroup = isOverlapping && !shouldIgnoreOverlap(lastNote, currentNote)

            if (shouldGroup || areSustainedTogether(lastNote, currentNote)) {
                currentGroup.add(currentNote)
            } else {
                groups.add(TimedArcGroup(currentGroup))
                currentGroup = mutableSetOf(currentNote)
            }
        }

        // Look ahead to handle sustained notes forming chords
        val lookAheadNotes = notePeriods.subList(i + 1, notePeriods.size)

        for (lookAheadNote in lookAheadNotes) {
            if (areSustainedTogether(currentNote, lookAheadNote)) {
                currentGroup.add(lookAheadNote)
            }
        }
    }

    if (currentGroup.isNotEmpty()) {
        groups.add(TimedArcGroup(currentGroup))
    }

    // Break some groups if the predominant polyphony is 1
    val probablySoloGroups = groups.filter { (arcs) ->
        Polyphony.averagePolyphony(arcs.map { listOf(it.noteOn, it.noteOff) }.flatten()).roundToInt() == 1
    }
    groups.removeAll(probablySoloGroups)
    probablySoloGroups.forEach { soloGroup ->
        soloGroup.arcs.forEach { notePeriod ->
            groups.add(TimedArcGroup(setOf(notePeriod)))
        }
    }

    return groups
}

/**
 * Calculates the note periods based on the given context and [modulus].
 *
 * @param context The context to the main class.
 * @param modulus The index of the pitch class in the list `A, A#, B, ..., G#` (0-indexed).
 * @return The note-periods based on the given context and modulus.
 */
fun List<MidiEvent>.notePeriodsModulus(context: Midis2jam2, modulus: Int): List<TimedArc> =
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

