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

package org.wysko.midis2jam2.instrument.family.piano

import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcCollector
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * An instrument that uses keys to play notes.
 *
 * @param rangeLow The lowest note that this instrument can play.
 * @param rangeHigh The highest note that this instrument can play.
 */
abstract class KeyedInstrument(
    context: Midis2jam2,
    eventList: MutableList<MidiEvent>,
    val rangeLow: Byte,
    val rangeHigh: Byte,
) : SustainedInstrument(context, eventList) {
    /** The keys of this instrument. */
    abstract val keys: Array<Key>

    override val collector: TimedArcCollector =
        TimedArcCollector(
            context = context,
            arcs = timedArcs,
            releaseCondition = { time: Duration, notePeriod: TimedArc ->
                processEventDuration(notePeriod, time)
            },
        )

    /** Returns the number of keys on this instrument. */
    fun keyCount(): Int = rangeHigh - rangeLow + 1

    /** Returns the [Key] associated with the [midiNote], or `null` if this instrument can't animate the note. */
    protected abstract fun getKeyByMidiNote(midiNote: Int): Key?

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)
        collector.advance(time)
        keys.forEach { it.tick(delta) }
    }

    private fun processEventDuration(it: TimedArc, time: Duration): Boolean = when {
        it.duration > 0.5.seconds -> time >= it.endTime - 0.1.seconds
        it.duration > 0.2.seconds -> time >= it.endTime - 0.05.seconds
        else -> time >= it.startTime + (it.duration * 0.5)
    }

    override fun toString(): String {
        return super.toString() +
                buildString {
                    append(
                        formatProperty(
                            "keys",
                            keys.joinToString(separator = "") { if (it.currentState is Key.State.Down) "X" else "_" },
                        ),
                    )
                }
    }

    /**
     * Returns the state of the key associated with the [midiNote].
     *
     * @param midiNote The MIDI note to check.
     * @return The state of the key.
     */
    open fun keyStatus(midiNote: Byte): Key.State =
        collector.currentTimedArcs.firstOrNull { it.note == midiNote }?.let {
            Key.State.Down(it.noteOn.velocity)
        } ?: Key.State.Up
}
