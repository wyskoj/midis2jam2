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

import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.NotePeriod.Companion.calculateNotePeriods
import kotlin.math.abs

/**
 * A sustained instrument is any instrument that also depends on knowing the [MidiNoteOffEvent] for proper
 * animation. Examples include: saxophone, piano, guitar, telephone ring.
 */
abstract class SustainedInstrument protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>
) : Instrument(context) {

    /**
     * This list shall not be updated and shall be used for visibility calculations.
     */
    private val unmodifiableNotePeriods: List<NotePeriod>

    /**
     * The list of current note periods. Will always be updating as the MIDI file progresses.
     */
    protected val currentNotePeriods: MutableList<NotePeriod> = ArrayList()

    /**
     * The list of note periods. This class expects that this variable will be truncated as the MIDI file progresses.
     *
     * @see NotePeriod
     */
    protected var notePeriods: MutableList<NotePeriod>

    /**
     * Determines which note periods should have starting animations at the specified time. Removes the returned
     * elements from [.notePeriods]. The method also removes elapsed note periods. All results are stored in
     * [.currentNotePeriods].
     *
     * @param time the current time
     * @see currentNotePeriods
     */
    protected open fun calculateCurrentNotePeriods(time: Double) {
        while (notePeriods.isNotEmpty() && notePeriods[0].startTime <= time) {
            currentNotePeriods.add(notePeriods.removeAt(0))
        }
        currentNotePeriods.removeIf { notePeriod: NotePeriod -> notePeriod.endTime <= time }
    }

    override fun tick(time: Double, delta: Float) {
        calculateCurrentNotePeriods(time)
        setIdleVisibilityByPeriods(time)
        moveForMultiChannel(delta)
    }

    /**
     * Determines whether this instrument should be visible at the time, and sets the visibility accordingly.
     *
     *
     * The instrument should be visible if:
     *
     *  * There is at least 1 second between now and the start of any note period,
     *  * There is at least 4 seconds between now and the end of any note period, or
     *  * Any note period is currently playing
     *
     * @param time the current time
     */
    protected fun setIdleVisibilityByPeriods(time: Double) {
        isVisible = calcVisibility(time, unmodifiableNotePeriods)
        instrumentNode.cullHint = if (isVisible) Dynamic else Always
    }

    companion object {
        /**
         * Filters a list of MIDI channel specific events and returns only the [MidiNoteEvent]s.
         *
         * @param events the event list
         * @return only the MidiNoteEvents
         * @see MidiNoteEvent
         */
        @Contract(pure = true)
        fun scrapeMidiNoteEvents(events: Collection<MidiChannelSpecificEvent>): List<MidiNoteEvent> =
            events.filterIsInstance<MidiNoteEvent>()

        /**
         * Calculate the current visibility.
         *
         * @param time                    the time
         * @param unmodifiableNotePeriods the unmodifiable note periods
         * @return true if this instrument should be visible, false otherwise
         */
        fun calcVisibility(time: Double, unmodifiableNotePeriods: Iterable<NotePeriod>): Boolean {
            var show = false
            for (notePeriod in unmodifiableNotePeriods) {
                // Within 1 second of a note on,
                // within 4 seconds of a note off,
                // or during a note, be visible
                if (notePeriod.isPlayingAt(time) || abs(time - notePeriod.startTime) < START_BUFFER || abs(time - notePeriod.endTime) < END_BUFFER && time > notePeriod.endTime) {
                    show = true
                    break
                }
            }
            return show
        }
    }

    init {
        val midiNoteEvents = scrapeMidiNoteEvents(eventList)
        notePeriods = calculateNotePeriods(this, midiNoteEvents as MutableList<MidiNoteEvent>)
        unmodifiableNotePeriods = ArrayList(notePeriods)
    }
}