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

import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument

/**
 * A note period is a tuple consisting of a [MidiNoteOnEvent] and a [MidiNoteOffEvent]. A note period is the
 * equivalent of the blocks you would see in a MIDI piano roll editor.
 */
open class NotePeriod(
    /** The MIDI pitch of this note period. */
    val midiNote: Int,
    /** The start time, expressed in seconds. */
    val startTime: Double,
    /** The end time, expressed in seconds. */
    var endTime: Double,
    /** The [MidiNoteOnEvent]. */
    val noteOn: MidiNoteOnEvent,
    /** The [MidiNoteOffEvent]. */
    val noteOff: MidiNoteOffEvent,
) {
    /** [FrettedInstrument] gets help from this. */
    var animationStarted: Boolean = false

    /** Returns the MIDI tick this note period starts. */
    fun startTick(): Long {
        return noteOn.time
    }

    /** Returns the MIDI tick this note period ends. */
    fun endTick(): Long {
        return noteOff.time
    }

    /** Returns the length of this note period, expressed in seconds. */
    fun duration(): Double {
        return endTime - startTime
    }

    /** The current progress of this elapsing, represented in the range `0.0..1.0` */
    fun progress(time: Double): Double = (1.0 - (endTime - time) / duration()).coerceIn(0.0..1.0)

    override fun toString(): String {
        return String.format("[%d @ %.1f--%.1f]", midiNote, startTime, endTime)
    }

    companion object {
        /**
         * A MIDI file is a sequence of [MidiNoteOnEvents][MidiNoteOnEvent] and [MidiNoteOffEvents][MidiNoteOffEvent].
         * Each pair of a NoteOn and NoteOff event can correspond to a [NotePeriod]. This method calculates those
         * pairs and returns them as a list of NotePeriods.
         *
         * @param instrument the [Instrument] that the [noteEvents] pertain to
         * @param noteEvents the note events to calculate into NotePeriods
         * @return the note events as a list of NotePeriods
         */
        @Contract(pure = true)
        fun calculateNotePeriods(
            midiFile: MidiFile,
            noteEvents: List<MidiNoteEvent>,
        ): MutableList<NotePeriod> {
            val notePeriods: ArrayList<NotePeriod> = ArrayList()
            val onEvents = arrayOfNulls<MidiNoteOnEvent>(MIDI_MAX_NOTE + 1)

            /* To calculate NotePeriods, we iterate over each MidiNoteEvent and keep track of when a NoteOnEvent occurs.
             * When it does, we insert it into the array at the index of the note's value. Then, when a NoteOffEvent
             * occurs, we look up the NoteOnEvent by the NoteOffEvent's value and create a NotePeriod from that.
             *
             * I wrote this with the assumption that there would not be duplicate notes of the same value that overlap,
             * so I'm not sure how it will handle in that scenario.
             *
             * Runs in O(n) time.
             */
            noteEvents.forEach { noteEvent ->
                if (noteEvent is MidiNoteOnEvent) {
                    // If the same note starts again while it is playing, just ignore this new NoteOn event
                    if (onEvents[noteEvent.note] == null) {
                        onEvents[noteEvent.note] = noteEvent
                    }
                } else {
                    val noteOff = noteEvent as MidiNoteOffEvent
                    onEvents[noteOff.note]?.let {
                        notePeriods.add(
                            NotePeriod(
                                midiNote = noteOff.note,
                                startTime = midiFile.eventInSeconds(it),
                                endTime = midiFile.eventInSeconds(noteOff),
                                noteOn = it,
                                noteOff = noteOff,
                            ),
                        )
                        onEvents[noteOff.note] = null
                    }
                }
            }

            // Remove exact duplicates
            return ArrayList(notePeriods.distinct())
        }
    }
}

fun List<MidiEvent>.notePeriodsModulus(
    context: Midis2jam2,
    modulus: Int,
): List<NotePeriod> =
    NotePeriod.calculateNotePeriods(context.file, filterIsInstance<MidiNoteEvent>().filter { it.note % 12 == modulus })

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

/**
 * A collection of [NotePeriod]s that have overlapping times.
 *
 * @property notePeriods The [NotePeriod]s that are in this group.
 */
data class NotePeriodGroup(val notePeriods: List<NotePeriod>) {
    /** The time at which the first [NotePeriod] in this group begins. */
    fun startTime(): Double = notePeriods.minOf { it.startTime }

    /** The time at which the last [NotePeriod] in this group ends. */
    fun endTime(): Double = notePeriods.maxOf { it.endTime }

    /** The total amount of time this group elapses. */
    fun duration(): Double = endTime() - startTime()

    /**
     * The progress of this group's elapsing, given the current [time], represented in the range `0.0..1.0`.
     *
     * @param time The current time, expressed in seconds.
     */
    fun progress(time: Double): Double = (1.0 - (endTime() - time) / duration()).coerceIn(0.0..1.0)
}
