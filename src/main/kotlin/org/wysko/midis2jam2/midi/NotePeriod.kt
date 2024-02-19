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

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument

/**
 * A note period is a tuple consisting of a [MidiNoteOnEvent] and a [MidiNoteOffEvent].
 *
 * @property note The note value.
 * @property start The start time, expressed in seconds.
 * @property end The end time, expressed in seconds.
 * @property noteOn The [MidiNoteOnEvent].
 * @property noteOff The [MidiNoteOffEvent].
 */
open class NotePeriod(
    val note: Int,
    val start: Double,
    var end: Double,
    val noteOn: MidiNoteOnEvent,
    val noteOff: MidiNoteOffEvent,
) {
    /** [FrettedInstrument] gets help from this. */
    var animationStarted: Boolean = false

    /**
     * The MIDI tick this note period starts.
     */
    val startTick: Long
        get() = noteOn.time

    /**
     * The MIDI tick this note period ends.
     */
    val endTick: Long
        get() = noteOff.time

    /**
     * The length of this note period, expressed in seconds.
     */
    val duration: Double
        get() = end - start

    /**
     * The current progress of this elapsing, represented in the range `0.0..1.0`.
     *
     * @param time The current time, expressed in seconds.
     */
    fun calculateProgress(time: Double): Double = (1.0 - (end - time) / duration).coerceIn(0.0..1.0)

    override fun toString(): String = "[%d @ %.1f--%.1f]".format(note, start, end)

    companion object {
        /**
         * From a list of [MidiNoteEvent]s, calculates the note periods.
         *
         * @param midiFile The MIDI file.
         * @param noteEvents The list of [MidiNoteEvent]s.
         */
        fun calculateNotePeriods(midiFile: MidiFile, noteEvents: List<MidiNoteEvent>): MutableList<NotePeriod> {
            val notePeriods = mutableListOf<NotePeriod>()
            val onEvents = arrayOfNulls<MidiNoteOnEvent>(MIDI_MAX_NOTE + 1)
            noteEvents.forEach { noteEvent ->
                if (noteEvent is MidiNoteOnEvent) {
                    // If the same note starts again while it is playing, ignore this new NoteOn event
                    if (onEvents[noteEvent.note] == null) {
                        onEvents[noteEvent.note] = noteEvent
                    }
                } else {
                    val noteOff = noteEvent as MidiNoteOffEvent
                    onEvents[noteOff.note]?.let {
                        notePeriods.add(
                            NotePeriod(
                                note = noteOff.note,
                                start = midiFile.eventInSeconds(it),
                                end = midiFile.eventInSeconds(noteOff),
                                noteOn = it,
                                noteOff = noteOff,
                            ),
                        )
                        onEvents[noteOff.note] = null
                    }
                }
            }

            // Remove exact duplicates
            return notePeriods.distinct().toMutableList()
        }
    }
}

/**
 * Calculates the note periods based on the given context and [modulus].
 *
 * @param context The context to the main class.
 * @param modulus The index of the pitch class in the list `A, A#, B, ..., G#` (0-indexed).
 * @return The note-periods based on the given context and modulus.
 */
fun List<MidiEvent>.notePeriodsModulus(context: Midis2jam2, modulus: Int): List<NotePeriod> =
    NotePeriod.calculateNotePeriods(
        context.file,
        filterIsInstance<MidiNoteEvent>().filter { (it.note + 3) % 12 == modulus }
    )

/**
 * It is useful for some instruments to identify groups of [NotePeriod]s that overlap. For example, if three notes with
 * different note values played at the same time (their [MidiNoteOnEvent] and [MidiNoteOffEvent]s have the same tick
 * values), this would constitute a group. However, notes with *any* amount of overlap constitute a group.
 *
 * This function assumes the input list is sorted by start time.
 */
fun List<NotePeriod>.contiguousGroups(): List<NotePeriodGroup> {
    // Easy gimmes
    if (this.isEmpty()) return emptyList()
    if (this.size == 1) return listOf(NotePeriodGroup(listOf(first())))

    val groups = mutableListOf<NotePeriodGroup>()
    var currentGroup: MutableList<NotePeriod> = mutableListOf()
    var furthestTime = 0L

    fun NotePeriod.register(newGroup: Boolean = false) {
        if (newGroup) {
            groups.add(NotePeriodGroup(currentGroup))
            currentGroup = mutableListOf(this)
            furthestTime = this.noteOff.time
        } else {
            currentGroup.add(this)
            furthestTime = this.noteOff.time.coerceAtLeast(furthestTime)
        }
    }

    this.forEach { notePeriod ->
        notePeriod.register(currentGroup.isNotEmpty() && notePeriod.noteOn.time >= furthestTime)
    }

    if (currentGroup.isNotEmpty()) {
        groups.add(NotePeriodGroup(currentGroup))
    }

    return groups
}

