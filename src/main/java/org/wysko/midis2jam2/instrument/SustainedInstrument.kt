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
package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.NotePeriod.Companion.calculateNotePeriods

/**
 * Any instrument that also depends on knowing the [MidiNoteOffEvent] for proper animation. Examples include:
 * saxophone, piano, guitar, telephone ring.
 */
abstract class SustainedInstrument protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>
) : Instrument(context) {

    /** This list shall not be updated and shall be used for visibility calculations. */
    private val unmodifiableNotePeriods: List<NotePeriod>

    /** The list of current note periods. Will always be updating as the MIDI file progresses. */
    protected val currentNotePeriods: MutableList<NotePeriod> = ArrayList()

    /**
     * The list of note periods. This class expects that this variable will be truncated as the MIDI file progresses.
     *
     * @see NotePeriod
     */
    protected var notePeriods: MutableList<NotePeriod>

    /** The last elapsed [NotePeriod]. Used for visibility calculation. */
    protected var lastPlayedNotePeriod: NotePeriod? = null

    /**
     * Determines which note periods should have starting animations at the specified time. Removes the returned
     * elements from [.notePeriods]. The method also removes elapsed note periods. All results are stored in
     * [.currentNotePeriods].
     *
     * @param time the current time
     * @see currentNotePeriods
     */
    protected open fun calculateCurrentNotePeriods(time: Double) {
        /* Look at the first note period in the list. If its starting time is less than or equal to the current time,
         * it's time to start animating it. */
        while (notePeriods.isNotEmpty() && notePeriods[0].startTime <= time) {
            currentNotePeriods.add(notePeriods.removeAt(0))
        }

        /* Remove all the note periods that have elapsed. */
        val takeWhile = currentNotePeriods.takeWhile { it.endTime <= time }
        currentNotePeriods.removeAll(takeWhile)

        /* Set the last played note period for visibility calculation. */
        takeWhile.lastOrNull()?.let {
            lastPlayedNotePeriod = it
        }
    }

    override fun tick(time: Double, delta: Float) {
        calculateCurrentNotePeriods(time)
        setVisibility(time)
        moveForMultiChannel(delta)
    }

    /** Calculates the current visibility by the current [time]. True if the instrument is visible, false otherwise.
     * This method can be used for time values in the future. */
    override fun calcVisibility(time: Double, future: Boolean): Boolean {
        if (!future) {
            /* Currently playing? Visible. */
            if (currentNotePeriods.isNotEmpty()) return true

            /* Within one second of playing? Visible. */
            if (notePeriods.isNotEmpty() && notePeriods[0].startTime - time <= 1) return true

            /* If within a 7-second gap between the last note and the next? Visible. */
            if (lastPlayedNotePeriod != null
                && notePeriods.isNotEmpty()
                && notePeriods[0].startTime - lastPlayedNotePeriod!!.endTime <= 7
            ) return true

            /* If after 2 seconds of the last note period? Visible. */
            if (lastPlayedNotePeriod != null && time - lastPlayedNotePeriod!!.endTime <= 2) return true

            /* Invisible. */
            return false
        } else {
            /* Currently playing? Visible. */
            if (unmodifiableNotePeriods.any { time in it.startTime..it.endTime }) return true

            /* Within one second of playing? Visible. */
            if (unmodifiableNotePeriods.any { it.startTime > time && it.startTime - time <= 1 }) return true

            /* If within a 7-second gap between two note periods? Visible. */
            for (i in 0 until unmodifiableNotePeriods.size - 1) {
                if (unmodifiableNotePeriods[i + 1].startTime - unmodifiableNotePeriods[i].endTime <= 7
                    && time in unmodifiableNotePeriods[i].endTime..unmodifiableNotePeriods[i + 1].startTime
                ) return true
            }

            /* If after 2 seconds of the last note period? Visible. */
            if (unmodifiableNotePeriods.any { time > it.endTime && time - it.endTime <= 2 }) return true

            /* Invisible. */
            return false
        }
    }

    init {
        val midiNoteEvents = eventList.filterIsInstance<MidiNoteEvent>()
        notePeriods = calculateNotePeriods(this, midiNoteEvents as MutableList<MidiNoteEvent>)

        /* In theory, the note periods should already be sorted due to the nature of how MIDI data is structured, but in
         * case during the conversion process they become unsorted, we sort as a safe measure. */
        notePeriods.sortBy { it.startTime }

        unmodifiableNotePeriods = ArrayList(notePeriods)
    }
}
